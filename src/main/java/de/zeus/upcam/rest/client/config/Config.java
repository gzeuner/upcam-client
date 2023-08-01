package de.zeus.upcam.rest.client.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private String imageRootResource;
    private String upcamUser;
    private String upcamPass;
    private String baseUrl;
    private String imageHtmlPattern;
    private String imgRcvFolder;
    private String imgSntFolder;
    private int reloadIntervalInSeconds;
    private int fileAgeMaxDays;
    private final Properties properties = new Properties();
    private final static Config config = new Config();

    private Config() {
    }

    /**
     * Returns the singleton instance of the Config class.
     *
     * @return The Config instance.
     */
    public static Config getInstance() {
        return config;
    }

    /**
     * Initializes the configuration by reading properties from the properties file.
     */
    public void init() {
        if (!readPropertiesFile()) {
            System.out.println("Error: Failed to read properties file. Exiting.");
            System.exit(1);
        }

        setBaseUrl("base.url");
        setImageRootResource("image.daily.root.resource");
        setImageHtmlPattern("image.html.pattern");
        setImgRcvFolder("image.local.store.rcv");
        setImgSntFolder("image.local.store.snt");
        setUpcamUser("upcam.user.name");
        setUpcamPass("upcam.user.pwd");
        setReloadIntervalInSeconds("upcam.reload.sec");
        setFileAgeMaxDays("file.max.age.days");
    }

    /**
     * Sets the path to the properties file.
     *
     * @param propertiesFile The path to the properties file.
     */
    public void setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    /**
     * Sets the path to the log4j configuration file and updates the system property.
     *
     * @param log4jConfigFile The path to the log4j configuration file.
     */
    public void setLog4jConfigFile(String log4jConfigFile) {
        this.log4jConfigFile = log4jConfigFile;
        System.setProperty("log4j2.configurationFile", this.log4jConfigFile);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String key) {
        this.baseUrl = getProperty(key);
    }

    public String getImageRootResource() {
        return imageRootResource;
    }

    public void setImageRootResource(String key) {
        this.imageRootResource = getProperty(key);
    }

    public String getImageHtmlPattern() {
        return imageHtmlPattern;
    }

    public void setImageHtmlPattern(String key) {
        this.imageHtmlPattern = getProperty(key);
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
        String value = getProperty(key);
        if (value == null || value.isEmpty()) {
            System.out.println("Error: Property " + key + " is missing. Exiting.");
            System.exit(1);
        }
        try {
            this.reloadIntervalInSeconds = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid value for property " + key + ". Exiting.");
            System.exit(1);
        }
    }

    public int getFileAgeMaxDays() {
        return fileAgeMaxDays;
    }

    public void setFileAgeMaxDays(String key) {
        String value = getProperty(key);
        if (value == null || value.isEmpty()) {
            System.out.println("Error: Property " + key + " is missing. Exiting.");
            System.exit(1);
        }
        try {
            this.fileAgeMaxDays = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid value for property " + key + ". Exiting.");
            System.exit(1);
        }
    }

    public String getUpcamUser() {
        return upcamUser;
    }

    public void setUpcamUser(String key) {
        this.upcamUser = getProperty(key);
    }

    public String getUpcamPass() {
        return upcamPass;
    }

    public void setUpcamPass(String key) {
        this.upcamPass = getProperty(key);
    }

    /**
     * Reads the properties file and loads the properties.
     *
     * @return True if successful, false otherwise.
     */
    private boolean readPropertiesFile() {
        try (FileInputStream propInFile = new FileInputStream(propertiesFile)) {
            properties.load(propInFile);
            return true;
        } catch (IOException e) {
            System.err.println("Error: Failed to read properties file. " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a property value by its key.
     * If the property is missing, it will print an error message and exit the program.
     *
     * @param key The key of the property.
     * @return The value of the property.
     */
    private String getProperty(String key) {
        String property = properties.getProperty(key);
        if (property == null || property.isEmpty()) {
            System.out.println("Error: Property " + key + " is missing. Exiting.");
            System.exit(1);
        }
        return property;
    }

    /**
     * Creates a Basic Authentication header based on the provided username and password.
     *
     * @param username The username.
     * @param password The password.
     * @return The Basic Authentication header string.
     */
    public String createBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String base64encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + base64encodedCredentials;
    }
}
