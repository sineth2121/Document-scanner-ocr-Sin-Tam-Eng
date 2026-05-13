package controller.DashboardController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

public class dashboardFormController implements Initializable {
    @FXML private AnchorPane dropPane;
    @FXML private com.jfoenix.controls.JFXButton btnCapture;
    @FXML private com.jfoenix.controls.JFXButton btnScreenshot;
    @FXML private com.jfoenix.controls.JFXButton btnBrowse;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDragAndDrop();
    }
    
    private void setupDragAndDrop() {
        dropPane.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            e.consume();
        });
        
        dropPane.setOnDragDropped(e -> {
            boolean success = false;
            if (e.getDragboard().hasFiles()) {
                java.io.File file = e.getDragboard().getFiles().get(0);
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
                    if (img != null) {
                        javafx.scene.image.Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(img, null);
                        handleImageCaptured(fxImage);
                        success = true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    @FXML
    private void handleScreenshot() {
        util.CaptureUtils.takeScreenshot(this::handleImageCaptured);
    }

    @FXML
    private void handleBrowse() {
        javafx.stage.Stage stage = (javafx.stage.Stage) dropPane.getScene().getWindow();
        util.CaptureUtils.browseImage(stage, this::handleImageCaptured);
    }

    @FXML
    private void handleCapture() {
        util.CaptureUtils.openLiveCamera(this::handleImageCaptured);
    }

    private void handleImageCaptured(javafx.scene.image.Image img) {
        if (img != null) {
            util.SharedContext.getInstance().addImage(img);
            navigateToScanForm();
        }
    }
    
    private void navigateToScanForm() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) dropPane.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(javafx.fxml.FXMLLoader.load(getClass().getResource("/view/ScanForm.fxml"))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
