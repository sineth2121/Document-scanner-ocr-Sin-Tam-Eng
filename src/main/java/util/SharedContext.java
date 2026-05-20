package util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

public class SharedContext {
    private static final SharedContext instance = new SharedContext();

    private final ObservableList<Image> capturedImages;
    private Image currentlySelectedImage;

    /** Stores the OCR-extracted text for each captured image. */
    private final Map<Image, String> imageTextMap;

    private SharedContext() {
        capturedImages = FXCollections.observableArrayList();
        imageTextMap   = new HashMap<>();
    }

    public static SharedContext getInstance() {
        return instance;
    }

    public ObservableList<Image> getCapturedImages() {
        return capturedImages;
    }

    public void addImage(Image img) {
        if (img != null) {
            capturedImages.add(img);
            currentlySelectedImage = img;
        }
    }

    public Image getCurrentlySelectedImage() {
        return currentlySelectedImage;
    }

    public void setCurrentlySelectedImage(Image img) {
        this.currentlySelectedImage = img;
    }

    // ── Per-image OCR text ──────────────────────────────────────────────────

    /**
     * Store OCR text for a specific image.
     */
    public void setImageText(Image img, String text) {
        if (img != null) {
            imageTextMap.put(img, text);
        }
    }

    /**
     * Retrieve previously stored OCR text for an image.
     * Returns null if no text has been extracted yet.
     */
    public String getImageText(Image img) {
        return imageTextMap.get(img);
    }

    /**
     * Re-key text entry when an image is replaced (e.g. after rotate).
     * Transfers the old image's text to the new image key.
     */
    public void transferImageText(Image oldImg, Image newImg) {
        if (oldImg != null && newImg != null && imageTextMap.containsKey(oldImg)) {
            imageTextMap.put(newImg, imageTextMap.remove(oldImg));
        } else if (oldImg != null) {
            imageTextMap.remove(oldImg);
        }
    }

    /**
     * Remove text entry for a deleted image.
     */
    public void removeImageText(Image img) {
        if (img != null) {
            imageTextMap.remove(img);
        }
    }

    /**
     * Clear all stored text (called when session is cleared).
     */
    public void clearAllText() {
        imageTextMap.clear();
    }
}
