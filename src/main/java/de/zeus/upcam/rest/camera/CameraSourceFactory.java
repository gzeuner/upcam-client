package de.zeus.upcam.rest.camera;

import de.zeus.upcam.rest.client.config.Config;

import java.util.Locale;

public class CameraSourceFactory {

    private final Config conf;

    public CameraSourceFactory(Config conf) {
        this.conf = conf;
    }

    public CameraSource create() {
        String type = conf.getCameraType();
        if ("REOLINK".equalsIgnoreCase(type)) {
            return new ReolinkSource(conf);
        }
        return new UpcamSource(conf);
    }

    public static String normalizeType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "UPCAM";
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }
}
