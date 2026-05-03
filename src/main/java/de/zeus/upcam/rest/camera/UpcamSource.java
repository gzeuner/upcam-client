package de.zeus.upcam.rest.camera;

import de.zeus.upcam.rest.RestClientService;
import de.zeus.upcam.rest.client.config.Config;
import de.zeus.upcam.rest.format.Format;
import de.zeus.upcam.rest.model.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UpcamSource implements CameraSource {
    private static final Logger LOG = LogManager.getLogger(UpcamSource.class);

    private final Config conf;
    private final RestClientService clientService;
    private final HashMap<String, String> headers = new HashMap<>();

    public UpcamSource(Config conf) {
        this.conf = conf;
        this.clientService = new RestClientService();
        headers.put("Authorization", conf.createBasicAuthHeader(conf.getUpcamUser(), conf.getUpcamPass()));
    }

    @Override
    public List<CameraImage> fetchImages(List<String> knownImageNames) {
        List<CameraImage> images = new ArrayList<>();
        String listingUrl = resolveListingUrl();

        for (String imgUrl : getImageURLs(listingUrl)) {
            String finalName = imgUrl.substring((imgUrl.lastIndexOf("/")) + 1);
            String imgNameWithoutExt = filenameWithoutExtension(finalName);
            if (knownImageNames.contains(imgNameWithoutExt)) {
                continue;
            }
            CameraImage image = downloadImage(imgUrl, finalName);
            if (image != null) {
                images.add(image);
            }
        }
        return images;
    }

    @Override
    public String getName() {
        return "UPCAM";
    }

    private String resolveListingUrl() {
        if (conf.getUpcamListingUrl() != null && !conf.getUpcamListingUrl().isEmpty()) {
            return Format.prepareUrl(conf.getUpcamListingUrl());
        }
        return Format.prepareUrl(conf.getBaseUrl() + conf.getImageRootResource());
    }

    private List<String> getImageURLs(String url) {
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

    private List<String> getImageFolders(String url) {
        Response response = clientService.get(url, headers);
        LOG.info(response.getStatusLine());
        return response.getResponseBody()
                .map(body -> Format.parseHtml(body, conf.getImageHtmlPattern()))
                .orElse(new ArrayList<>());
    }

    private List<String> getImageNames(String folderUrl) {
        Response response = clientService.get(folderUrl, headers);
        LOG.info(response.getStatusLine());
        return response.getResponseBody()
                .map(body -> Format.parseHtml(body, conf.getImageHtmlPattern()))
                .orElse(new ArrayList<>());
    }

    private CameraImage downloadImage(String imgUrl, String finalName) {
        HttpGet httpGet = new HttpGet(imgUrl);
        clientService.addHeadersToRequest(httpGet, headers);

        try (CloseableHttpResponse response = clientService.executeImageRequest(httpGet)) {
            if (response == null || response.getEntity() == null) {
                return null;
            }
            String contentType = response.getEntity().getContentType() != null
                    ? response.getEntity().getContentType().getValue() : null;
            byte[] data = readAllBytes(response.getEntity().getContent());
            if (data.length == 0) {
                return null;
            }
            return new CameraImage(data, finalName, Instant.now(), contentType, CameraSignal.none());
        } catch (IOException e) {
            LOG.error("Error while downloading image {}", imgUrl, e);
            return null;
        }
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private String filenameWithoutExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return filename;
        }
        return filename.substring(0, dot);
    }
}
