package de.zeus.upcam.rest;

import de.zeus.upcam.rest.client.config.Config;
import de.zeus.upcam.rest.fileio.FileIO;
import de.zeus.upcam.rest.format.Format;
import de.zeus.upcam.rest.model.ImageFileTracker;
import de.zeus.upcam.rest.model.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class handles REST API commands to interact with the camera and download images.
 */
public class RestClientCommands {

    private final RestClientService clientService;
    private final Config conf = Config.getInstance();
    private final HashMap<String, String> headers = new HashMap<>();
    private static final Logger LOG = LogManager.getLogger(RestClientCommands.class);
    private static final FileIO fileIO = new FileIO();

    /**
     * Constructor to initialize RestClientCommands with initial headers.
     */
    public RestClientCommands() {
        this.clientService = new RestClientService();
        setInitialHeaders();
    }

    /**
     * Perform a GET request.
     *
     * @param url The URL to send the GET request to.
     * @return The Response object containing the result of the GET request.
     */
    public Response get(String url) {
        LOG.debug("GET {}", url);
        return clientService.get(url, headers);
    }

    /**
     * Set initial headers required for the REST API calls.
     */
    private void setInitialHeaders() {
        headers.put("Authorization", conf.createBasicAuthHeader(conf.getUpcamUser(), conf.getUpcamPass()));
    }

    /**
     * Get a list of image URLs from the specified URL.
     *
     * @param url The URL to fetch image URLs from.
     * @return List of image URLs.
     */
    public List<String> getImageURLs(String url) {
        List<String> imgUrls = new ArrayList<>();
        getImageFolders(url).forEach(folder -> {
            String folderUrl = url + "/" + folder;
            getImageNames(folderUrl).stream()
                    .filter(imageName -> imageName.toLowerCase().contains("jpg"))
                    .map(imageName -> folderUrl + imageName)
                    .forEach(imgUrls::add);
        });
        return imgUrls;
    }

    /**
     * Get a list of image folders from the specified URL.
     *
     * @param url The URL to fetch image folders from.
     * @return List of image folders.
     */
    private List<String> getImageFolders(String url) {
        LOG.info("getImageFolders("+url+")");
        Response response = get(url);
        getResponseStatus(response);
        return response.getResponseBody()
                .map(body -> Format.parseHtml(body, conf.getImageHtmlPattern()))
                .orElse(new ArrayList<>());
    }

    /**
     * Get a list of image names from the specified folder URL.
     *
     * @param folderUrl The URL of the folder to fetch image names from.
     * @return List of image names.
     */
    private List<String> getImageNames(String folderUrl) {
        LOG.info("getImageNames("+folderUrl+")");
        Response response = get(folderUrl);
        getResponseStatus(response);
        return response.getResponseBody()
                .map(body -> Format.parseHtml(body, conf.getImageHtmlPattern()))
                .orElse(new ArrayList<>());
    }

    /**
     * Download images from the given list of URLs and add them to the ImageFileTracker.
     *
     * @param imgUrls         List of image URLs to download.
     * @param imageFileTracker The ImageFileTracker to keep track of downloaded images.
     */
    public void downloadImages(List<String> imgUrls, ImageFileTracker imageFileTracker) {
        for (String imgUrl : imgUrls) {
            String imgName = imgUrl.substring((imgUrl.lastIndexOf("/")) + 1) + "_tmp";
            String imgNameWithoutExt = imgName.substring(0, imgName.lastIndexOf("."));

            // Check if image name without extension is not in the sent or received images list
            if (!imageFileTracker.getImagesSent().contains(imgNameWithoutExt)
                    && !imageFileTracker.getImagesReceived().contains(imgNameWithoutExt)) {
                HttpGet httpGet = new HttpGet(imgUrl);
                HttpResponse response = clientService.executeImageRequest(httpGet);
                if (response != null) {
                    try (InputStream inputStream = response.getEntity().getContent()) {
                        fileIO.copy(conf.getImgRcvFolder() + imgName, inputStream);
                        imageFileTracker.addImageReceived(imgName);
                        LOG.info("Received from Upcam: {}", imgName);
                        // Call the method to rename the current downloaded image
                        fileIO.renameDownloadedImage(imgName);
                    } catch (IOException e) {
                        LOG.error("Error while downloading or renaming image", e);
                    }
                }
            }
        }
    }

    /**
     * Log the status line of the Response.
     *
     * @param response The Response to log the status line.
     */
    public void getResponseStatus(Response response) {
        LOG.info(response.getStatusLine());
    }
}
