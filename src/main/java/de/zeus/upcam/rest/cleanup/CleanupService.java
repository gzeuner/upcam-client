package de.zeus.upcam.rest.cleanup;

import de.zeus.upcam.rest.client.config.Config;
import de.zeus.upcam.rest.fileio.FileIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Periodic retention cleanup for image folders and log files.
 */
public class CleanupService {
    private static final Logger LOG = LogManager.getLogger(CleanupService.class);

    private final Config conf = Config.getInstance();
    private volatile long lastCleanupAtMillis = 0L;

    public synchronized void runIfDue() {
        if (!conf.isCleanupEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        int intervalSeconds = Math.max(0, conf.getCleanupIntervalSeconds());
        if (lastCleanupAtMillis > 0 && intervalSeconds > 0
                && (now - lastCleanupAtMillis) < (intervalSeconds * 1000L)) {
            return;
        }

        try {
            if (conf.isCleanupImagesEnabled()) {
                cleanImageSubfolders();
            }
            if (conf.isCleanupLogsEnabled()) {
                cleanLogs();
            }
            lastCleanupAtMillis = now;
        } catch (Exception e) {
            LOG.error("Error while running cleanup service", e);
        }
    }

    private void cleanImageSubfolders() {
        int maxAgeDays = conf.getCleanupImagesMaxAgeDays();
        int maxAgeHours = conf.getCleanupImagesMaxAgeHours();
        int maxFilesPerDir = conf.getCleanupImagesMaxFilesPerDir();
        if (maxAgeDays < 0 && maxAgeHours < 0 && maxFilesPerDir < 0) {
            return;
        }

        Path imagesRoot = resolveImagesRoot();
        if (imagesRoot == null || !Files.exists(imagesRoot)) {
            return;
        }

        try (Stream<Path> children = Files.list(imagesRoot)) {
            children
                    .filter(Files::isDirectory)
                    .forEach(path -> cleanImageDir(path, maxAgeDays, maxAgeHours, maxFilesPerDir));
        } catch (IOException e) {
            LOG.error("Error while cleaning image subfolders under {}", imagesRoot, e);
        }
    }

    private void cleanLogs() {
        int maxAgeDays = conf.getCleanupLogsMaxAgeDays();
        if (maxAgeDays < 0) {
            return;
        }
        deleteFilesOlderThan(maxAgeDays, Paths.get(conf.getCleanupLogsDir()));
    }

    private void deleteFilesOlderThan(int maxAgeDays, Path dir) {
        try {
            FileIO.deleteFilesOlderThanNDays(maxAgeDays, dir.toString());
        } catch (IOException e) {
            LOG.error("Error while cleaning directory {}", dir, e);
        }
    }

    private void deleteFilesOlderThanHours(int maxAgeHours, Path dir) {
        try {
            FileIO.deleteFilesOlderThanNHours(maxAgeHours, dir.toString());
        } catch (IOException e) {
            LOG.error("Error while cleaning directory {} by hours", dir, e);
        }
    }

    private void cleanImageDir(Path dir, int maxAgeDays, int maxAgeHours, int maxFilesPerDir) {
        if (maxAgeHours >= 0) {
            deleteFilesOlderThanHours(maxAgeHours, dir);
        } else if (maxAgeDays >= 0) {
            deleteFilesOlderThan(maxAgeDays, dir);
        }

        if (maxFilesPerDir >= 0) {
            FileIO.keepLatestFiles(dir.toString(), maxFilesPerDir);
        }

        if (conf.isPrefilterEnabled() && dir.normalize().equals(Paths.get(conf.getPrefilterNoiseDir()).normalize())) {
            FileIO.keepLatestFiles(conf.getPrefilterNoiseDir(), Math.min(maxFilesPerDir, conf.getPrefilterNoiseMaxFiles()));
        }
    }

    private Path resolveImagesRoot() {
        Path receivedDir = Paths.get(conf.getImgRcvFolder()).normalize();
        Path parent = receivedDir.getParent();
        if (parent != null) {
            return parent;
        }

        Path sentDir = Paths.get(conf.getImgSntFolder()).normalize();
        return sentDir.getParent();
    }
}
