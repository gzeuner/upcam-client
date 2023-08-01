package de.zeus.upcam.rest.model;

import java.util.ArrayList;
import java.util.List;

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
        this.imagesReceived.add(imageReceived);
    }

    /**
     * Set the list of filenames of images that have been sent.
     *
     * @param imagesSent The list of sent image filenames to set.
     */
    public void setImagesSent(List<String> imagesSent) {
        this.imagesSent = imagesSent;
    }

}
