package util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

public class SharedContext {
    private static final SharedContext instance = new SharedContext();
    
    private final ObservableList<Image> capturedImages;
    private Image currentlySelectedImage;
    
    private SharedContext() {
        capturedImages = FXCollections.observableArrayList();
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
}
