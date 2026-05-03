package de.zeus.upcam.rest.prefilter;

import de.zeus.upcam.rest.camera.CameraImage;
import de.zeus.upcam.rest.camera.CameraSignal;
import de.zeus.upcam.rest.client.config.Config;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class PrefilterService {
    private static final int BASE_MASK_WIDTH = 384;
    private static final int BASE_MASK_HEIGHT = 192;
    private static final int COMPARE_HEIGHT = 192;

    private static final int EDGE_DIFF_PIXEL_THRESHOLD = 20;
    private static final double EDGE_DIFF_RATIO_THRESHOLD = 0.0038d;
    private static final double HIGH_EDGE_DIFF_RATIO = 0.0105d;

    private static final int PIXEL_DIFF_THRESHOLD = 17;
    private static final int MIN_FOREGROUND_AREA = 90;
    private static final int HIGH_FOREGROUND_AREA = 220;
    private static final double HIGH_FOREGROUND_RATIO = 0.009d;
    private static final int MIN_FOREGROUND_COMPONENT_AREA = 22;
    private static final int HIGH_FOREGROUND_COMPONENT_AREA = 55;

    private static final double BRIGHTNESS_MAX_UNIFORM_DELTA = 10.0d;
    private static final double BRIGHTNESS_MAX_STD_DELTA = 7.0d;
    private static final double BRIGHTNESS_MAX_FOREGROUND_RATIO = 0.04d;

    private static final double[][][] NORMALIZED_POLYGONS = new double[][][]{
            normalizePolygon(new int[][]{{60, 51}, {106, 49}, {106, 110}, {59, 110}}),
            normalizePolygon(new int[][]{{59, 110}, {94, 110}, {105, 128}, {123, 160}, {132, 191}, {82, 191}, {63, 126}}),
            normalizePolygon(new int[][]{{98, 54}, {240, 49}, {240, 161}, {176, 171}, {123, 160}, {105, 128}, {99, 92}}),
            normalizePolygon(new int[][]{{82, 191}, {243, 191}, {240, 161}, {176, 171}, {132, 191}}),
            normalizePolygon(new int[][]{{243, 48}, {383, 46}, {383, 191}, {298, 191}, {243, 167}})
    };

    private final Config conf = Config.getInstance();
    private PreparedFrame previousFrame;
    private Instant lastAcceptedAt;

    public PrefilterDecision evaluate(Path imagePath) {
        return evaluate(imagePath, null);
    }

    public synchronized PrefilterDecision evaluate(Path imagePath, CameraImage cameraImage) {
        CameraSignal cameraSignal = cameraImage != null ? cameraImage.getCameraSignal() : CameraSignal.none();

        if (!conf.isPrefilterEnabled()) {
            return decision(PrefilterDecision.Action.ACCEPT, "prefilter_disabled", MotionMetrics.empty(), cameraSignal);
        }

        try {
            long size = Files.size(imagePath);
            if (conf.getPrefilterMinBytes() > 0 && size < conf.getPrefilterMinBytes()) {
                return decision(PrefilterDecision.Action.NOISE, "min_bytes_quarantine", MotionMetrics.empty(), cameraSignal);
            }

            BufferedImage img = ImageIO.read(imagePath.toFile());
            if (img == null) {
                return failOpenOrClosed("decode_error", cameraSignal);
            }

            PreparedFrame currentFrame = prepareFrame(img);
            if (currentFrame == null || currentFrame.activePixelCount <= 0) {
                return failOpenOrClosed("frame_prepare_error", cameraSignal);
            }

            MotionMetrics metrics = MotionMetrics.empty();
            if (previousFrame != null && previousFrame.matches(currentFrame)) {
                metrics = analyzeMotion(previousFrame, currentFrame);
            }

            boolean nativeSignalActive = conf.isPrefilterCameraSignalBypass() && cameraSignal.isAnyActive();
            if (previousFrame == null || !currentFrame.matches(previousFrame)) {
                previousFrame = currentFrame;
                markAccepted();
                return decision(PrefilterDecision.Action.ACCEPT,
                        nativeSignalActive ? "native_signal_reference_accept" : "reference_accept_first_frame",
                        metrics,
                        cameraSignal);
            }

            previousFrame = currentFrame;

            if (conf.isPrefilterObserveOnly()) {
                markAccepted();
                return decision(PrefilterDecision.Action.ACCEPT,
                        observeOnlyReason(metrics, nativeSignalActive),
                        metrics,
                        cameraSignal);
            }

            if (nativeSignalActive) {
                markAccepted();
                return decision(PrefilterDecision.Action.ACCEPT, "native_signal_accept", metrics, cameraSignal);
            }

            if (metrics.motion) {
                markAccepted();
                return decision(PrefilterDecision.Action.ACCEPT,
                        metrics.highConfidence ? "roi_motion_accept_high" : "roi_motion_accept",
                        metrics,
                        cameraSignal);
            }

            if (shouldAcceptAfterSuppressWindow()) {
                markAccepted();
                return decision(PrefilterDecision.Action.ACCEPT, "max_suppress_window_accept", metrics, cameraSignal);
            }

            return decision(PrefilterDecision.Action.NOISE,
                    metrics.brightnessSuppressed ? "brightness_shift_quarantine" : "roi_no_motion_quarantine",
                    metrics,
                    cameraSignal);
        } catch (IOException e) {
            return failOpenOrClosed("io_error", cameraSignal);
        } catch (Exception e) {
            return failOpenOrClosed("unexpected_error", cameraSignal);
        }
    }

    private PreparedFrame prepareFrame(BufferedImage image) {
        int targetWidth = Math.max(64, conf.getPrefilterResizeWidth());
        int targetHeight = Math.max(2, (int) Math.round((double) image.getHeight() * targetWidth / image.getWidth()));
        BufferedImage resized = resize(image, targetWidth, targetHeight);

        int cropTop = Math.max(0, Math.min(conf.getPrefilterCropTopPx(), Math.max(0, resized.getHeight() - 2)));
        int compareHeight = Math.min(COMPARE_HEIGHT, resized.getHeight() - cropTop);
        if (compareHeight <= 1) {
            return null;
        }

        byte[] gray = new byte[targetWidth * compareHeight];
        for (int y = 0; y < compareHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                gray[(y * targetWidth) + x] = (byte) luminance(resized.getRGB(x, y + cropTop));
            }
        }

        byte[] activeMask = buildActiveMask(targetWidth, compareHeight);
        byte[] edges = computeSobelEdges(gray, targetWidth, compareHeight);
        int activePixelCount = countActivePixels(activeMask);
        return new PreparedFrame(targetWidth, compareHeight, gray, edges, activeMask, activePixelCount);
    }

    private MotionMetrics analyzeMotion(PreparedFrame previous, PreparedFrame current) {
        int activePixelCount = current.activePixelCount;
        if (activePixelCount <= 0) {
            return MotionMetrics.empty();
        }

        double signedDiffSum = 0.0d;
        for (int i = 0; i < current.gray.length; i++) {
            if (!isActive(current.activeMask, i)) {
                continue;
            }
            signedDiffSum += value(current.gray[i]) - value(previous.gray[i]);
        }
        double meanDelta = signedDiffSum / activePixelCount;

        double diffSum = 0.0d;
        double diffSq = 0.0d;
        int diffCount = 0;
        byte[] foregroundMask = new byte[current.gray.length];

        for (int i = 0; i < current.gray.length; i++) {
            if (!isActive(current.activeMask, i)) {
                continue;
            }

            double normalizedDiff = Math.abs((value(current.gray[i]) - value(previous.gray[i])) - meanDelta);
            diffSum += normalizedDiff;
            diffSq += normalizedDiff * normalizedDiff;

            if (normalizedDiff > PIXEL_DIFF_THRESHOLD) {
                foregroundMask[i] = 1;
                diffCount++;
            }
        }

        double meanAbsDiff = diffSum / activePixelCount;
        double diffVariance = Math.max(0.0d, (diffSq / activePixelCount) - (meanAbsDiff * meanAbsDiff));
        double stdAbsDiff = Math.sqrt(diffVariance);

        int edgeDiffCount = 0;
        for (int i = 0; i < current.edges.length; i++) {
            if (!isActive(current.activeMask, i)) {
                continue;
            }
            if (Math.abs(value(current.edges[i]) - value(previous.edges[i])) > EDGE_DIFF_PIXEL_THRESHOLD) {
                edgeDiffCount++;
            }
        }

        double edgeDiffRatio = (double) edgeDiffCount / activePixelCount;
        double foregroundRatio = (double) diffCount / activePixelCount;
        int largestComponentArea = largestConnectedComponent(foregroundMask, current.activeMask, current.width, current.height);
        double largestAreaRatio = (double) largestComponentArea / activePixelCount;
        double motionScore = (edgeDiffRatio * 0.45d) + (foregroundRatio * 0.35d) + (largestAreaRatio * 0.20d);

        boolean edgePass = edgeDiffRatio >= EDGE_DIFF_RATIO_THRESHOLD;
        boolean foregroundPass = diffCount >= MIN_FOREGROUND_AREA;
        boolean componentPass = largestComponentArea >= MIN_FOREGROUND_COMPONENT_AREA;
        boolean highArea = diffCount >= HIGH_FOREGROUND_AREA;
        boolean highComponent = largestComponentArea >= HIGH_FOREGROUND_COMPONENT_AREA;
        boolean highFgRatio = foregroundRatio >= HIGH_FOREGROUND_RATIO;
        boolean highEdge = edgeDiffRatio >= HIGH_EDGE_DIFF_RATIO;

        boolean brightnessSuppressed = meanAbsDiff >= BRIGHTNESS_MAX_UNIFORM_DELTA
                && stdAbsDiff <= BRIGHTNESS_MAX_STD_DELTA
                && foregroundRatio <= BRIGHTNESS_MAX_FOREGROUND_RATIO
                && !highArea
                && !highComponent
                && !highEdge;

        boolean motion = !brightnessSuppressed
                && ((edgePass && (foregroundPass || componentPass)) || highArea || highComponent);
        boolean highConfidence = !brightnessSuppressed && (highArea || highComponent);

        return new MotionMetrics(motion, highConfidence, brightnessSuppressed, motionScore,
                edgeDiffRatio, foregroundRatio, diffCount, largestComponentArea);
    }

    private PrefilterDecision decision(PrefilterDecision.Action action,
                                       String reason,
                                       MotionMetrics metrics,
                                       CameraSignal cameraSignal) {
        return new PrefilterDecision(action,
                reason,
                null,
                -1,
                metrics.motionScore,
                metrics.edgeDiffRatio,
                metrics.foregroundRatio,
                metrics.foregroundArea,
                metrics.largestComponentArea,
                cameraSignal.summarize(),
                cameraSignal.isAnyActive());
    }

    private PrefilterDecision failOpenOrClosed(String reason, CameraSignal cameraSignal) {
        String mode = conf.getPrefilterFailMode();
        boolean open = mode == null || mode.trim().isEmpty() || "open".equalsIgnoreCase(mode);
        return decision(open ? PrefilterDecision.Action.ACCEPT : PrefilterDecision.Action.NOISE,
                reason + (open ? "_open" : "_closed"),
                MotionMetrics.empty(),
                cameraSignal);
    }

    private void markAccepted() {
        lastAcceptedAt = Instant.now();
    }

    private boolean shouldAcceptAfterSuppressWindow() {
        int maxSuppressSeconds = conf.getPrefilterMaxSuppressSeconds();
        if (maxSuppressSeconds <= 0 || lastAcceptedAt == null) {
            return false;
        }
        return Duration.between(lastAcceptedAt, Instant.now()).getSeconds() >= maxSuppressSeconds;
    }

    private String observeOnlyReason(MotionMetrics metrics, boolean nativeSignalActive) {
        if (nativeSignalActive) {
            return "observe_only_native_signal";
        }
        if (metrics.motion) {
            return metrics.highConfidence ? "observe_only_motion_high" : "observe_only_motion";
        }
        if (metrics.brightnessSuppressed) {
            return "observe_only_brightness_shift";
        }
        return "observe_only_no_motion";
    }

    private static BufferedImage resize(BufferedImage input, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = out.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(input, 0, 0, width, height, null);
        graphics.dispose();
        return out;
    }

    private static byte[] buildActiveMask(int width, int height) {
        byte[] mask = new byte[width * height];
        for (int y = 0; y < height; y++) {
            double py = (y + 0.5d) / height;
            for (int x = 0; x < width; x++) {
                double px = (x + 0.5d) / width;
                if (isInsideAnyPolygon(px, py)) {
                    mask[(y * width) + x] = 1;
                }
            }
        }
        return mask;
    }

    private static boolean isInsideAnyPolygon(double px, double py) {
        for (double[][] polygon : NORMALIZED_POLYGONS) {
            if (pointInPolygon(px, py, polygon)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pointInPolygon(double px, double py, double[][] polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
            double xi = polygon[i][0];
            double yi = polygon[i][1];
            double xj = polygon[j][0];
            double yj = polygon[j][1];
            boolean intersects = ((yi > py) != (yj > py))
                    && (px < (((xj - xi) * (py - yi)) / ((yj - yi) == 0.0d ? 1e-9d : (yj - yi))) + xi);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double[][] normalizePolygon(int[][] polygon) {
        double[][] normalized = new double[polygon.length][2];
        for (int i = 0; i < polygon.length; i++) {
            normalized[i][0] = polygon[i][0] / (double) BASE_MASK_WIDTH;
            normalized[i][1] = polygon[i][1] / (double) BASE_MASK_HEIGHT;
        }
        return normalized;
    }

    private static byte[] computeSobelEdges(byte[] gray, int width, int height) {
        byte[] out = new byte[gray.length];
        if (width < 3 || height < 3) {
            return out;
        }

        for (int y = 1; y < height - 1; y++) {
            int y0 = (y - 1) * width;
            int y1 = y * width;
            int y2 = (y + 1) * width;
            for (int x = 1; x < width - 1; x++) {
                int gx = -value(gray[y0 + (x - 1)]) + value(gray[y0 + (x + 1)])
                        - (2 * value(gray[y1 + (x - 1)])) + (2 * value(gray[y1 + (x + 1)]))
                        - value(gray[y2 + (x - 1)]) + value(gray[y2 + (x + 1)]);
                int gy = value(gray[y0 + (x - 1)]) + (2 * value(gray[y0 + x])) + value(gray[y0 + (x + 1)])
                        - value(gray[y2 + (x - 1)]) - (2 * value(gray[y2 + x])) - value(gray[y2 + (x + 1)]);
                int magnitude = Math.min(255, Math.abs(gx) + Math.abs(gy));
                out[y1 + x] = (byte) magnitude;
            }
        }
        return out;
    }

    private static int largestConnectedComponent(byte[] mask, byte[] activeMask, int width, int height) {
        int largest = 0;
        byte[] visited = new byte[mask.length];
        int[] queue = new int[mask.length];

        for (int i = 0; i < mask.length; i++) {
            if (mask[i] == 0 || visited[i] != 0 || activeMask[i] == 0) {
                continue;
            }

            int head = 0;
            int tail = 0;
            queue[tail++] = i;
            visited[i] = 1;
            int area = 0;

            while (head < tail) {
                int index = queue[head++];
                area++;

                int x = index % width;
                int y = index / width;

                for (int ny = y - 1; ny <= y + 1; ny++) {
                    if (ny < 0 || ny >= height) {
                        continue;
                    }
                    int row = ny * width;
                    for (int nx = x - 1; nx <= x + 1; nx++) {
                        if (nx < 0 || nx >= width) {
                            continue;
                        }
                        int neighbor = row + nx;
                        if (visited[neighbor] == 0 && activeMask[neighbor] != 0 && mask[neighbor] != 0) {
                            visited[neighbor] = 1;
                            queue[tail++] = neighbor;
                        }
                    }
                }
            }

            if (area > largest) {
                largest = area;
            }
        }

        return largest;
    }

    private static int countActivePixels(byte[] mask) {
        int count = 0;
        for (byte pixel : mask) {
            if (pixel != 0) {
                count++;
            }
        }
        return count;
    }

    private static boolean isActive(byte[] mask, int index) {
        return mask[index] != 0;
    }

    private static int value(byte b) {
        return b & 0xFF;
    }

    private static int luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    private static class PreparedFrame {
        private final int width;
        private final int height;
        private final byte[] gray;
        private final byte[] edges;
        private final byte[] activeMask;
        private final int activePixelCount;

        private PreparedFrame(int width, int height, byte[] gray, byte[] edges, byte[] activeMask, int activePixelCount) {
            this.width = width;
            this.height = height;
            this.gray = gray;
            this.edges = edges;
            this.activeMask = activeMask;
            this.activePixelCount = activePixelCount;
        }

        private boolean matches(PreparedFrame other) {
            return other != null
                    && width == other.width
                    && height == other.height
                    && gray.length == other.gray.length
                    && edges.length == other.edges.length
                    && activeMask.length == other.activeMask.length;
        }
    }

    private static class MotionMetrics {
        private static final MotionMetrics EMPTY = new MotionMetrics(false, false, false, 0.0d, 0.0d, 0.0d, 0, 0);

        private final boolean motion;
        private final boolean highConfidence;
        private final boolean brightnessSuppressed;
        private final double motionScore;
        private final double edgeDiffRatio;
        private final double foregroundRatio;
        private final int foregroundArea;
        private final int largestComponentArea;

        private MotionMetrics(boolean motion,
                              boolean highConfidence,
                              boolean brightnessSuppressed,
                              double motionScore,
                              double edgeDiffRatio,
                              double foregroundRatio,
                              int foregroundArea,
                              int largestComponentArea) {
            this.motion = motion;
            this.highConfidence = highConfidence;
            this.brightnessSuppressed = brightnessSuppressed;
            this.motionScore = motionScore;
            this.edgeDiffRatio = edgeDiffRatio;
            this.foregroundRatio = foregroundRatio;
            this.foregroundArea = foregroundArea;
            this.largestComponentArea = largestComponentArea;
        }

        private static MotionMetrics empty() {
            return EMPTY;
        }
    }
}
