package de.zeus.upcam.rest.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class for tracking sent and received image filenames.
 */
public class ImageFileTracker {

    private List<String> imagesSent = new ArrayList<>();
    private final List<String> imagesReceived = new ArrayList<>();

    /**
     * Get the list of filenames of images that have been sent.
     *
     * @return The list of sent image filenames.
     */
    public List<String> getImagesSent() {
        return imagesSent;
    }

    /**
     * Get the list of filenames of images that have been received.
     *
     * @return The list of received image filenames.
     */
    public List<String> getImagesReceived() {
        return imagesReceived;
    }

    /**
     * Add the filename of an image that has been received to the list.
     *
     * @param imageReceived The filename of the received image to add.
     */
    public void addImageReceived(String imageReceived) {
        String normalized = normalizeImageId(imageReceived);
        if (!normalized.isEmpty()) {
            this.imagesReceived.add(normalized);
        }
    }

    /**
     * Set the list of filenames of images that have been sent.
     *
     * @param imagesSent The list of sent image filenames to set.
     */
    public void setImagesSent(List<String> imagesSent) {
        Set<String> normalized = new LinkedHashSet<>();
        if (imagesSent != null) {
            for (String image : imagesSent) {
                String imageId = normalizeImageId(image);
                if (!imageId.isEmpty()) {
                    normalized.add(imageId);
                }
            }
        }
        this.imagesSent = new ArrayList<>(normalized);
    }

    private String normalizeImageId(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        int slash = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        if (slash >= 0 && slash < normalized.length() - 1) {
            normalized = normalized.substring(slash + 1);
        }
        if (normalized.endsWith("_tmp")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }

        int dot = normalized.lastIndexOf('.');
        if (dot > 0) {
            normalized = normalized.substring(0, dot);
        }

        return normalized;
    }
}
