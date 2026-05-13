package controller.ScanFromController;

import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import util.CaptureUtils;
import util.SharedContext;

import javafx.scene.input.Dragboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.geometry.Pos;
import javafx.scene.Scene;

import java.net.URL;
import java.util.ResourceBundle;

public class scanFormController implements Initializable, scanService {

    @FXML private JFXComboBox<String> languageBox;
    @FXML private Button captureButton;
    @FXML private Button screenshotButton;
    @FXML private Button addButton;
    @FXML private Button rotateButton;
    @FXML private Button deleteButton;
    @FXML private Button recognizeButton;
    @FXML private Button copyButton;
    @FXML private Button saveButton;
    
    @FXML private HBox thumbnailBox;
    @FXML private AnchorPane leftImagePane;
    
    private ImageView mainImageView;
    private static Integer draggedIndex = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageBox.getItems().setAll("Sinhala", "English", "Tamil");
        languageBox.getSelectionModel().selectFirst();
        setupButtonHandlers();
        
        mainImageView = new ImageView();
        leftImagePane.getChildren().add(mainImageView);
        
        refreshThumbnails();
        
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected == null && !SharedContext.getInstance().getCapturedImages().isEmpty()) {
            selected = SharedContext.getInstance().getCapturedImages().get(0);
        }
        if (selected != null) {
            displayMainImage(selected);
        }
    }
    
    private void refreshThumbnails() {
        Platform.runLater(() -> {
            thumbnailBox.getChildren().clear();
            Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
            
            for (int i = 0; i < SharedContext.getInstance().getCapturedImages().size(); i++) {
                final int index = i;
                Image img = SharedContext.getInstance().getCapturedImages().get(index);
                
                ImageView thumb = new ImageView(img);
                thumb.setFitWidth(80);
                thumb.setFitHeight(80);
                thumb.setPreserveRatio(true);
                
                javafx.scene.layout.StackPane stackPane = new javafx.scene.layout.StackPane(thumb);
                stackPane.setPrefSize(80, 80);
                
                javafx.scene.layout.HBox overlay = new javafx.scene.layout.HBox(5);
                overlay.setAlignment(javafx.geometry.Pos.CENTER);
                overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
                overlay.setVisible(false);
                
                Button btnRetake = new Button("Retake");
                btnRetake.setStyle("-fx-background-color: #0D99FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 5 2 5; -fx-cursor: hand;");
                btnRetake.setOnAction(e -> showRetakeDialog(img, index));
                
                Button btnDelete = new Button("X");
                btnDelete.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 5 2 5; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> {
                    SharedContext.getInstance().getCapturedImages().remove(img);
                    if (selected == img) {
                        Image nextSelected = null;
                        if (!SharedContext.getInstance().getCapturedImages().isEmpty()) {
                            nextSelected = SharedContext.getInstance().getCapturedImages().get(0);
                        }
                        SharedContext.getInstance().setCurrentlySelectedImage(nextSelected);
                        if (nextSelected != null) {
                            displayMainImage(nextSelected);
                        } else {
                            mainImageView.setImage(null);
                        }
                    }
                    refreshThumbnails();
                });
                
                overlay.getChildren().addAll(btnRetake, btnDelete);
                stackPane.getChildren().add(overlay);
                
                AnchorPane thumbContainer = new AnchorPane(stackPane);
                thumbContainer.setPrefSize(90, 90);
                stackPane.setLayoutX(5);
                stackPane.setLayoutY(5);
                
                if (img == selected) {
                    thumbContainer.setStyle("-fx-background-color: #1D2A44; -fx-background-radius: 8; -fx-border-color: #00FF00; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");
                } else {
                    thumbContainer.setStyle("-fx-background-color: #1D2A44; -fx-background-radius: 8; -fx-border-color: #0D99FF; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
                }
                
                thumbContainer.setOnMouseEntered(e -> overlay.setVisible(true));
                thumbContainer.setOnMouseExited(e -> overlay.setVisible(false));
                
                thumbContainer.setOnMouseClicked(e -> {
                    if (e.getTarget() instanceof javafx.scene.Node) {
                        javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
                        if (target == btnRetake || target == btnDelete || 
                            target.getParent() == btnRetake || target.getParent() == btnDelete) {
                            return;
                        }
                    }
                    SharedContext.getInstance().setCurrentlySelectedImage(img);
                    refreshThumbnails();
                    displayMainImage(img);
                });
                
                thumbContainer.setOnDragDetected(e -> {
                    Dragboard db = thumbContainer.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("reorder");
                    db.setContent(content);
                    draggedIndex = index;
                    
                    SnapshotParameters sp = new SnapshotParameters();
                    sp.setFill(Color.TRANSPARENT);
                    db.setDragView(thumbContainer.snapshot(sp, null));
                    e.consume();
                });
                
                thumbContainer.setOnDragOver(e -> {
                    if (e.getGestureSource() != thumbContainer && e.getDragboard().hasString() && "reorder".equals(e.getDragboard().getString())) {
                        e.acceptTransferModes(TransferMode.MOVE);
                    }
                    e.consume();
                });
                
                thumbContainer.setOnDragDropped(e -> {
                    if (draggedIndex != null && draggedIndex != index) {
                        javafx.collections.ObservableList<Image> list = SharedContext.getInstance().getCapturedImages();
                        Image draggedImg = list.remove(draggedIndex.intValue());
                        list.add(index, draggedImg);
                        refreshThumbnails();
                        e.setDropCompleted(true);
                    } else {
                        e.setDropCompleted(false);
                    }
                    e.consume();
                });
                
                thumbContainer.setOnDragDone(e -> {
                    draggedIndex = null;
                });
                
                thumbnailBox.getChildren().add(thumbContainer);
            }
        });
    }
    
    private void displayMainImage(Image img) {
        Platform.runLater(() -> {
            mainImageView.setImage(img);
            
            double paneW = leftImagePane.getWidth();
            double paneH = leftImagePane.getHeight();
            // Fallback sizes if pane hasn't laid out yet
            if (paneW == 0) paneW = 712.0;
            if (paneH == 0) paneH = 609.0;
            
            double imgW = img.getWidth();
            double imgH = img.getHeight();
            
            double scale = Math.min(paneW / imgW, paneH / imgH);
            double scaledW = imgW * scale;
            double scaledH = imgH * scale;
            
            mainImageView.setFitWidth(scaledW);
            mainImageView.setFitHeight(scaledH);
            mainImageView.setPreserveRatio(true);
            
            mainImageView.setLayoutX((paneW - scaledW) / 2);
            mainImageView.setLayoutY((paneH - scaledH) / 2);
            
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(-mainImageView.getLayoutX(), -mainImageView.getLayoutY(), paneW, paneH);
            clip.setArcWidth(32); // 16px radius * 2
            clip.setArcHeight(32);
            mainImageView.setClip(clip);
        });
    }
    
    private void setupButtonHandlers() {
        if (captureButton != null) captureButton.setOnAction(e -> openCamera());
        if (screenshotButton != null) screenshotButton.setOnAction(e -> takeScreenshot());
        if (addButton != null) addButton.setOnAction(e -> browseImage());
        if (rotateButton != null) rotateButton.setOnAction(e -> rotateImage());
        if (deleteButton != null) deleteButton.setOnAction(e -> deleteImage());
        if (recognizeButton != null) recognizeButton.setOnAction(e -> recognizeText());
        if (copyButton != null) copyButton.setOnAction(e -> copyText());
        if (saveButton != null) saveButton.setOnAction(e -> saveImages());
    }
    
    @FXML
    private void openCamera() {
        CaptureUtils.openLiveCamera(this::handleNewImage);
    }
    
    @FXML
    private void takeScreenshot() {
        CaptureUtils.takeScreenshot(this::handleNewImage);
    }
    
    @FXML
    private void browseImage() {
        Stage stage = (Stage) languageBox.getScene().getWindow();
        CaptureUtils.browseImage(stage, this::handleNewImage);
    }
    
    private void handleNewImage(Image img) {
        if (img != null) {
            SharedContext.getInstance().addImage(img);
            refreshThumbnails();
            displayMainImage(img);
        }
    }
    
    private void showRetakeDialog(Image oldImg, int index) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1D2A44; -fx-background-radius: 16; -fx-border-color: #0D99FF; -fx-border-width: 2; -fx-border-radius: 16; -fx-padding: 30;");
        
        Label title = new Label("Choose Source to Retake");
        title.setStyle("-fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Button btnCam = createDialogButton("Live Camera", "#0D99FF");
        btnCam.setOnAction(e -> {
            dialog.close();
            CaptureUtils.openLiveCamera(img -> handleRetakeImage(img, index, oldImg));
        });
        
        Button btnSS = createDialogButton("Screen Shot", "#0D99FF");
        btnSS.setOnAction(e -> {
            dialog.close();
            CaptureUtils.takeScreenshot(img -> handleRetakeImage(img, index, oldImg));
        });
        
        Button btnBrowse = createDialogButton("Browse Image", "#0D99FF");
        btnBrowse.setOnAction(e -> {
            dialog.close();
            CaptureUtils.browseImage((Stage) languageBox.getScene().getWindow(), img -> handleRetakeImage(img, index, oldImg));
        });
        
        Button btnCancel = createDialogButton("Cancel", "#FF4444");
        btnCancel.setOnAction(e -> dialog.close());
        
        root.getChildren().addAll(title, btnCam, btnSS, btnBrowse, btnCancel);
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        
        Stage mainStage = (Stage) languageBox.getScene().getWindow();
        dialog.setX(mainStage.getX() + mainStage.getWidth() / 2 - 150);
        dialog.setY(mainStage.getY() + mainStage.getHeight() / 2 - 175);
        
        dialog.showAndWait();
    }
    
    private Button createDialogButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setPrefHeight(40);
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + color + "; -fx-text-fill: white; -fx-border-radius: 8; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-border-radius: 8; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + color + "; -fx-text-fill: white; -fx-border-radius: 8; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-cursor: hand;"));
        return btn;
    }
    
    private void handleRetakeImage(Image newImg, int index, Image oldImg) {
        if (newImg != null) {
            SharedContext.getInstance().getCapturedImages().set(index, newImg);
            if (SharedContext.getInstance().getCurrentlySelectedImage() == oldImg) {
                SharedContext.getInstance().setCurrentlySelectedImage(newImg);
            }
            refreshThumbnails();
            if (SharedContext.getInstance().getCurrentlySelectedImage() == newImg) {
                displayMainImage(newImg);
            }
        }
    }
    
    @FXML
    private void rotateImage() {
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected != null) {
            double w = selected.getWidth();
            double h = selected.getHeight();
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(h, w);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            
            gc.translate(h / 2.0, w / 2.0);
            gc.rotate(90);
            gc.translate(-w / 2.0, -h / 2.0);
            gc.drawImage(selected, 0, 0);
            
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            Image rotatedImage = canvas.snapshot(params, null);
            
            int index = SharedContext.getInstance().getCapturedImages().indexOf(selected);
            if (index != -1) {
                SharedContext.getInstance().getCapturedImages().set(index, rotatedImage);
                SharedContext.getInstance().setCurrentlySelectedImage(rotatedImage);
                displayMainImage(rotatedImage);
                refreshThumbnails();
            }
        }
    }
    
    @FXML
    private void deleteImage() {
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected != null) {
            SharedContext.getInstance().getCapturedImages().remove(selected);
            Image nextSelected = null;
            if (!SharedContext.getInstance().getCapturedImages().isEmpty()) {
                nextSelected = SharedContext.getInstance().getCapturedImages().get(0);
            }
            SharedContext.getInstance().setCurrentlySelectedImage(nextSelected);
            if (nextSelected != null) {
                displayMainImage(nextSelected);
            } else {
                mainImageView.setImage(null);
            }
            refreshThumbnails();
        }
    }
    
    @FXML
    private void recognizeText() {
        System.out.println("Recognize button clicked - OCR processing");
        String selectedLanguage = languageBox.getSelectionModel().getSelectedItem();
        System.out.println("Processing with language: " + selectedLanguage);
    }
    
    @FXML
    private void copyText() {
        System.out.println("Copy button clicked");
    }
    
    @FXML
    private void saveImages() {
        if (SharedContext.getInstance().getCapturedImages().isEmpty()) {
            System.out.println("No images to save!");
            return;
        }
        
        controller.HistoryController.HistoryService historyService = new controller.HistoryController.historyController();
        String sessionName = "Session_" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        boolean success = historyService.saveSession(new java.util.ArrayList<>(SharedContext.getInstance().getCapturedImages()), sessionName);
        
        if (success) {
            System.out.println("Images saved successfully as " + sessionName);
            // Optionally clear the context or show an alert
            SharedContext.getInstance().getCapturedImages().clear();
            SharedContext.getInstance().setCurrentlySelectedImage(null);
            refreshThumbnails();
            mainImageView.setImage(null);
        } else {
            System.err.println("Failed to save images.");
        }
    }
    
    @Override
    public void processImage(String language) {
        System.out.println("Processing image with language: " + language);
    }
    
    @Override
    public void captureImage() {
        openCamera();
    }
    
    @Override
    public void browseAndAdd() {
        browseImage();
    }
}
