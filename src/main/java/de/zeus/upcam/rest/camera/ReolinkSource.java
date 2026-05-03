package de.zeus.upcam.rest.camera;

import de.zeus.upcam.rest.client.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReolinkSource implements CameraSource {
    private static final Logger LOG = LogManager.getLogger(ReolinkSource.class);
    private static final DateTimeFormatter NAME_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern TOKEN_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TOKEN_LEASE_PATTERN = Pattern.compile("\"leaseTime\"\\s*:\\s*(\\d+)");
    private static final Pattern MD_STATE_PATTERN = Pattern.compile("\"state\"\\s*:\\s*(\\d+)");
    private static final Pattern PEOPLE_ALARM_PATTERN = Pattern.compile("\"people\"\\s*:\\s*\\{[^}]*\"alarm_state\"\\s*:\\s*(\\d+)", Pattern.DOTALL);
    private static final Pattern VEHICLE_ALARM_PATTERN = Pattern.compile("\"vehicle\"\\s*:\\s*\\{[^}]*\"alarm_state\"\\s*:\\s*(\\d+)", Pattern.DOTALL);
    private static final Pattern DOG_CAT_ALARM_PATTERN = Pattern.compile("\"dog_cat\"\\s*:\\s*\\{[^}]*\"alarm_state\"\\s*:\\s*(\\d+)", Pattern.DOTALL);

    private final Config conf;
    private String loginToken;
    private Instant loginTokenExpiresAt = Instant.EPOCH;

    public ReolinkSource(Config conf) {
        this.conf = conf;
    }

    @Override
    public List<CameraImage> fetchImages(List<String> knownImageNames) {
        List<CameraImage> images = new ArrayList<>();
        CameraImage snapshot = fetchSnapshot();
        if (snapshot != null) {
            images.add(snapshot);
            if (shouldTriggerBurst(snapshot.getCameraSignal())) {
                int burstCount = Math.max(0, conf.getReolinkBurstCount());
                for (int i = 0; i < burstCount; i++) {
                    sleepQuietly(conf.getReolinkBurstIntervalMs());
                    CameraImage burstImage = fetchSnapshot();
                    if (burstImage != null) {
                        images.add(withFilenameSuffix(burstImage, "_b" + (i + 1)));
                    }
                }
            }
        }
        return images;
    }

    @Override
    public String getName() {
        return "REOLINK";
    }

    private CameraImage fetchSnapshot() {
        return fetchViaHttp();
    }

    private boolean shouldTriggerBurst(CameraSignal signal) {
        if (!conf.isReolinkBurstEnabled()) {
            return false;
        }
        if (!conf.isReolinkBurstRequireSignal()) {
            return true;
        }
        return signal != null && signal.isAnyActive();
    }

    private CameraImage withFilenameSuffix(CameraImage image, String suffix) {
        String filename = image.getSuggestedFilename();
        int dot = filename.lastIndexOf('.');
        String nextFilename = dot >= 0
                ? filename.substring(0, dot) + suffix + filename.substring(dot)
                : filename + suffix;
        return new CameraImage(image.getData(), nextFilename, image.getTimestamp(), image.getContentType(), image.getCameraSignal());
    }

    private void sleepQuietly(int millis) {
        long sleepMs = Math.max(0, millis);
        if (sleepMs <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private CameraImage fetchViaHttp() {
        HttpURLConnection connection = null;
        try {
            String snapshotUrl = buildSnapshotUrl();
            URL url = new URL(snapshotUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            String username = conf.getReolinkUsername();
            String password = conf.getReolinkPassword();
            if (!username.isEmpty() || !password.isEmpty()) {
                String credentials = username + ":" + password;
                String auth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + auth);
            }

            if (connection instanceof HttpsURLConnection && !conf.isReolinkVerifyTls()) {
                configureInsecureTls((HttpsURLConnection) connection);
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                LOG.warn("Reolink HTTP snapshot failed with status {}", status);
                return null;
            }

            String contentType = connection.getContentType();
            byte[] data = readAllBytes(connection.getInputStream());
            if (data.length == 0) {
                return null;
            }

            if (!looksLikeImage(data, contentType)) {
                String body = new String(data, StandardCharsets.UTF_8);
                LOG.warn("Reolink HTTP snapshot returned non-image payload: {}", truncate(body, 220));
                if (containsAuthFailure(body)) {
                    return fetchViaHttpWithToken();
                }
                return null;
            }

            return new CameraImage(data, buildSnapshotFilename(contentType), Instant.now(), contentType, fetchCameraSignal());
        } catch (IOException e) {
            LOG.error("Error while fetching Reolink HTTP snapshot", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private CameraImage fetchViaHttpWithToken() {
        String token = requestLoginToken();
        if (token == null || token.isEmpty()) {
            return null;
        }

        HttpURLConnection connection = null;
        try {
            String snapshotUrlWithToken = appendToken(buildSnapshotUrl(), token);
            URL url = new URL(snapshotUrlWithToken);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            if (connection instanceof HttpsURLConnection && !conf.isReolinkVerifyTls()) {
                configureInsecureTls((HttpsURLConnection) connection);
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                LOG.warn("Reolink token snapshot failed with status {}", status);
                return null;
            }

            String contentType = connection.getContentType();
            byte[] data = readAllBytes(connection.getInputStream());
            if (!looksLikeImage(data, contentType)) {
                String body = new String(data, StandardCharsets.UTF_8);
                LOG.warn("Reolink token snapshot returned non-image payload: {}", truncate(body, 220));
                return null;
            }
            return new CameraImage(data, buildSnapshotFilename(contentType), Instant.now(), contentType, fetchCameraSignal());
        } catch (IOException e) {
            LOG.error("Error while fetching Reolink token snapshot", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String requestLoginToken() {
        HttpURLConnection connection = null;
        try {
            String loginUrl = buildLoginUrl();
            URL url = new URL(loginUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Content-Type", "application/json");
            if (connection instanceof HttpsURLConnection && !conf.isReolinkVerifyTls()) {
                configureInsecureTls((HttpsURLConnection) connection);
            }

            String payload = buildLoginPayload();
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            InputStream responseStream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            if (responseStream == null) {
                LOG.warn("Reolink login returned no response stream (status={})", status);
                return null;
            }

            String body = new String(readAllBytes(responseStream), StandardCharsets.UTF_8);
            Matcher matcher = TOKEN_NAME_PATTERN.matcher(body);
            if (matcher.find()) {
                String token = matcher.group(1);
                Matcher leaseMatcher = TOKEN_LEASE_PATTERN.matcher(body);
                int leaseSeconds = leaseMatcher.find() ? Integer.parseInt(leaseMatcher.group(1)) : 300;
                loginToken = token;
                loginTokenExpiresAt = Instant.now().plusSeconds(Math.max(60, leaseSeconds - 60L));
                return token;
            }

            LOG.warn("Reolink login token not found in response: {}", truncate(body, 220));
            return null;
        } catch (IOException e) {
            LOG.error("Error while requesting Reolink login token", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private CameraSignal fetchCameraSignal() {
        try {
            String token = ensureLoginToken();
            if (token == null || token.isEmpty()) {
                return CameraSignal.none();
            }

            String mdBody = executeApiGet("GetMdState", token);
            String aiBody = executeApiGet("GetAiState", token);

            boolean motionDetected = extractBoolean(MD_STATE_PATTERN, mdBody);
            boolean personDetected = extractBoolean(PEOPLE_ALARM_PATTERN, aiBody);
            boolean vehicleDetected = extractBoolean(VEHICLE_ALARM_PATTERN, aiBody);
            boolean animalDetected = extractBoolean(DOG_CAT_ALARM_PATTERN, aiBody);

            return new CameraSignal(motionDetected, personDetected, vehicleDetected, animalDetected);
        } catch (Exception e) {
            LOG.debug("Unable to fetch Reolink native motion state", e);
            return CameraSignal.none();
        }
    }

    private synchronized String ensureLoginToken() {
        if (loginToken != null && !loginToken.isEmpty() && Instant.now().isBefore(loginTokenExpiresAt)) {
            return loginToken;
        }
        loginToken = null;
        loginTokenExpiresAt = Instant.EPOCH;
        return requestLoginToken();
    }

    private String executeApiGet(String command, String token) throws IOException {
        HttpURLConnection connection = null;
        try {
            String baseUrl = buildApiBaseUrl();
            String urlWithCommand = baseUrl + "?cmd=" + command + "&channel=0&token=" + urlEncode(token);
            URL url = new URL(urlWithCommand);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            if (connection instanceof HttpsURLConnection && !conf.isReolinkVerifyTls()) {
                configureInsecureTls((HttpsURLConnection) connection);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            if (stream == null) {
                return "";
            }
            String body = new String(readAllBytes(stream), StandardCharsets.UTF_8);
            if (status == 401 || containsAuthFailure(body) || body.toLowerCase(Locale.ROOT).contains("invalid user")) {
                loginToken = null;
                loginTokenExpiresAt = Instant.EPOCH;
            }
            return body;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildSnapshotUrl() {
        String explicit = conf.getReolinkSnapshotUrl();
        String timestamp = String.valueOf(System.currentTimeMillis());

        if (explicit != null && !explicit.isEmpty()) {
            return applyTemplate(explicit, timestamp);
        }

        String scheme = conf.isReolinkVerifyTls() ? "https" : "http";
        String path = conf.getReolinkSnapshotPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String template = scheme + "://{host}:{port}" + path;
        return applyTemplate(template, timestamp);
    }

    private String buildLoginUrl() {
        return buildApiBaseUrl() + "?cmd=Login";
    }

    private String buildApiBaseUrl() {
        String scheme = conf.isReolinkVerifyTls() ? "https" : "http";
        return scheme + "://" + conf.getReolinkHost() + ":" + conf.getReolinkHttpPort() + "/cgi-bin/api.cgi";
    }

    private String buildLoginPayload() {
        return "[{\"cmd\":\"Login\",\"action\":0,\"param\":{\"User\":{\"Version\":\"0\",\"userName\":\""
                + escapeJson(conf.getReolinkUsername())
                + "\",\"password\":\""
                + escapeJson(conf.getReolinkPassword())
                + "\"}}}]";
    }

    private String applyTemplate(String template, String timestamp) {
        return template
                .replace("{host}", conf.getReolinkHost())
                .replace("{port}", String.valueOf(conf.getReolinkHttpPort()))
                .replace("{username}", conf.getReolinkUsername())
                .replace("{password}", conf.getReolinkPassword())
                .replace("{usernameEncoded}", urlEncode(conf.getReolinkUsername()))
                .replace("{passwordEncoded}", urlEncode(conf.getReolinkPassword()))
                .replace("{timestamp}", timestamp);
    }

    private String buildSnapshotFilename(String contentType) {
        String extension = (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("png")) ? "png" : "jpg";
        return NAME_TS.format(LocalDateTime.now()) + "." + extension;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
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

    private boolean looksLikeImage(byte[] data, String contentType) {
        if (data == null || data.length < 4) {
            return false;
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("image/jpeg") || ct.contains("image/jpg") || ct.contains("image/png")) {
                return true;
            }
        }
        boolean isJpeg = (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
        boolean isPng = (data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47;
        return isJpeg || isPng;
    }

    private boolean containsAuthFailure(String body) {
        if (body == null) {
            return false;
        }
        String normalized = body.toLowerCase(Locale.ROOT);
        return normalized.contains("please login first")
                || normalized.contains("login failed")
                || normalized.contains("\"rspcode\" : -6")
                || normalized.contains("\"rspcode\" : -7");
    }

    private String appendToken(String url, String token) {
        String clean = url.replaceAll("([?&])(user|password)=[^&]*", "$1");
        clean = clean.replaceAll("[?&]+$", "");
        return clean + (clean.contains("?") ? "&" : "?") + "token=" + urlEncode(token);
    }

    private boolean extractBoolean(Pattern pattern, String body) {
        if (body == null || body.isEmpty()) {
            return false;
        }
        Matcher matcher = pattern.matcher(body);
        return matcher.find() && "1".equals(matcher.group(1));
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void configureInsecureTls(HttpsURLConnection connection) {
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (GeneralSecurityException e) {
            LOG.warn("Unable to configure insecure TLS for Reolink request", e);
        }
    }
}
