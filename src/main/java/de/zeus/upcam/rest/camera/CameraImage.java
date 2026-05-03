package de.zeus.upcam.rest.camera;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;

public class CameraImage {
    private final byte[] data;
    private final String suggestedFilename;
    private final Instant timestamp;
    private final String contentType;
    private final CameraSignal cameraSignal;

    public CameraImage(byte[] data, String suggestedFilename, Instant timestamp, String contentType) {
        this(data, suggestedFilename, timestamp, contentType, CameraSignal.none());
    }

    public CameraImage(byte[] data, String suggestedFilename, Instant timestamp, String contentType, CameraSignal cameraSignal) {
        this.data = data;
        this.suggestedFilename = suggestedFilename;
        this.timestamp = timestamp;
        this.contentType = contentType;
        this.cameraSignal = cameraSignal == null ? CameraSignal.none() : cameraSignal;
    }

    public byte[] getData() {
        return data;
    }

    public String getSuggestedFilename() {
        return suggestedFilename;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getContentType() {
        return contentType;
    }

    public CameraSignal getCameraSignal() {
        return cameraSignal;
    }

    public InputStream asInputStream() {
        return new ByteArrayInputStream(data);
    }
}
