package controller.ScanFromController;

public class scanController implements scanService {
    
    @Override
    public void processImage(String language) {
        System.out.println("Processing image with language: " + language);
    }
    
    @Override
    public void captureImage() {
        System.out.println("Capture image");
    }
    
    @Override
    public void browseAndAdd() {
        System.out.println("Browse and add image");
    }
}
