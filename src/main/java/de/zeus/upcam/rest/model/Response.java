package de.zeus.upcam.rest.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

/**
 * Represents the response received from an HTTP request.
 */
public class Response {

    public static final Log LOG = LogFactory.getLog(Response.class);

    private final HttpResponse httpResponse;

    private Optional<String> responseBody;

    /**
     * Creates a new instance of Response with the given HttpResponse.
     *
     * @param httpResponse The HttpResponse to wrap.
     */
    public Response(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
        setResponseBody();
    }



    /**
     * Gets the response body as a string.
     *
     * @return The response body as an Optional string.
     */
    public Optional<String> getResponseBody() {
        return responseBody;
    }

    /**
     * Sets the response body by reading it from the HttpResponse entity.
     */
    private void setResponseBody() {
        try {
            responseBody = Optional.of(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.error("Error while reading response body", e);
            responseBody = Optional.empty();
        }
    }

    /**
     * Gets the StatusLine of the response.
     *
     * @return The StatusLine of the response.
     */
    public StatusLine getStatusLine() {
        return getHttpResponse().map(HttpResponse::getStatusLine).orElseGet(() -> new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("Not provided", 0, 0);
            }

            @Override
            public int getStatusCode() {
                return 0;
            }

            @Override
            public String getReasonPhrase() {
                return "No status available";
            }
        });
    }

    /**
     * Gets the underlying HttpResponse.
     *
     * @return The HttpResponse wrapped by this Response.
     */
    public Optional<HttpResponse> getHttpResponse() {
        return Optional.ofNullable(httpResponse);
    }
}
