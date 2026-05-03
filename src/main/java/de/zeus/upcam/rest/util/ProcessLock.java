package de.zeus.upcam.rest.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public final class ProcessLock {
    private static final Logger LOG = LogManager.getLogger(ProcessLock.class);
    private static FileChannel channel;
    private static FileLock lock;

    private ProcessLock() {
    }

    public static synchronized boolean acquire(String lockFilePath) {
        try {
            Path lockPath = Paths.get(lockFilePath);
            Path parent = lockPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            channel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            if (lock == null) {
                LOG.warn("Lock already held: {}", lockFilePath);
                return false;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(ProcessLock::release));
            LOG.info("Process lock acquired: {}", lockFilePath);
            return true;
        } catch (IOException e) {
            LOG.error("Failed to acquire lock: {}", lockFilePath, e);
            return false;
        }
    }

    public static synchronized void release() {
        try {
            if (lock != null) {
                lock.release();
                lock = null;
            }
        } catch (IOException ignored) {
        }
        try {
            if (channel != null) {
                channel.close();
                channel = null;
            }
        } catch (IOException ignored) {
        }
    }
}
