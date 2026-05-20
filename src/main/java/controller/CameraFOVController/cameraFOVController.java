package controller.CameraFOVController;

import java.awt.image.BufferedImage;

public class cameraFOVController implements cameraFOVService {
    
    private cameraFOVFormController formController;
    
    public cameraFOVController(cameraFOVFormController formController) {
        this.formController = formController;
    }
    
    @Override
    public void captureFromCamera() {
        if (formController != null) {
            formController.captureFromCamera();
        }
    }
    
    @Override
    public void captureScreenshot() {
        if (formController != null) {
            formController.captureScreenshot();
        }
    }
    
    @Override
    public BufferedImage getCapturedImage() {
        if (formController != null) {
            return formController.getCapturedImage();
        }
        return null;
    }
}
