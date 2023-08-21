package de.zeus.upcam.rest;

import de.zeus.upcam.rest.model.Response;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Service class for making RESTful HTTP requests.
 */
public class RestClientService {

    private static final Logger LOG = LogManager.getLogger(RestClientService.class);
    private static CloseableHttpClient client;

    /**
     * Constructs a new RestClientService instance. Creates a new HttpClient if one does not exist.
     */
    public RestClientService() {
        if (client == null) {
            createHttpClient();
        }
    }

    /**
     * Creates a singleton instance of CloseableHttpClient.
     */
    private synchronized void createHttpClient() {
        if (client == null) {
            RequestConfig config = RequestConfig.custom()
                    .setSocketTimeout(15000)  // Sets the timeout for data transfers to 15 seconds
                    .setConnectionRequestTimeout(15000)  // Sets the timeout for the connection request to 15 seconds
                    .build();

            client = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .build();
        }
    }

    /**
     * Sends a GET request to the specified URL with custom headers.
     *
     * @param url     The URL to request.
     * @param headers The custom headers to add to the request.
     * @return The response from the server as a Response object.
     */
    public Response get(String url, HashMap<String, String> headers) {
        HttpUriRequest httpUriRequest = new HttpGet(url);
        addHeadersToRequest(httpUriRequest, headers);
        return execute(httpUriRequest);
    }

    /**
     * Executes the provided HttpUriRequest and returns the response as a Response object.
     *
     * @param httpUriRequest The HttpUriRequest to execute.
     * @return The response from the server as a Response object.
     */
    public Response execute(HttpUriRequest httpUriRequest) {
        try {
            HttpResponse httpResponse = client.execute(httpUriRequest);
            return new Response(httpResponse);
        } catch (IOException e) {
            LOG.error("Error while executing HTTP request", e);
        }
        return new Response(null);
    }


    /**
     * Executes the provided HttpUriRequest and returns the HttpResponse.
     * Used specifically for image requests.
     *
     * @param httpUriRequest The HttpUriRequest to execute.
     * @return The HttpResponse from the server.
     */
    public HttpResponse executeImageRequest(HttpUriRequest httpUriRequest) {
        try {
            LOG.debug("GET " + httpUriRequest.getURI().toString());
            return client.execute(httpUriRequest);
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }

    /**
     * Adds custom headers to the HttpRequest.
     *
     * @param httpRequest The HttpRequest to which headers will be added.
     * @param headers     The custom headers to add to the request.
     */
    public void addHeadersToRequest(HttpRequest httpRequest, HashMap<String, String> headers) {
        for (Entry<String, String> header : headers.entrySet()) {
            httpRequest.addHeader(header.getKey(), header.getValue());
        }
    }

    /**
     * Closes the HttpClient instance and sets it to null.
     */
    public void closeHttpClient() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                LOG.error(e);
            } finally {
                client = null;
            }
        }
    }

}
