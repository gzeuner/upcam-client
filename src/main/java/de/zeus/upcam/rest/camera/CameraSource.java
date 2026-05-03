package de.zeus.upcam.rest.camera;

import java.util.List;

public interface CameraSource {
    List<CameraImage> fetchImages(List<String> knownImageNames);

    String getName();
}
