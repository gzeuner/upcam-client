package de.zeus.upcam.rest.client.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

/**
 * Singleton class for managing application configuration properties.
 */
public class Config {

    private String propertiesFile;
    private String log4jConfigFile;

    private String cameraType;

    private String imageRootResource;
    private String upcamUser;
    private String upcamPass;
    private String baseUrl;
    private String imageHtmlPattern;
    private String upcamListingUrl;

    private String imgRcvFolder;
    private String imgSntFolder;
    private int reloadIntervalInSeconds;
    private int fileAgeMaxDays;
    private boolean cleanupEnabled;
    private int cleanupIntervalSeconds;
    private boolean cleanupImagesEnabled;
    private int cleanupImagesMaxAgeDays;
    private int cleanupImagesMaxAgeHours;
    private int cleanupImagesMaxFilesPerDir;
    private boolean cleanupLogsEnabled;
    private String cleanupLogsDir;
    private int cleanupLogsMaxAgeDays;

    private boolean prefilterEnabled;
    private String prefilterHashType;
    private int prefilterResizeWidth;
    private int prefilterCropTopPx;
    private int prefilterHammingThreshold;
    private String prefilterMode;
    private String prefilterNoiseDir;
    private String prefilterStateFile;
    private int prefilterMinBytes;
    private String prefilterFailMode;
    private int prefilterNoiseMaxFiles;
    private int prefilterMaxSuppressSeconds;
    private boolean prefilterCameraSignalBypass;
    private boolean prefilterObserveOnly;

    private String lockFile;

    private String reolinkHost;
    private int reolinkHttpPort;
    private String reolinkUsername;
    private String reolinkPassword;
    private String reolinkSnapshotPath;
    private String reolinkSnapshotUrl;
    private boolean reolinkVerifyTls;
    private boolean reolinkBurstEnabled;
    private int reolinkBurstCount;
    private int reolinkBurstIntervalMs;
    private boolean reolinkBurstRequireSignal;

    private final Properties properties = new Properties();
    private static final Config config = new Config();

    private Config() {
    }

    public static Config getInstance() {
        return config;
    }

    public void init() {
        if (!readPropertiesFile()) {
            System.out.println("Error: Failed to read properties file. Exiting.");
            System.exit(1);
        }

        setCameraType("camera.type");

        setBaseUrl("base.url");
        setImageRootResource("image.daily.root.resource");
        setImageHtmlPattern("image.html.pattern");
        setUpcamListingUrl("upcam.listingUrl");
        setUpcamUser("upcam.user.name");
        setUpcamPass("upcam.user.pwd");

        setImgRcvFolder("image.local.store.rcv");
        setImgSntFolder("image.local.store.snt");
        setReloadIntervalInSeconds("upcam.reload.sec");
        setFileAgeMaxDays("file.max.age.days");
        setCleanupEnabled("cleanup.enabled");
        setCleanupIntervalSeconds("cleanup.interval.seconds");
        setCleanupImagesEnabled("cleanup.images.enabled");
        setCleanupImagesMaxAgeDays("cleanup.images.maxAgeDays");
        setCleanupImagesMaxAgeHours("cleanup.images.maxAgeHours");
        setCleanupImagesMaxFilesPerDir("cleanup.images.maxFilesPerDir");
        setCleanupLogsEnabled("cleanup.logs.enabled");
        setCleanupLogsDir("cleanup.logs.dir");
        setCleanupLogsMaxAgeDays("cleanup.logs.maxAgeDays");

        setPrefilterEnabled("prefilter.enabled");
        setPrefilterHashType("prefilter.hashType");
        setPrefilterResizeWidth("prefilter.resizeWidth");
        setPrefilterCropTopPx("prefilter.cropTopPx");
        setPrefilterHammingThreshold("prefilter.hammingThreshold");
        setPrefilterMode("prefilter.mode");
        setPrefilterNoiseDir("prefilter.noiseDir");
        setPrefilterStateFile("prefilter.stateFile");
        setPrefilterMinBytes("prefilter.minBytes");
        setPrefilterFailMode("prefilter.failMode");
        setPrefilterNoiseMaxFiles("prefilter.noise.maxFiles");
        setPrefilterMaxSuppressSeconds("prefilter.maxSuppressSeconds");
        setPrefilterCameraSignalBypass("prefilter.cameraSignalBypass");
        setPrefilterObserveOnly("prefilter.observeOnly");

        setLockFile("lock.file");

        setReolinkHost("reolink.host");
        setReolinkHttpPort("reolink.httpPort");
        setReolinkUsername("reolink.username");
        setReolinkPassword("reolink.password");
        setReolinkSnapshotPath("reolink.snapshotPath");
        setReolinkSnapshotUrl("reolink.snapshotUrl");
        setReolinkVerifyTls("reolink.verifyTls");
        setReolinkBurstEnabled("reolink.burst.enabled");
        setReolinkBurstCount("reolink.burst.count");
        setReolinkBurstIntervalMs("reolink.burst.intervalMs");
        setReolinkBurstRequireSignal("reolink.burst.requireSignal");

        validateCameraSpecificConfiguration();
    }

    public void setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public void setLog4jConfigFile(String log4jConfigFile) {
        this.log4jConfigFile = log4jConfigFile;
        System.setProperty("log4j2.configurationFile", this.log4jConfigFile);
    }

    public String getCameraType() {
        return cameraType;
    }

    public void setCameraType(String key) {
        this.cameraType = getOptionalProperty(key, "UPCAM").toUpperCase();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String key) {
        this.baseUrl = getOptionalProperty(key, "");
    }

    public String getImageRootResource() {
        return imageRootResource;
    }

    public void setImageRootResource(String key) {
        this.imageRootResource = getOptionalProperty(key, "/sd/${day}");
    }

    public String getImageHtmlPattern() {
        return imageHtmlPattern;
    }

    public void setImageHtmlPattern(String key) {
        this.imageHtmlPattern = getOptionalProperty(key, "a[href*=images]");
    }

    public String getUpcamListingUrl() {
        return upcamListingUrl;
    }

    public void setUpcamListingUrl(String key) {
        this.upcamListingUrl = getOptionalProperty(key, "");
    }

    public String getImgRcvFolder() {
        return imgRcvFolder;
    }

    public void setImgRcvFolder(String key) {
        this.imgRcvFolder = getProperty(key);
    }

    public String getImgSntFolder() {
        return imgSntFolder;
    }

    public void setImgSntFolder(String key) {
        this.imgSntFolder = getProperty(key);
    }

    public int getReloadIntervalInSeconds() {
        return reloadIntervalInSeconds;
    }

    public void setReloadIntervalInSeconds(String key) {
        this.reloadIntervalInSeconds = getIntProperty(key, 20);
    }

    public int getFileAgeMaxDays() {
        return fileAgeMaxDays;
    }

    public void setFileAgeMaxDays(String key) {
        this.fileAgeMaxDays = getIntProperty(key, 2);
    }

    public boolean isCleanupEnabled() {
        return cleanupEnabled;
    }

    public void setCleanupEnabled(String key) {
        this.cleanupEnabled = Boolean.parseBoolean(getOptionalProperty(key, "true"));
    }

    public int getCleanupIntervalSeconds() {
        return cleanupIntervalSeconds;
    }

    public void setCleanupIntervalSeconds(String key) {
        this.cleanupIntervalSeconds = getIntProperty(key, 300);
    }

    public boolean isCleanupImagesEnabled() {
        return cleanupImagesEnabled;
    }

    public void setCleanupImagesEnabled(String key) {
        this.cleanupImagesEnabled = Boolean.parseBoolean(getOptionalProperty(key, "true"));
    }

    public int getCleanupImagesMaxAgeDays() {
        return cleanupImagesMaxAgeDays;
    }

    public void setCleanupImagesMaxAgeDays(String key) {
        this.cleanupImagesMaxAgeDays = getIntProperty(key, getFileAgeMaxDays());
    }

    public int getCleanupImagesMaxAgeHours() {
        return cleanupImagesMaxAgeHours;
    }

    public void setCleanupImagesMaxAgeHours(String key) {
        this.cleanupImagesMaxAgeHours = getIntProperty(key, 12);
    }

    public int getCleanupImagesMaxFilesPerDir() {
        return cleanupImagesMaxFilesPerDir;
    }

    public void setCleanupImagesMaxFilesPerDir(String key) {
        this.cleanupImagesMaxFilesPerDir = getIntProperty(key, 250);
    }

    public boolean isCleanupLogsEnabled() {
        return cleanupLogsEnabled;
    }

    public void setCleanupLogsEnabled(String key) {
        this.cleanupLogsEnabled = Boolean.parseBoolean(getOptionalProperty(key, "true"));
    }

    public String getCleanupLogsDir() {
        return cleanupLogsDir;
    }

    public void setCleanupLogsDir(String key) {
        this.cleanupLogsDir = getOptionalProperty(key, "./logs/");
    }

    public int getCleanupLogsMaxAgeDays() {
        return cleanupLogsMaxAgeDays;
    }

    public void setCleanupLogsMaxAgeDays(String key) {
        this.cleanupLogsMaxAgeDays = getIntProperty(key, 30);
    }

    public boolean isPrefilterEnabled() {
        return prefilterEnabled;
    }

    public void setPrefilterEnabled(String key) {
        this.prefilterEnabled = Boolean.parseBoolean(getOptionalProperty(key, "true"));
    }

    public String getPrefilterHashType() {
        return prefilterHashType;
    }

    public void setPrefilterHashType(String key) {
        this.prefilterHashType = getOptionalProperty(key, "dh");
    }

    public int getPrefilterResizeWidth() {
        return prefilterResizeWidth;
    }

    public void setPrefilterResizeWidth(String key) {
        this.prefilterResizeWidth = getIntProperty(key, 384);
    }

    public int getPrefilterCropTopPx() {
        return prefilterCropTopPx;
    }

    public void setPrefilterCropTopPx(String key) {
        this.prefilterCropTopPx = getIntProperty(key, 80);
    }

    public int getPrefilterHammingThreshold() {
        return prefilterHammingThreshold;
    }

    public void setPrefilterHammingThreshold(String key) {
        this.prefilterHammingThreshold = getIntProperty(key, 8);
    }

    public String getPrefilterMode() {
        return prefilterMode;
    }

    public void setPrefilterMode(String key) {
        this.prefilterMode = getOptionalProperty(key, "quarantine");
    }

    public String getPrefilterNoiseDir() {
        return prefilterNoiseDir;
    }

    public void setPrefilterNoiseDir(String key) {
        this.prefilterNoiseDir = getOptionalProperty(key, "./images/noise/");
    }

    public String getPrefilterStateFile() {
        return prefilterStateFile;
    }

    public void setPrefilterStateFile(String key) {
        this.prefilterStateFile = getOptionalProperty(key, "./.state/upcam_prefilter_state.json");
    }

    public int getPrefilterMinBytes() {
        return prefilterMinBytes;
    }

    public void setPrefilterMinBytes(String key) {
        this.prefilterMinBytes = getIntProperty(key, 10000);
    }

    public String getPrefilterFailMode() {
        return prefilterFailMode;
    }

    public void setPrefilterFailMode(String key) {
        this.prefilterFailMode = getOptionalProperty(key, "open");
    }

    public int getPrefilterNoiseMaxFiles() {
        return prefilterNoiseMaxFiles;
    }

    public void setPrefilterNoiseMaxFiles(String key) {
        this.prefilterNoiseMaxFiles = getIntProperty(key, 20);
    }

    public int getPrefilterMaxSuppressSeconds() {
        return prefilterMaxSuppressSeconds;
    }

    public void setPrefilterMaxSuppressSeconds(String key) {
        this.prefilterMaxSuppressSeconds = getIntProperty(key, 0);
    }

    public boolean isPrefilterCameraSignalBypass() {
        return prefilterCameraSignalBypass;
    }

    public void setPrefilterCameraSignalBypass(String key) {
        this.prefilterCameraSignalBypass = Boolean.parseBoolean(getOptionalProperty(key, "true"));
    }

    public boolean isPrefilterObserveOnly() {
        return prefilterObserveOnly;
    }

    public void setPrefilterObserveOnly(String key) {
        this.prefilterObserveOnly = Boolean.parseBoolean(getOptionalProperty(key, "true"));
    }

    public String getLockFile() {
        return lockFile;
    }

    public void setLockFile(String key) {
        this.lockFile = getOptionalProperty(key, "./.lock/upcamclient.lock");
    }

    public String getUpcamUser() {
        return upcamUser;
    }

    public void setUpcamUser(String key) {
        this.upcamUser = getOptionalProperty(key, "");
    }

    public String getUpcamPass() {
        return upcamPass;
    }

    public void setUpcamPass(String key) {
        this.upcamPass = getOptionalProperty(key, "");
    }

    public String getReolinkHost() {
        return reolinkHost;
    }

    public void setReolinkHost(String key) {
        this.reolinkHost = getOptionalProperty(key, "");
    }

    public int getReolinkHttpPort() {
        return reolinkHttpPort;
    }

    public void setReolinkHttpPort(String key) {
        this.reolinkHttpPort = getIntProperty(key, 80);
    }

    public String getReolinkUsername() {
        return reolinkUsername;
    }

    public void setReolinkUsername(String key) {
        this.reolinkUsername = getOptionalProperty(key, "");
    }

    public String getReolinkPassword() {
        return reolinkPassword;
    }

    public void setReolinkPassword(String key) {
        this.reolinkPassword = getOptionalProperty(key, "");
    }

    public String getReolinkSnapshotPath() {
        return reolinkSnapshotPath;
    }

    public void setReolinkSnapshotPath(String key) {
        this.reolinkSnapshotPath = getOptionalProperty(key, "/cgi-bin/api.cgi?cmd=Snap&channel=0&rs={timestamp}");
    }

    public String getReolinkSnapshotUrl() {
        return reolinkSnapshotUrl;
    }

    public void setReolinkSnapshotUrl(String key) {
        this.reolinkSnapshotUrl = getOptionalProperty(key, "");
    }

    public boolean isReolinkVerifyTls() {
        return reolinkVerifyTls;
    }

    public void setReolinkVerifyTls(String key) {
        this.reolinkVerifyTls = Boolean.parseBoolean(getOptionalProperty(key, "false"));
    }

    public boolean isReolinkBurstEnabled() {
        return reolinkBurstEnabled;
    }

    public void setReolinkBurstEnabled(String key) {
        this.reolinkBurstEnabled = Boolean.parseBoolean(getOptionalProperty(key, "true"));
    }

    public int getReolinkBurstCount() {
        return reolinkBurstCount;
    }

    public void setReolinkBurstCount(String key) {
        this.reolinkBurstCount = getIntProperty(key, 2);
    }

    public int getReolinkBurstIntervalMs() {
        return reolinkBurstIntervalMs;
    }

    public void setReolinkBurstIntervalMs(String key) {
        this.reolinkBurstIntervalMs = getIntProperty(key, 350);
    }

    public boolean isReolinkBurstRequireSignal() {
        return reolinkBurstRequireSignal;
    }

    public void setReolinkBurstRequireSignal(String key) {
        this.reolinkBurstRequireSignal = Boolean.parseBoolean(getOptionalProperty(key, "false"));
    }

    private boolean readPropertiesFile() {
        try (FileInputStream propInFile = new FileInputStream(propertiesFile)) {
            properties.load(propInFile);
            return true;
        } catch (IOException e) {
            System.err.println("Error: Failed to read properties file. " + e.getMessage());
            return false;
        }
    }

    private String getProperty(String key) {
        String property = properties.getProperty(key);
        if (property == null || property.isEmpty()) {
            System.out.println("Error: Property " + key + " is missing. Exiting.");
            System.exit(1);
        }
        return property;
    }

    private String getOptionalProperty(String key, String defaultValue) {
        String property = properties.getProperty(key);
        if (property == null || property.trim().isEmpty()) {
            return defaultValue;
        }
        return property.trim();
    }

    private int getIntProperty(String key, int defaultValue) {
        String value = getOptionalProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid value for property " + key + ". Exiting.");
            System.exit(1);
            return defaultValue;
        }
    }

    public String createBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String base64encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + base64encodedCredentials;
    }

    private void validateCameraSpecificConfiguration() {
        String type = cameraType == null ? "" : cameraType.trim().toUpperCase();
        if ("UPCAM".equals(type)) {
            requireNotBlank(baseUrl, "base.url");
            requireNotBlank(imageRootResource, "image.daily.root.resource");
            requireNotBlank(imageHtmlPattern, "image.html.pattern");
            requireNotBlank(upcamUser, "upcam.user.name");
            requireNotBlank(upcamPass, "upcam.user.pwd");
            return;
        }

        if ("REOLINK".equals(type)) {
            requireNotBlank(reolinkHost, "reolink.host");
            return;
        }

        System.out.println("Error: Unsupported camera.type '" + cameraType + "'. Use UPCAM or REOLINK. Exiting.");
        System.exit(1);
    }

    private void requireNotBlank(String value, String key) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println("Error: Property " + key + " is missing for camera.type=" + cameraType + ". Exiting.");
            System.exit(1);
        }
    }
}
