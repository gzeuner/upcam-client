package de.zeus.upcam.rest;

import de.zeus.upcam.rest.camera.CameraImage;
import de.zeus.upcam.rest.camera.CameraSource;
import de.zeus.upcam.rest.camera.CameraSourceFactory;
import de.zeus.upcam.rest.cleanup.CleanupService;
import de.zeus.upcam.rest.client.config.Config;
import de.zeus.upcam.rest.fileio.FileIO;
import de.zeus.upcam.rest.model.ImageFileTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main download coordinator. The cycle logic stays unchanged; only the source is pluggable.
 */
public class UpCamClient {
    private static final Logger LOG = LogManager.getLogger(UpCamClient.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Config conf = Config.getInstance();
    private final RestClientCommands clientCommands = new RestClientCommands();
    private final ImageFileTracker imageFileTracker = new ImageFileTracker();
    private final FileIO fileIO = new FileIO();
    private final CameraSource cameraSource = new CameraSourceFactory(conf).create();
    private final CleanupService cleanupService = new CleanupService();

    public void startImageReceiveProcess() {
        LOG.info("Camera source selected: type={} source={}", conf.getCameraType(), cameraSource.getName());
        int delay = 0;
        int period = conf.getReloadIntervalInSeconds();
        scheduler.scheduleAtFixedRate(this::initImageReceiveProcess, delay, period, TimeUnit.SECONDS);
    }

    public void runSingleCycle() {
        LOG.info("Camera source selected: type={} source={} (single cycle)", conf.getCameraType(), cameraSource.getName());
        initImageReceiveProcess();
    }

    public void stopImageReceiveProcess() {
        scheduler.shutdown();
    }

    private void initImageReceiveProcess() {
        try {
            LOG.info("called 'initImageReceiveProcess()'.....");
            ArrayList<String> knownImages = collectKnownImageBaseNames();
            imageFileTracker.setImagesSent(knownImages);

            List<CameraImage> images = cameraSource.fetchImages(knownImages);
            if (!images.isEmpty()) {
                clientCommands.persistImages(images, imageFileTracker);
            }

            cleanupService.runIfDue();
            LOG.info("Upcam-ReCheck in {} seconds.", conf.getReloadIntervalInSeconds());
        } catch (Exception e) {
            LOG.error("Failure: {}", e.getMessage(), e);
        }
    }

    private ArrayList<String> collectKnownImageBaseNames() {
        ArrayList<String> knownImages = new ArrayList<>(getFilenamesWithoutExt(
                new ArrayList<>(fileIO.listFilesInDir(conf.getImgSntFolder()))));
        knownImages.addAll(getFilenamesWithoutExt(
                new ArrayList<>(fileIO.listFilesInDir(conf.getImgRcvFolder()))));
        if (conf.isPrefilterEnabled()) {
            knownImages.addAll(getFilenamesWithoutExt(
                    new ArrayList<>(fileIO.listFilesInDir(conf.getPrefilterNoiseDir()))));
        }
        return knownImages;
    }

    private List<String> getFilenamesWithoutExt(List<String> filenamesWithExt) {
        List<String> filenamesWithoutExt = new ArrayList<>();
        for (String filename : filenamesWithExt) {
            int dotIndex = filename.lastIndexOf(".");
            if (dotIndex > 0) {
                filenamesWithoutExt.add(filename.substring(0, dotIndex));
            } else {
                filenamesWithoutExt.add(filename);
            }
        }
        return filenamesWithoutExt;
    }

}
