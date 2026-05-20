package controller.ScanFromController;

import com.jfoenix.controls.JFXComboBox;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.scene.input.Clipboard;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

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
    @FXML private Button exportButton;
    @FXML private TextArea ocrTextArea;

    @FXML private HBox thumbnailBox;
    @FXML private AnchorPane leftImagePane;

    private ImageView mainImageView;
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private static final String OCR_API_URL = "http://127.0.0.1:5000/api/ocr";
    private static Integer draggedIndex = null;

    // Placeholder constant — used to guard copy/export against the initial hint
    private static final String PLACEHOLDER = "Click Recognize to extract OCR text, or type here...";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageBox.getItems().setAll("Sinhala", "English", "Tamil");
        languageBox.getSelectionModel().selectFirst();
        setupButtonHandlers();
        setupAnimations();

        mainImageView = new ImageView();
        leftImagePane.getChildren().add(mainImageView);

        refreshThumbnails();

        // Show the currently selected image (set by dashboard before navigation)
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected == null && !SharedContext.getInstance().getCapturedImages().isEmpty()) {
            selected = SharedContext.getInstance().getCapturedImages().get(0);
            SharedContext.getInstance().setCurrentlySelectedImage(selected);
        }
        if (selected != null) {
            displayMainImage(selected);
            showImageText(selected);
        }
    }

    // ── Animations ──────────────────────────────────────────────────────────

    private void setupAnimations() {
        addGlowHover(captureButton,   "#6366F1", 18.0, true);
        addGlowHover(recognizeButton, "#6366F1", 18.0, true);
        addGlowHover(screenshotButton,"#6366F1", 12.0, false);
        addGlowHover(addButton,       "#6366F1", 12.0, false);
        addGlowHover(rotateButton,    "#A855F7", 10.0, false);
        addGlowHover(deleteButton,    "#FF4757", 10.0, false);
        addGlowHover(copyButton,      "#22D3EE", 10.0, false);
        addGlowHover(saveButton,      "#22D3EE", 10.0, false);
    }

    private void addGlowHover(Node node, String glowColor, double radius, boolean primary) {
        if (node == null) return;
        DropShadow idleGlow  = new DropShadow(radius,       Color.web(glowColor + "88"));
        DropShadow hoverGlow = new DropShadow(radius * 1.8, Color.web(glowColor));
        if (primary) node.setEffect(idleGlow);

        ScaleTransition scaleUp   = new ScaleTransition(Duration.millis(160), node);
        scaleUp.setToX(1.07); scaleUp.setToY(1.07);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(160), node);
        scaleDown.setToX(1.0); scaleDown.setToY(1.0);

        node.setOnMouseEntered(e -> { scaleUp.playFromStart();   node.setEffect(hoverGlow); });
        node.setOnMouseExited (e -> { scaleDown.playFromStart(); node.setEffect(primary ? idleGlow : null); });
    }

    // ── Thumbnails ───────────────────────────────────────────────────────────

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

                // Overlay with Retake / Delete mini-buttons
                javafx.scene.layout.HBox overlay = new javafx.scene.layout.HBox(5);
                overlay.setAlignment(javafx.geometry.Pos.CENTER);
                overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
                overlay.setVisible(false);

                Button btnRetake = new Button("Retake");
                btnRetake.setStyle("-fx-background-color: #0D99FF; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 5 2 5; -fx-cursor: hand;");
                btnRetake.setOnAction(e -> showRetakeDialog(img, index));

                Button btnDelete = new Button("X");
                btnDelete.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 5 2 5; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> {
                    SharedContext.getInstance().removeImageText(img);
                    SharedContext.getInstance().getCapturedImages().remove(img);
                    Image nextSelected = null;
                    if (!SharedContext.getInstance().getCapturedImages().isEmpty()) {
                        nextSelected = SharedContext.getInstance().getCapturedImages().get(0);
                    }
                    SharedContext.getInstance().setCurrentlySelectedImage(nextSelected);
                    if (nextSelected != null) {
                        displayMainImage(nextSelected);
                        showImageText(nextSelected);
                    } else {
                        mainImageView.setImage(null);
                        ocrTextArea.clear();
                    }
                    refreshThumbnails();
                });

                overlay.getChildren().addAll(btnRetake, btnDelete);
                stackPane.getChildren().add(overlay);

                AnchorPane thumbContainer = new AnchorPane(stackPane);
                thumbContainer.setPrefSize(90, 90);
                stackPane.setLayoutX(5);
                stackPane.setLayoutY(5);

                // Highlight the currently selected thumbnail
                if (img == selected) {
                    thumbContainer.setStyle("-fx-background-color: rgba(99,102,241,0.25); -fx-background-radius: 8; -fx-border-color: #6366F1; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");
                } else {
                    thumbContainer.setStyle("-fx-background-color: rgba(15,23,42,0.7); -fx-background-radius: 8; -fx-border-color: rgba(99,102,241,0.3); -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
                }

                thumbContainer.setOnMouseEntered(e -> overlay.setVisible(true));
                thumbContainer.setOnMouseExited (e -> overlay.setVisible(false));

                // Click thumbnail → select image AND show its stored OCR text
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
                    showImageText(img);   // ← load this image's OCR text into the right panel
                });

                // Drag-and-drop reordering
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
                    if (e.getGestureSource() != thumbContainer
                            && e.getDragboard().hasString()
                            && "reorder".equals(e.getDragboard().getString())) {
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

                thumbContainer.setOnDragDone(e -> draggedIndex = null);

                thumbnailBox.getChildren().add(thumbContainer);
            }
        });
    }

    // ── Image display ────────────────────────────────────────────────────────

    private void displayMainImage(Image img) {
        Platform.runLater(() -> {
            mainImageView.setImage(img);

            double paneW = leftImagePane.getWidth();
            double paneH = leftImagePane.getHeight();
            if (paneW == 0) paneW = 693.0;
            if (paneH == 0) paneH = 609.0;

            double imgW = img.getWidth();
            double imgH = img.getHeight();

            double scale   = Math.min(paneW / imgW, paneH / imgH);
            double scaledW = imgW * scale;
            double scaledH = imgH * scale;

            mainImageView.setFitWidth(scaledW);
            mainImageView.setFitHeight(scaledH);
            mainImageView.setPreserveRatio(true);

            double layoutX = (paneW - scaledW) / 2;
            double layoutY = (paneH - scaledH) / 2;
            mainImageView.setLayoutX(layoutX);
            mainImageView.setLayoutY(layoutY);

            // FIX: clip origin must be (0,0) relative to the ImageView, not negative
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(0, 0, scaledW, scaledH);
            clip.setArcWidth(32);
            clip.setArcHeight(32);
            mainImageView.setClip(clip);
        });
    }

    /**
     * Show the stored OCR text for the given image in the right-side panel.
     * If none has been extracted yet, leave the area empty/cleared.
     */
    private void showImageText(Image img) {
        Platform.runLater(() -> {
            String stored = SharedContext.getInstance().getImageText(img);
            if (stored != null && !stored.isBlank()) {
                ocrTextArea.setText(stored);
            } else {
                ocrTextArea.clear();
            }
        });
    }

    // ── Button wiring ────────────────────────────────────────────────────────

    private void setupButtonHandlers() {
        if (captureButton    != null) captureButton.setOnAction(e    -> openCamera());
        if (screenshotButton != null) screenshotButton.setOnAction(e -> takeScreenshot());
        if (addButton        != null) addButton.setOnAction(e        -> browseImage());
        if (rotateButton     != null) rotateButton.setOnAction(e     -> rotateImage());
        if (deleteButton     != null) deleteButton.setOnAction(e     -> deleteImage());
        if (recognizeButton  != null) recognizeButton.setOnAction(e  -> recognizeText());
        if (copyButton       != null) copyButton.setOnAction(e       -> copyText());
        if (saveButton       != null) saveButton.setOnAction(e       -> saveImages());
        if (exportButton     != null) exportButton.setOnAction(e     -> exportText());
    }

    // ── Capture actions ──────────────────────────────────────────────────────

    @FXML private void openCamera()     { CaptureUtils.openLiveCamera(this::handleNewImage); }
    @FXML private void takeScreenshot() { CaptureUtils.takeScreenshot(this::handleNewImage); }

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
            ocrTextArea.clear();   // fresh image → clear right panel
        }
    }

    // ── Retake dialog ────────────────────────────────────────────────────────

    private void showRetakeDialog(Image oldImg, int index) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: rgba(15,23,42,0.95); -fx-background-radius: 24; -fx-border-color: rgba(99,102,241,0.5); -fx-border-width: 1; -fx-border-radius: 24; -fx-padding: 30;");

        Label title = new Label("Choose Source to Retake");
        title.setStyle("-fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-size: 18px; -fx-font-weight: bold;");

        Button btnCam = createDialogButton("Live Camera", "#0D99FF");
        btnCam.setOnAction(e -> { dialog.close(); CaptureUtils.openLiveCamera(img -> handleRetakeImage(img, index, oldImg)); });

        Button btnSS = createDialogButton("Screen Shot", "#0D99FF");
        btnSS.setOnAction(e -> { dialog.close(); CaptureUtils.takeScreenshot(img -> handleRetakeImage(img, index, oldImg)); });

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
        dialog.setX(mainStage.getX() + mainStage.getWidth()  / 2 - 150);
        dialog.setY(mainStage.getY() + mainStage.getHeight() / 2 - 175);

        dialog.showAndWait();
    }

    private Button createDialogButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(200);
        btn.setPrefHeight(40);
        btn.setStyle("-fx-background-color: rgba(30,41,59,0.8); -fx-border-color: " + color + "; -fx-text-fill: white; -fx-border-radius: 8; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-border-radius: 8; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-cursor: hand;"));
        btn.setOnMouseExited (e -> btn.setStyle("-fx-background-color: rgba(30,41,59,0.8); -fx-border-color: " + color + "; -fx-text-fill: white; -fx-border-radius: 8; -fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-cursor: hand;"));
        return btn;
    }

    private void handleRetakeImage(Image newImg, int index, Image oldImg) {
        if (newImg != null) {
            // Transfer any stored OCR text from old → new image key
            SharedContext.getInstance().transferImageText(oldImg, newImg);
            SharedContext.getInstance().getCapturedImages().set(index, newImg);
            if (SharedContext.getInstance().getCurrentlySelectedImage() == oldImg) {
                SharedContext.getInstance().setCurrentlySelectedImage(newImg);
            }
            refreshThumbnails();
            if (SharedContext.getInstance().getCurrentlySelectedImage() == newImg) {
                displayMainImage(newImg);
                showImageText(newImg);
            }
        }
    }

    // ── Rotate ───────────────────────────────────────────────────────────────

    @FXML
    private void rotateImage() {
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected == null) return;

        double w = selected.getWidth();
        double h = selected.getHeight();

        // New canvas dimensions are swapped (w↔h)
        javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(h, w);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        // FIX: correct 90° CW rotation transform
        // 1. Move origin to centre of the NEW canvas
        gc.translate(h / 2.0, w / 2.0);
        // 2. Rotate 90° clockwise
        gc.rotate(90);
        // 3. Translate back by ORIGINAL image half-dimensions (note: -w/2, -h/2)
        gc.translate(-w / 2.0, -h / 2.0);
        // 4. Draw the original image at (0,0) with its original w×h
        gc.drawImage(selected, 0, 0, w, h);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        Image rotatedImage = canvas.snapshot(params, null);

        int index = SharedContext.getInstance().getCapturedImages().indexOf(selected);
        if (index != -1) {
            // Carry OCR text over to the rotated image
            SharedContext.getInstance().transferImageText(selected, rotatedImage);
            SharedContext.getInstance().getCapturedImages().set(index, rotatedImage);
            SharedContext.getInstance().setCurrentlySelectedImage(rotatedImage);
            displayMainImage(rotatedImage);
            showImageText(rotatedImage);
            refreshThumbnails();
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @FXML
    private void deleteImage() {
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected == null) return;

        SharedContext.getInstance().removeImageText(selected);
        SharedContext.getInstance().getCapturedImages().remove(selected);

        Image nextSelected = null;
        if (!SharedContext.getInstance().getCapturedImages().isEmpty()) {
            nextSelected = SharedContext.getInstance().getCapturedImages().get(0);
        }
        SharedContext.getInstance().setCurrentlySelectedImage(nextSelected);
        if (nextSelected != null) {
            displayMainImage(nextSelected);
            showImageText(nextSelected);
        } else {
            mainImageView.setImage(null);
            ocrTextArea.clear();
        }
        refreshThumbnails();
    }

    // ── OCR Recognition ──────────────────────────────────────────────────────

    /**
     * Recognizes text from the currently selected image only.
     * Stores result in SharedContext and updates the right panel.
     * If no image is selected, warns the user.
     */
    @FXML
    private void recognizeText() {
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Image Selected",
                "Please select an image from the thumbnails above before recognizing.");
            return;
        }

        String selectedLanguage = languageBox.getSelectionModel().getSelectedItem();
        if (selectedLanguage == null || selectedLanguage.isBlank()) {
            selectedLanguage = "Sinhala";
        }
        final String lang     = selectedLanguage;
        final String langCode = mapLanguageCode(selectedLanguage);

        ocrTextArea.setText("Recognizing (" + lang + ") … please wait.");
        recognizeButton.setDisable(true);

        new Thread(() -> {
            try {
                String result = performOcrForImage(selected, langCode);
                String text   = (result == null || result.isBlank()) ? "[No text detected]" : result.trim();

                // Store per image
                SharedContext.getInstance().setImageText(selected, text);

                Platform.runLater(() -> {
                    // Only update UI if this image is still selected
                    if (SharedContext.getInstance().getCurrentlySelectedImage() == selected) {
                        ocrTextArea.setText(text);
                    }
                    recognizeButton.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    ocrTextArea.clear();
                    recognizeButton.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "OCR Error",
                        "Unable to perform OCR: " + ex.getMessage()
                        + "\n\nMake sure the Python backend is running and Tesseract is installed.");
                });
            }
        }).start();
    }

    // ── Copy ─────────────────────────────────────────────────────────────────

    @FXML
    private void copyText() {
        String text = ocrTextArea.getText();
        if (text == null || text.isBlank() || text.equals(PLACEHOLDER)) {
            showAlert(Alert.AlertType.WARNING, "No OCR Text", "Run Recognize first to extract text.");
            return;
        }
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        Clipboard.getSystemClipboard().setContent(cc);
        showAlert(Alert.AlertType.INFORMATION, "Copied", "OCR text copied to clipboard.");
    }

    // ── Export ───────────────────────────────────────────────────────────────

    private void exportText() {
        String text = ocrTextArea.getText();
        if (text == null || text.isBlank() || text.equals(PLACEHOLDER)) {
            showAlert(Alert.AlertType.WARNING, "No OCR Text", "Run Recognize first, then export.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>("PDF", List.of("PDF", "Word", "Text"));
        dialog.setTitle("Export OCR Output");
        dialog.setHeaderText("Choose an export format");
        dialog.setContentText("Export as:");
        Optional<String> choice = dialog.showAndWait();
        if (choice.isEmpty()) return;

        String format = choice.get();
        String ext = switch (format) {
            case "Word"  -> "rtf";
            case "Text"  -> "txt";
            default      -> "pdf";
        };

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save OCR Output");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format + " File", "*." + ext));
        chooser.setInitialFileName("ocr_output_"
            + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            + "." + ext);

        File destination = chooser.showSaveDialog(languageBox.getScene().getWindow());
        if (destination == null) return;

        try {
            switch (format) {
                case "PDF"  -> saveAsPdf(destination, text);
                case "Word" -> saveAsWord(destination, text);
                default     -> saveAsText(destination, text);
            }
            showAlert(Alert.AlertType.INFORMATION, "Export Complete",
                "Saved OCR output as: " + destination.getName());
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Export Failed",
                "Could not export file: " + ex.getMessage());
        }
    }

    // ── Save to history ──────────────────────────────────────────────────────

    @FXML
    private void saveImages() {
        if (SharedContext.getInstance().getCapturedImages().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Images", "There are no images to save.");
            return;
        }

        controller.HistoryController.HistoryService historyService = new controller.HistoryController.historyController();
        String sessionName = "Session_"
            + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        boolean success = historyService.saveSession(
            new java.util.ArrayList<>(SharedContext.getInstance().getCapturedImages()), sessionName);

        if (success) {
            SharedContext.getInstance().getCapturedImages().clear();
            SharedContext.getInstance().setCurrentlySelectedImage(null);
            SharedContext.getInstance().clearAllText();
            refreshThumbnails();
            mainImageView.setImage(null);
            ocrTextArea.clear();
            showAlert(Alert.AlertType.INFORMATION, "Saved",
                "Session saved as: " + sessionName);
        } else {
            showAlert(Alert.AlertType.ERROR, "Save Failed",
                "Could not save images. Please try again.");
        }
    }

    // ── HTTP / OCR helpers ───────────────────────────────────────────────────

    private String performOcrForImage(Image image, String language)
            throws IOException, InterruptedException {
        byte[] imageBytes = imageToPngBytes(image);
        String boundary   = "JavaOCRBoundary" + System.currentTimeMillis();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OCR_API_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(ofMimeMultipartData(boundary, imageBytes, language))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("OCR server returned " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private static HttpRequest.BodyPublisher ofMimeMultipartData(
            String boundary, byte[] imageBytes, String language) {
        List<byte[]> parts = new ArrayList<>();
        String ln = "\r\n";
        parts.add(("--" + boundary + ln).getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Disposition: form-data; name=\"language\"" + ln + ln).getBytes(StandardCharsets.UTF_8));
        parts.add(language.getBytes(StandardCharsets.UTF_8));
        parts.add(ln.getBytes(StandardCharsets.UTF_8));
        parts.add(("--" + boundary + ln).getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Disposition: form-data; name=\"image\"; filename=\"ocr.png\"" + ln).getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Type: image/png" + ln + ln).getBytes(StandardCharsets.UTF_8));
        parts.add(imageBytes);
        parts.add(ln.getBytes(StandardCharsets.UTF_8));
        parts.add(("--" + boundary + "--" + ln).getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    private byte[] imageToPngBytes(Image image) throws IOException {
        BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(bi, "png", os);
            return os.toByteArray();
        }
    }

    private static String mapLanguageCode(String selectedLanguage) {
        return switch (selectedLanguage.toLowerCase()) {
            case "sinhala" -> "sin";
            case "tamil"   -> "tam";
            default        -> "eng";
        };
    }

    // ── File-save helpers ────────────────────────────────────────────────────

    private static void saveAsText(File file, String text) throws IOException {
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
    }

    private static void saveAsWord(File file, String text) throws IOException {
        StringBuilder rtf = new StringBuilder();
        rtf.append("{\\rtf1\\ansi\\deff0\n");
        rtf.append("{\\fonttbl{\\f0\\fswiss Arial;}}\n");
        rtf.append("\\f0\\fs24\n");
        for (String line : text.split("\n", -1)) {
            rtf.append(escapeRtf(line)).append("\\par\n");
        }
        rtf.append("}");
        Files.writeString(file.toPath(), rtf, StandardCharsets.UTF_8);
    }

    private static String escapeRtf(String text) {
        return text.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}");
    }

    /**
     * Saves OCR text to a multi-page PDF.
     * Adds a new page whenever text overflows the current page boundary.
     */
    private static void saveAsPdf(File file, String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            final float margin    = 50;
            final float fontSize  = 12;
            final float leading   = 16;

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);

            float pageHeight = page.getMediaBox().getHeight();
            float cursorY    = pageHeight - margin;
            content.newLineAtOffset(margin, cursorY);

            for (String rawLine : text.split("\n", -1)) {
                for (String line : wrapText(rawLine, 90)) {
                    if (cursorY - leading < margin) {
                        // Overflow → close current stream, add new page
                        content.endText();
                        content.close();

                        PDPage newPage = new PDPage(PDRectangle.LETTER);
                        document.addPage(newPage);
                        content = new PDPageContentStream(document, newPage);
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), fontSize);
                        cursorY = pageHeight - margin;
                        content.newLineAtOffset(margin, cursorY);
                    }
                    content.showText(line);
                    content.newLineAtOffset(0, -leading);
                    cursorY -= leading;
                }
            }
            content.endText();
            content.close();
            document.save(file);
        }
    }

    private static List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        // Handle non-ASCII characters that may crash PDType1Font
        text = text.replaceAll("[^\\x20-\\x7E]", "?");
        while (text.length() > maxChars) {
            int breakAt = text.lastIndexOf(' ', maxChars);
            if (breakAt <= 0) breakAt = maxChars;
            lines.add(text.substring(0, breakAt));
            text = text.substring(breakAt).stripLeading();
        }
        lines.add(text);
        return lines;
    }

    // ── Alert helper ────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type, message, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            if (languageBox != null && languageBox.getScene() != null) {
                alert.initOwner(languageBox.getScene().getWindow());
            }
            alert.showAndWait();
        });
    }

    // ── scanService interface ────────────────────────────────────────────────

    @Override public void processImage(String language) { recognizeText(); }
    @Override public void captureImage()               { openCamera(); }
    @Override public void browseAndAdd()               { browseImage(); }

    /**
     * Called by dashboardFormController after loading this scene
     * to ensure the UI refreshes from the current SharedContext state.
     */
    public void showFromContext() {
        Platform.runLater(() -> {
            refreshThumbnails();
            Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
            if (selected != null) {
                displayMainImage(selected);
                showImageText(selected);
            }
        });
    }
}
