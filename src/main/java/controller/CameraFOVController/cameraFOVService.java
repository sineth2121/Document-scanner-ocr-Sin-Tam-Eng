package controller.CameraFOVController;

import java.awt.image.BufferedImage;

public interface cameraFOVService {
    void captureFromCamera();
    void captureScreenshot();
    BufferedImage getCapturedImage();
}
