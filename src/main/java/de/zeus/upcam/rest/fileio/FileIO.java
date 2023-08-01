package de.zeus.upcam.rest.fileio;

import de.zeus.upcam.rest.client.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
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

    /**
     * Deletes files in the specified directory that are older than the specified number of days.
     *
     * @param days     The maximum number of days for a file to be considered old.
     * @param dirPath  The path of the directory to delete old files from.
     * @throws IOException If an I/O error occurs during the file deletion process.
     */
    public static void deleteFilesOlderThanNDays(int days, String dirPath) throws IOException {
        long cutOff = System.currentTimeMillis() - ((long) days * 24 * 60 * 60 * 1000);
        Files.list(Paths.get(dirPath))
                .filter(path -> {
                    try {
                        return Files.isRegularFile(path) && Files.getLastModifiedTime(path).to(TimeUnit.MILLISECONDS) < cutOff;
                    } catch (IOException ex) {
                        LOG.error("Error while filtering files by last modified time", ex);
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException ex) {
                        LOG.error("Error while deleting file: {}", path, ex);
                    }
                });
    }
}
