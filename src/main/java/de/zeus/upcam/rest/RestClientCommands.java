package de.zeus.upcam.rest;

import de.zeus.upcam.rest.camera.CameraImage;
import de.zeus.upcam.rest.client.config.Config;
import de.zeus.upcam.rest.fileio.FileIO;
import de.zeus.upcam.rest.model.ImageFileTracker;
import de.zeus.upcam.rest.prefilter.PrefilterDecision;
import de.zeus.upcam.rest.prefilter.PrefilterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Handles image persistence and prefilter decisions after the camera source has fetched image bytes.
 */
public class RestClientCommands {

    private final Config conf = Config.getInstance();
    private static final Logger LOG = LogManager.getLogger(RestClientCommands.class);
    private static final FileIO fileIO = new FileIO();
    private final PrefilterService prefilterService = new PrefilterService();

    public void persistImages(List<CameraImage> images, ImageFileTracker imageFileTracker) {
        for (CameraImage image : images) {
            String finalName = image.getSuggestedFilename();
            String tmpName = finalName + "_tmp";
            String imageId = filenameWithoutExtension(finalName);

            if (imageFileTracker.getImagesSent().contains(imageId)
                    || imageFileTracker.getImagesReceived().contains(imageId)) {
                continue;
            }

            Path tempPath = Paths.get(conf.getImgRcvFolder(), tmpName);
            try {
                fileIO.copy(tempPath.toString(), image.asInputStream());
                applyPrefilterDecision(tempPath, finalName, imageId, image, imageFileTracker);
            } catch (IOException e) {
                LOG.error("Error while persisting image {}", finalName, e);
            }
        }
    }

    private void applyPrefilterDecision(Path tempPath,
                                        String finalName,
                                        String imageId,
                                        CameraImage image,
                                        ImageFileTracker imageFileTracker) throws IOException {
        PrefilterDecision decision = prefilterService.evaluate(tempPath, image);
        long size = Files.size(tempPath);
        String targetPath = conf.getImgRcvFolder();

        if (decision.getAction() == PrefilterDecision.Action.ACCEPT) {
            Path finalPath = Paths.get(conf.getImgRcvFolder(), finalName);
            fileIO.move(tempPath, finalPath);
            writeMetadataFile(finalPath, image, decision);
            imageFileTracker.addImageReceived(imageId);
            targetPath = finalPath.toString();
        } else if (decision.getAction() == PrefilterDecision.Action.NOISE) {
            String mode = conf.getPrefilterMode();
            if ("drop".equalsIgnoreCase(mode)) {
                Files.deleteIfExists(tempPath);
                imageFileTracker.addImageReceived(imageId);
                targetPath = "DROP";
            } else {
                Path noisePath = Paths.get(conf.getPrefilterNoiseDir(), finalName);
                fileIO.move(tempPath, noisePath);
                writeMetadataFile(noisePath, image, decision);
                imageFileTracker.addImageReceived(imageId);
                targetPath = noisePath.toString();
            }
        }

        LOG.info("Prefilter decision file={} size={} hash={} dist={} motionScore={} edgeRatio={} fgRatio={} fgArea={} largest={} cameraSignal={} nativeActive={} decision={} target={} reason={}",
                finalName,
                size,
                decision.getHashHex(),
                decision.getHammingDistance(),
                String.format(Locale.ROOT, "%.5f", decision.getMotionScore()),
                String.format(Locale.ROOT, "%.5f", decision.getEdgeDiffRatio()),
                String.format(Locale.ROOT, "%.5f", decision.getForegroundRatio()),
                decision.getForegroundArea(),
                decision.getLargestComponentArea(),
                decision.getCameraSignalSummary(),
                decision.isCameraSignalActive(),
                decision.getAction(),
                targetPath,
                decision.getReason());
    }

    private String filenameWithoutExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return filename;
        }
        return filename.substring(0, dot);
    }

    private void writeMetadataFile(Path imagePath, CameraImage image, PrefilterDecision decision) {
        if (imagePath == null || image == null || decision == null) {
            return;
        }

        String imageName = imagePath.getFileName().toString();
        String metadataName = filenameWithoutExtension(imageName) + ".json";
        Path metadataPath = imagePath.resolveSibling(metadataName);
        String timestamp = image.getTimestamp() != null ? image.getTimestamp().toString() : "";
        String contentType = image.getContentType() != null ? image.getContentType() : "";
        String summary = decision.getCameraSignalSummary() != null ? decision.getCameraSignalSummary() : "";
        long sizeBytes = safeSize(imagePath);
        String sha256 = sha256Hex(image.getData());

        String json = "{\n" +
                "  \"source\": \"java\",\n" +
                "  \"capturedAt\": \"" + escapeJson(timestamp) + "\",\n" +
                "  \"contentType\": \"" + escapeJson(contentType) + "\",\n" +
                "  \"frame\": {\n" +
                "    \"sizeBytes\": " + sizeBytes + ",\n" +
                "    \"sha256\": \"" + escapeJson(sha256) + "\"\n" +
                "  },\n" +
                "  \"cameraSignal\": {\n" +
                "    \"motionDetected\": " + image.getCameraSignal().isMotionDetected() + ",\n" +
                "    \"personDetected\": " + image.getCameraSignal().isPersonDetected() + ",\n" +
                "    \"vehicleDetected\": " + image.getCameraSignal().isVehicleDetected() + ",\n" +
                "    \"animalDetected\": " + image.getCameraSignal().isAnimalDetected() + ",\n" +
                "    \"active\": " + image.getCameraSignal().isAnyActive() + ",\n" +
                "    \"summary\": \"" + escapeJson(summary) + "\"\n" +
                "  },\n" +
                "  \"prefilter\": {\n" +
                "    \"action\": \"" + decision.getAction() + "\",\n" +
                "    \"reason\": \"" + escapeJson(decision.getReason()) + "\",\n" +
                "    \"motionScore\": " + formatDouble(decision.getMotionScore()) + ",\n" +
                "    \"edgeDiffRatio\": " + formatDouble(decision.getEdgeDiffRatio()) + ",\n" +
                "    \"foregroundRatio\": " + formatDouble(decision.getForegroundRatio()) + ",\n" +
                "    \"foregroundArea\": " + decision.getForegroundArea() + ",\n" +
                "    \"largestComponentArea\": " + decision.getLargestComponentArea() + "\n" +
                "  }\n" +
                "}\n";

        try {
            Files.writeString(metadataPath, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Unable to write metadata sidecar for {}", imageName, e);
        }
    }

    private long safeSize(Path imagePath) {
        try {
            return Files.size(imagePath);
        } catch (IOException e) {
            return -1L;
        }
    }

    private String sha256Hex(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
