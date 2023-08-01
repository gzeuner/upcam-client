package de.zeus.upcam.rest;

import de.zeus.upcam.rest.client.config.Config;
import de.zeus.upcam.rest.fileio.FileIO;
import de.zeus.upcam.rest.format.Format;
import de.zeus.upcam.rest.model.ImageFileTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MIT License
 *
 * Copyright (c) 2023 Guido Zeuner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * This Class Represents the UpCam-Client that initiates and
 * manages the image receive process from an Upcam device.
 */
public class UpCamClient {
    private static final Logger LOG = LogManager.getLogger(UpCamClient.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Config conf = Config.getInstance();
    private final RestClientCommands clientCommands = new RestClientCommands();
    private final ImageFileTracker imageFileTracker = new ImageFileTracker();
    private final FileIO fileIO = new FileIO();

    /**
     * Starts the image receive process by scheduling periodic image downloads from Upcam.
     */
    public void startImageReceiveProcess() {
        int delay = 0; // Initial delay in seconds (you can adjust this as needed)
        int period = conf.getReloadIntervalInSeconds(); // Period between subsequent executions in seconds

        scheduler.scheduleAtFixedRate(this::initImageReceiveProcess, delay, period, TimeUnit.SECONDS);
    }

    /**
     * Stops the image receive process by shutting down the scheduler.
     */
    public void stopImageReceiveProcess() {
        scheduler.shutdown();
    }

    /**
     * Initiates the image receive process by fetching image URLs from Upcam and downloading new images.
     */
    private void initImageReceiveProcess() {
        try {
            LOG.info("called 'initImageReceiveProcess()'.....");
            String url = Format.prepareUrl(conf.getBaseUrl() + conf.getImageRootResource());
            List<String> imgURLs = clientCommands.getImageURLs(url);
            if (!imgURLs.isEmpty()) {
                ArrayList<String> knownImages = new ArrayList<>(getFilenamesWithoutExt(
                        new ArrayList<>(fileIO.listFilesInDir(conf.getImgSntFolder()))));
                knownImages.addAll(getFilenamesWithoutExt(
                        new ArrayList<>(fileIO.listFilesInDir(conf.getImgRcvFolder()))));
                imageFileTracker.setImagesSent(knownImages);
                clientCommands.downloadImages(imgURLs, imageFileTracker);
            }
            cleanUpImages();
            LOG.info("Upcam-ReCheck in " + conf.getReloadIntervalInSeconds() + " seconds.");
        } catch (Exception e) {
            LOG.error("Failure: " + e.getMessage());
        }
    }

    /**
     * Helper method to get filenames without extension from a list of filenames with extension.
     *
     * @param filenamesWithExt The list of filenames with extensions.
     * @return The list of filenames without extensions.
     */
    private List<String> getFilenamesWithoutExt(List<String> filenamesWithExt) {
        List<String> filenamesWithoutExt = new ArrayList<>();
        for (String filename : filenamesWithExt) {
            filenamesWithoutExt.add(filename.substring(0, filename.lastIndexOf(".")));
        }
        return filenamesWithoutExt;
    }

    /**
     * Cleans up images in the image sent folder that are older than the specified max age.
     */
    private void cleanUpImages() {
        try {
            FileIO.deleteFilesOlderThanNDays(conf.getFileAgeMaxDays(),
                    conf.getImgSntFolder());
        } catch (IOException e) {
            LOG.error("Error while cleaning up " + e.getMessage());
        }
    }
}
