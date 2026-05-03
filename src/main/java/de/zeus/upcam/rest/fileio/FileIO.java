package de.zeus.upcam.rest.fileio;

import de.zeus.upcam.rest.client.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for file input/output operations.
 */
public class FileIO {
    private static final Logger LOG = LogManager.getLogger(FileIO.class);
    private static final Config conf = Config.getInstance();

    /**
     * Renames the downloaded image file if it ends with "_tmp".
     *
     * @param imgName The name of the image file to rename.
     */
    public void renameDownloadedImage(String imgName) {
        if (imgName.endsWith("_tmp")) {
            Path src = Paths.get(conf.getImgRcvFolder(), imgName);
            Path dst = Paths.get(conf.getImgRcvFolder(), imgName.replace("_tmp", ""));
            try {
                Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Renamed image: {}", imgName.replace("_tmp", ""));
            } catch (IOException e) {
                LOG.error("Error while renaming image", e);
            }
        }
    }

    /**
     * Lists all regular files in the specified directory.
     *
     * @param dir The path of the directory to list files from.
     * @return A set of filenames in the directory (excluding directories).
     */
    public Set<String> listFilesInDir(String dir) {
        Set<String> fileList = new HashSet<>();
        Path directory = Paths.get(dir);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            LOG.error("Error while creating directory before listing: {}", dir, e);
            return fileList;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path.getFileName().toString());
                }
            }
        } catch (IOException e) {
            LOG.error("Error while listing files in directory: {}", dir, e);
        }
        return fileList;
    }

    /**
     * Copies the content of the input stream to a new file with the specified name.
     *
     * @param imgName      The name of the new file to create.
     * @param inputStream The input stream to read from.
     * @throws IOException If an I/O error occurs during the copying process.
     */
    public void copy(String imgName, InputStream inputStream) throws IOException {
        Files.copy(inputStream, Paths.get(imgName), StandardCopyOption.REPLACE_EXISTING);
    }

    public void ensureDir(String dirPath) {
        try {
            Files.createDirectories(Paths.get(dirPath));
        } catch (IOException e) {
            LOG.error("Error while creating directory: {}", dirPath, e);
        }
    }

    public void move(Path src, Path dst) throws IOException {
        Path parent = dst.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Deletes files in the specified directory that are older than the specified number of days.
     *
     * @param days     The maximum number of days for a file to be considered old.
     * @param dirPath  The path of the directory to delete old files from.
     * @throws IOException If an I/O error occurs during the file deletion process.
     */
    public static void deleteFilesOlderThanNDays(int days, String dirPath) throws IOException {
        if (days < 0) {
            return;
        }
        Path dir = Paths.get(dirPath);
        Files.createDirectories(dir);
        long cutOff = System.currentTimeMillis() - ((long) days * 24 * 60 * 60 * 1000);
        deleteFilesOlderThanCutoff(dir, cutOff);
    }

    public static void deleteFilesOlderThanNHours(int hours, String dirPath) throws IOException {
        if (hours < 0) {
            return;
        }
        Path dir = Paths.get(dirPath);
        Files.createDirectories(dir);
        long cutOff = System.currentTimeMillis() - ((long) hours * 60 * 60 * 1000);
        deleteFilesOlderThanCutoff(dir, cutOff);
    }

    private static void deleteFilesOlderThanCutoff(Path dir, long cutOff) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> {
                        try {
                            return Files.isRegularFile(path) && Files.getLastModifiedTime(path).to(TimeUnit.MILLISECONDS) < cutOff;
                        } catch (IOException ex) {
                            LOG.error("Error while filtering files by last modified time", ex);
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            LOG.error("Error while deleting file: {}", path, ex);
                        }
                    });
        }
    }

    /**
     * Keeps only the newest {@code maxFiles} regular files in a directory and deletes older files.
     *
     * @param dirPath  The directory to clean up.
     * @param maxFiles Number of newest files to keep.
     */
    public static void keepLatestFiles(String dirPath, int maxFiles) {
        if (maxFiles < 0) {
            return;
        }
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir)) {
            return;
        }
        try {
            List<Path> filesByAgeDesc;
            try (Stream<Path> files = Files.list(dir)) {
                filesByAgeDesc = files
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.comparingLong(FileIO::safeLastModified).reversed())
                        .collect(Collectors.toList());
            }

            if (filesByAgeDesc.size() <= maxFiles) {
                return;
            }

            for (int i = maxFiles; i < filesByAgeDesc.size(); i++) {
                Path oldFile = filesByAgeDesc.get(i);
                try {
                    Files.deleteIfExists(oldFile);
                } catch (IOException ex) {
                    LOG.error("Error while deleting old rollover file: {}", oldFile, ex);
                }
            }
        } catch (IOException e) {
            LOG.error("Error while rolling files in directory: {}", dirPath, e);
        }
    }

    private static long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).to(TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }
}
