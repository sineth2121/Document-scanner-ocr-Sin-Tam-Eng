package controller.CameraFOVController;

import com.github.sarxos.webcam.Webcam;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class cameraFOVFormController implements Initializable, cameraFOVService {
    
    @FXML
    private AnchorPane cameraPane;
    
    private BufferedImage capturedImage;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupButtonHandlers();
    }
    
    @FXML
    public void captureImage() {
        Stage liveStage = new Stage();
        liveStage.setTitle("Live Camera");
        
        AnchorPane root = new AnchorPane();
        ImageView liveView = new ImageView();
        liveView.setFitWidth(640);
        liveView.setFitHeight(480);
        liveView.setPreserveRatio(true);
        
        javafx.scene.control.Button snapBtn = new javafx.scene.control.Button("Take Photo");
        snapBtn.setStyle("-fx-background-color: #0D99FF; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8;");
        snapBtn.setPrefWidth(150);
        snapBtn.setPrefHeight(40);
        snapBtn.setLayoutX(245);
        snapBtn.setLayoutY(490);
        
        root.getChildren().addAll(liveView, snapBtn);
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 640, 540);
        liveStage.setScene(scene);
        
        try {
            Webcam tempWebcam = Webcam.getDefault();
            if (tempWebcam != null) {
                tempWebcam.setViewSize(new java.awt.Dimension(640, 480));
                tempWebcam.open();
                
                AnimationTimer timer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        BufferedImage img = tempWebcam.getImage();
                        if (img != null) {
                            liveView.setImage(SwingFXUtils.toFXImage(img, null));
                        }
                    }
                };
                timer.start();
                
                snapBtn.setOnAction(e -> {
                    capturedImage = tempWebcam.getImage();
                    if (capturedImage != null) {
                        displayCapturedImage(capturedImage);
                        System.out.println("Image captured from live feed!");
                    }
                    timer.stop();
                    tempWebcam.close();
                    liveStage.close();
                });
                
                liveStage.setOnCloseRequest(e -> {
                    timer.stop();
                    tempWebcam.close();
                });
                
                liveStage.show();
            }
        } catch (Exception e) {
            System.err.println("Camera initialization error: " + e.getMessage());
        }
    }
    
    private void displayCapturedImage(BufferedImage img) {
        if (img != null && cameraPane.getWidth() > 0) {
            WritableImage fxImage = SwingFXUtils.toFXImage(img, null);
            Platform.runLater(() -> {
                ImageView imageView;
                if (cameraPane.getChildren().isEmpty()) {
                    imageView = new ImageView(fxImage);
                    cameraPane.getChildren().add(imageView);
                } else {
                    imageView = (ImageView) cameraPane.getChildren().get(0);
                    imageView.setImage(fxImage);
                }
                
                double imgW = fxImage.getWidth();
                double imgH = fxImage.getHeight();
                double paneW = cameraPane.getWidth();
                double paneH = cameraPane.getHeight();
                
                double scale = Math.min(paneW / imgW, paneH / imgH);
                double scaledW = imgW * scale;
                double scaledH = imgH * scale;
                
                imageView.setFitWidth(scaledW);
                imageView.setFitHeight(scaledH);
                imageView.setPreserveRatio(true);
                
                imageView.setLayoutX((paneW - scaledW) / 2);
                imageView.setLayoutY((paneH - scaledH) / 2);
                
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(-imageView.getLayoutX(), -imageView.getLayoutY(), paneW, paneH);
                clip.setArcWidth(48);
                clip.setArcHeight(48);
                imageView.setClip(clip);
            });
        }
    }
    
    @FXML
    public void takeScreenshot() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = "screenshot_" + timestamp + ".png";
            String desktopPath = System.getProperty("user.home") + "/Desktop/" + fileName;
            
            if (capturedImage != null) {
                ImageIO.write(capturedImage, "PNG", new File(desktopPath));
                System.out.println("Screenshot saved: " + desktopPath);
            }
        } catch (Exception e) {
            System.err.println("Screenshot error: " + e.getMessage());
        }
    }
    
    @FXML
    public void browseBrowse() {
        Stage stage = (Stage) cameraPane.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Browse Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                capturedImage = ImageIO.read(selectedFile);
                displayBrowsedImage(selectedFile);
                System.out.println("Image browsed: " + selectedFile.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("Browse image error: " + e.getMessage());
            }
        }
    }
    
    private void displayBrowsedImage(File imageFile) {
        try {
            Image image = new Image(imageFile.toURI().toString());
            if (cameraPane.getChildren().isEmpty()) {
                ImageView imageView = new ImageView(image);
                
                double imgW = image.getWidth();
                double imgH = image.getHeight();
                double paneW = cameraPane.getWidth();
                double paneH = cameraPane.getHeight();
                
                double scale = Math.min(paneW / imgW, paneH / imgH);
                double scaledW = imgW * scale;
                double scaledH = imgH * scale;
                
                imageView.setFitWidth(scaledW);
                imageView.setFitHeight(scaledH);
                imageView.setPreserveRatio(true);
                
                imageView.setLayoutX((paneW - scaledW) / 2);
                imageView.setLayoutY((paneH - scaledH) / 2);
                
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(-imageView.getLayoutX(), -imageView.getLayoutY(), paneW, paneH);
                clip.setArcWidth(48);
                clip.setArcHeight(48);
                imageView.setClip(clip);
                
                cameraPane.getChildren().add(imageView);
            } else {
                ImageView imageView = (ImageView) cameraPane.getChildren().get(0);
                imageView.setImage(image);
                
                double imgW = image.getWidth();
                double imgH = image.getHeight();
                double paneW = cameraPane.getWidth();
                double paneH = cameraPane.getHeight();
                
                double scale = Math.min(paneW / imgW, paneH / imgH);
                double scaledW = imgW * scale;
                double scaledH = imgH * scale;
                
                imageView.setFitWidth(scaledW);
                imageView.setFitHeight(scaledH);
                
                imageView.setLayoutX((paneW - scaledW) / 2);
                imageView.setLayoutY((paneH - scaledH) / 2);
                
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(-imageView.getLayoutX(), -imageView.getLayoutY(), paneW, paneH);
                clip.setArcWidth(48);
                clip.setArcHeight(48);
                imageView.setClip(clip);
            }
        } catch (Exception e) {
            System.err.println("Display image error: " + e.getMessage());
        }
    }
    
    @FXML
    public void retakeImage() {
        cameraPane.getChildren().clear();
        capturedImage = null;
        captureImage();
    }
    
    @FXML
    public void goBack() {
        Stage stage = (Stage) cameraPane.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    public void useThis() {
        System.out.println("Image selected for processing");
        goBack();
    }
    
    private void setupButtonHandlers() {
        // Button handlers are connected via @FXML annotations
    }
    
    @Override
    public void captureFromCamera() {
        captureImage();
    }
    
    @Override
    public void captureScreenshot() {
        takeScreenshot();
    }
    
    @Override
    public BufferedImage getCapturedImage() {
        return capturedImage;
    }
}
