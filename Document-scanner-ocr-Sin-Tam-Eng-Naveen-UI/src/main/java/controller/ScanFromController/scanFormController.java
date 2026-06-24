package controller.ScanFromController;

import com.jfoenix.controls.JFXComboBox;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.CaptureUtils;
import util.PhoneCameraClient;
import util.SearchTextUtils;
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
import java.io.ByteArrayInputStream;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.fxmisc.richtext.InlineCssTextArea;

import javafx.scene.shape.SVGPath;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

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
    @FXML private Button ttsButton;
    @FXML private SVGPath ttsIcon;
    @FXML private Button findReplaceButton;
    @FXML private AnchorPane ocrTextContainer;
    private InlineCssTextArea ocrTextArea;

    // Find & Replace panel components
    @FXML private VBox findReplacePanel;
    @FXML private TextField searchField;
    @FXML private TextField replaceField;
    @FXML private Button searchButton;
    @FXML private Button prevMatchButton;
    @FXML private Button nextMatchButton;
    @FXML private Button closeFindReplaceButton;
    @FXML private Button replaceOneButton;
    @FXML private Button replaceAllButton;
    @FXML private Label matchCountLabel;
    @FXML private Label searchInfoLabel;
    @FXML private FlowPane suggestionsPane;

    @FXML private HBox thumbnailBox;
    @FXML private AnchorPane leftImagePane;
    @FXML private SVGPath phoneStatusIcon;
    @FXML private VBox connectionToast;
    @FXML private Button receiveButton;

    private ImageView mainImageView;
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private static final String OCR_API_URL = "http://127.0.0.1:5000/api/ocr";
    private static final String TTS_API_URL = "http://127.0.0.1:5000/api/tts";
    private static Integer draggedIndex = null;

    // ── TTS state ─────────────────────────────────────────────────────────────
    private static final String SPEAKER_SVG = "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z";
    private static final String PAUSE_SVG = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";
    private static final String PLAY_SVG = "M8 5v14l11-7z";
    private static final int TTS_CHUNK_MAX_CHARS = 300;
    private MediaPlayer mediaPlayer;
    private File tempAudioFile;
    private final List<byte[]> audioChunkQueue = new ArrayList<>();
    private int currentChunkIndex = 0;
    private int totalChunks = 0;
    private volatile boolean generatingChunks = false;
    private enum TtsState { IDLE, PLAYING, PAUSED }
    private TtsState ttsState = TtsState.IDLE;

    // Placeholder constant — used to guard copy/export against the initial hint
    private final PhoneCameraClient phoneClient = new PhoneCameraClient();
    private static final String PLACEHOLDER = "Click Recognize to extract OCR text, or type here...";

    // Find & Replace state
    private final List<int[]> currentMatches = new ArrayList<>();  // each entry: [startIndex, endIndex]
    private int currentMatchIndex = -1;
    private String lastSearchWord = "";
    private static final double TEXT_AREA_DEFAULT_TOP = 54.0;
    /** Inline CSS applied to all OCR text so it is visible on the dark panel. */
    private static final String BASE_TEXT_STYLE =
            "-fx-fill: #e8eaf6; -fx-font-family: 'Poppins'; -fx-font-size: 14px;";
    private static final String MATCH_TEXT_STYLE =
            BASE_TEXT_STYLE + " -rtfx-background-color: rgba(99,102,241,0.5);";
    private static final String CURRENT_MATCH_TEXT_STYLE =
            BASE_TEXT_STYLE + " -rtfx-background-color: rgba(239,68,68,0.7);";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageBox.getItems().setAll("Sinhala", "English", "Tamil", "Mixed");
        languageBox.getSelectionModel().selectFirst();
        setupButtonHandlers();
        setupAnimations();
        startPhoneDiscovery();

        mainImageView = new ImageView();
        leftImagePane.getChildren().add(mainImageView);

        ocrTextArea = new InlineCssTextArea();
        ocrTextArea.getStyleClass().add("ocr-rich-text");
        ocrTextArea.setWrapText(true);
        ocrTextArea.setEditable(true);
        ocrTextArea.setStyle("-fx-background-color: rgba(10,15,28,0.95); -fx-background-insets: 0; -fx-padding: 8;");
        AnchorPane.setTopAnchor(ocrTextArea, 0.0);
        AnchorPane.setBottomAnchor(ocrTextArea, 0.0);
        AnchorPane.setLeftAnchor(ocrTextArea, 0.0);
        AnchorPane.setRightAnchor(ocrTextArea, 0.0);
        ocrTextContainer.getChildren().add(ocrTextArea);

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
        addGlowHover(captureButton,    "#6366F1", 18.0, true);
        addGlowHover(recognizeButton,  "#6366F1", 18.0, true);
        addGlowHover(screenshotButton, "#6366F1", 12.0, false);
        addGlowHover(addButton,        "#6366F1", 12.0, false);
        addGlowHover(rotateButton,     "#A855F7", 10.0, false);
        addGlowHover(deleteButton,     "#FF4757", 10.0, false);
        addGlowHover(copyButton,       "#22D3EE", 10.0, false);
        addGlowHover(saveButton,       "#22D3EE", 10.0, false);
        addGlowHover(findReplaceButton,"#A855F7", 10.0, false);
        addGlowHover(ttsButton,        "#22D3EE", 10.0, false);
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
                        clearOcrDisplayText();
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
                setOcrDisplayText(stored);
            } else {
                clearOcrDisplayText();
            }
        });
    }

    /** Replace OCR panel content and apply visible base styling (required for InlineCssTextArea). */
    private void setOcrDisplayText(String text) {
        if (text == null) {
            text = "";
        }
        text = SearchTextUtils.normalizeForSearch(text);
        ocrTextArea.replaceText(text);
        applyHighlights();
        if (!text.isEmpty()) {
            ocrTextArea.moveTo(0);
            ocrTextArea.requestFollowCaret();
        }
    }

    private void clearOcrDisplayText() {
        ocrTextArea.clear();
        currentMatches.clear();
        currentMatchIndex = -1;
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
        if (ttsButton        != null) ttsButton.setOnAction(e        -> speakText());
        if (receiveButton    != null) receiveButton.setOnAction(e    -> receiveFromPhone());

        // Find & Replace handlers
        if (findReplaceButton      != null) findReplaceButton.setOnAction(e      -> toggleFindReplacePanel());
        if (closeFindReplaceButton != null) closeFindReplaceButton.setOnAction(e -> toggleFindReplacePanel());
        if (searchButton           != null) searchButton.setOnAction(e           -> performSearch());
        if (prevMatchButton        != null) prevMatchButton.setOnAction(e        -> navigateMatch(-1));
        if (nextMatchButton        != null) nextMatchButton.setOnAction(e        -> navigateMatch(1));
        if (replaceOneButton       != null) replaceOneButton.setOnAction(e       -> replaceCurrentMatch());
        if (replaceAllButton       != null) replaceAllButton.setOnAction(e       -> replaceAllMatches());
        // Allow pressing Enter in the search field to trigger search
        if (searchField            != null) searchField.setOnAction(e            -> performSearch());
        // Allow pressing Enter in the replace field to replace current
        if (replaceField           != null) replaceField.setOnAction(e           -> replaceCurrentMatch());
    }

    // ── Phone camera ─────────────────────────────────────────────────────────

    private void startPhoneDiscovery() {
        phoneClient.startDiscovery(connected -> {
            phoneStatusIcon.setVisible(connected);
            receiveButton.setVisible(connected);
            captureButton.setDisable(!connected);
            if (connected) {
                System.out.println("Phone connected. Ready to capture.");
                showConnectionToast();
            } else {
                System.out.println("Phone not found. Waiting for discovery...");
            }
        });
    }

    private void showConnectionToast() {
        connectionToast.setVisible(true);
        connectionToast.setManaged(true);
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Platform.runLater(() -> {
                connectionToast.setVisible(false);
                connectionToast.setManaged(false);
            });
        }).start();
    }

    // ── Capture actions ──────────────────────────────────────────────────────

    @FXML private void openCamera() {
        captureFromPhone(this::handleNewImage);
    }

    private void captureFromPhone(Consumer<Image> callback) {
        if (!phoneClient.isConnected()) {
            showAlert(Alert.AlertType.WARNING, "Phone Not Connected",
                "Make sure your phone is on the same WiFi network and the camera app is running.");
            return;
        }
        new Thread(() -> {
            try {
                byte[] imageBytes = phoneClient.captureSingleImage();
                BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (buffered != null) {
                    Image fxImage = SwingFXUtils.toFXImage(buffered, null);
                    Platform.runLater(() -> callback.accept(fxImage));
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Capture Failed",
                        "Phone returned unreadable image data (" + imageBytes.length + " bytes)"));
                }
            } catch (IOException e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Capture Failed",
                    "Could not capture from phone: " + e.getMessage()));
            }
        }).start();
    }

    @FXML private void receiveFromPhone() {
        if (!phoneClient.isConnected()) {
            showAlert(Alert.AlertType.WARNING, "Phone Not Connected",
                "Make sure your phone is on the same WiFi network and the camera app is running.");
            return;
        }
        new Thread(() -> {
            try {
                byte[] imageBytes = phoneClient.receiveImage();
                BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (buffered != null) {
                    Image fxImage = SwingFXUtils.toFXImage(buffered, null);
                    Platform.runLater(() -> handleNewImage(fxImage));
                } else {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Receive Failed",
                        "Phone returned unreadable image data (" + imageBytes.length + " bytes)"));
                }
            } catch (IOException e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Receive Failed",
                    "Could not receive image from phone: " + e.getMessage()));
            }
        }).start();
    }

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
            clearOcrDisplayText();   // fresh image → clear right panel
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

        Button btnCam = createDialogButton("Phone Camera", "#22c55e");
        btnCam.setOnAction(e -> { dialog.close(); captureFromPhone(img -> handleRetakeImage(img, index, oldImg)); });

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
            clearOcrDisplayText();
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

        setOcrDisplayText("Recognizing (" + lang + ") … please wait.");
        recognizeButton.setDisable(true);

        final int selectedIndex = SharedContext.getInstance().getCapturedImages().indexOf(selected);

        new Thread(() -> {
            try {
                String result = performOcrForImage(selected, langCode);
                String text   = (result == null || result.isBlank()) ? "[No text detected]" : result.trim();

                // Store per image
                SharedContext.getInstance().setImageText(selected, text);

                Platform.runLater(() -> {
                    Image current = SharedContext.getInstance().getCurrentlySelectedImage();
                    int currentIndex = SharedContext.getInstance().getCapturedImages().indexOf(current);
                    if (currentIndex == selectedIndex && selectedIndex >= 0) {
                        setOcrDisplayText(text);
                    }
                    recognizeButton.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    clearOcrDisplayText();
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

    // ── Find & Replace ──────────────────────────────────────────────────────

    /**
     * Toggle the Find & Replace panel visibility.
     * When shown, shifts the TextArea down to make room; when hidden, restores it.
     */
    private void toggleFindReplacePanel() {
        if (findReplacePanel == null) return;

        boolean show = !findReplacePanel.isVisible();
        findReplacePanel.setVisible(show);
        findReplacePanel.setManaged(show);

        Platform.runLater(() -> {
            if (show) {
                findReplacePanel.applyCss();
                findReplacePanel.layout();
                double panelHeight = findReplacePanel.getBoundsInLocal().getHeight();
                double newTop = TEXT_AREA_DEFAULT_TOP + panelHeight + 8;
                AnchorPane.setTopAnchor(ocrTextContainer, newTop);
                searchField.requestFocus();
            } else {
                AnchorPane.setTopAnchor(ocrTextContainer, TEXT_AREA_DEFAULT_TOP);
                clearSearchState();
            }
        });
    }

    /**
     * Perform a whole-word, case-insensitive search in the OCR text.
     * Uses Unicode word boundaries so Sinhala, Tamil, and English behave the same way.
     * If no matches found, show fuzzy close-match suggestions.
     */
    private void performSearch() {
        String word = searchField.getText();
        if (word == null || word.isBlank()) return;

        word = SearchTextUtils.normalizeForSearch(word.trim());
        lastSearchWord = word;
        String text = ocrTextArea.getText();
        if (text == null || text.isBlank()) {
            searchInfoLabel.setText("No text to search.");
            clearSearchState();
            return;
        }

        int totalWords = SearchTextUtils.countWordTokens(text);

        currentMatches.clear();
        try {
            currentMatches.addAll(SearchTextUtils.findWholeWordMatches(text, word));
        } catch (Exception e) {
            searchInfoLabel.setText("Invalid search term.");
            return;
        }

        // Hide suggestions pane initially
        suggestionsPane.setVisible(false);
        suggestionsPane.setManaged(false);
        suggestionsPane.getChildren().clear();

        if (currentMatches.isEmpty()) {
            currentMatchIndex = -1;
            matchCountLabel.setText("0 / 0");
            searchInfoLabel.setText("'" + word + "' not found.  Total words: " + totalWords);

            // Show fuzzy close-match suggestions
            List<String> suggestions = findCloseMatches(word, text, 5, 0.6);
            if (!suggestions.isEmpty()) {
                searchInfoLabel.setText("'" + word + "' not found.  Total words: " + totalWords
                        + "\nDid you mean one of these?");
                for (String suggestion : suggestions) {
                    Button chip = new Button(suggestion);
                    chip.setStyle("-fx-background-color: rgba(99,102,241,0.3); "
                            + "-fx-text-fill: #A5B4FC; -fx-background-radius: 12; "
                            + "-fx-border-color: rgba(99,102,241,0.5); -fx-border-radius: 12; "
                            + "-fx-cursor: hand; -fx-font-family: 'Poppins'; -fx-font-size: 12px; "
                            + "-fx-padding: 4 12 4 12;");
                    chip.setOnMouseEntered(e -> chip.setStyle("-fx-background-color: #6366F1; "
                            + "-fx-text-fill: white; -fx-background-radius: 12; "
                            + "-fx-border-color: #6366F1; -fx-border-radius: 12; "
                            + "-fx-cursor: hand; -fx-font-family: 'Poppins'; -fx-font-size: 12px; "
                            + "-fx-padding: 4 12 4 12;"));
                    chip.setOnMouseExited(e -> chip.setStyle("-fx-background-color: rgba(99,102,241,0.3); "
                            + "-fx-text-fill: #A5B4FC; -fx-background-radius: 12; "
                            + "-fx-border-color: rgba(99,102,241,0.5); -fx-border-radius: 12; "
                            + "-fx-cursor: hand; -fx-font-family: 'Poppins'; -fx-font-size: 12px; "
                            + "-fx-padding: 4 12 4 12;"));
                    chip.setOnAction(e -> {
                        searchField.setText(suggestion);
                        performSearch();
                    });
                    suggestionsPane.getChildren().add(chip);
                }
                suggestionsPane.setVisible(true);
                suggestionsPane.setManaged(true);

                // Re-measure panel height after suggestions added
                Platform.runLater(() -> {
                    findReplacePanel.applyCss();
                    findReplacePanel.layout();
                    double panelHeight = findReplacePanel.getBoundsInLocal().getHeight();
                    double newTop = TEXT_AREA_DEFAULT_TOP + panelHeight + 8;
                    AnchorPane.setTopAnchor(ocrTextContainer, newTop);
                });
            }
        } else {
            currentMatchIndex = 0;
            updateMatchCountLabel();

            // Build per-occurrence detail lines
            StringBuilder info = new StringBuilder();
            info.append("Total words: ").append(totalWords)
                    .append("  |  Occurrences of '").append(word).append("': ")
                    .append(currentMatches.size());
            searchInfoLabel.setText(info.toString());

            highlightCurrentMatch();
        }
    }

    /**
     * Navigate between matches: direction = -1 (prev) or +1 (next).
     */
    private void navigateMatch(int direction) {
        if (currentMatches.isEmpty()) return;
        currentMatchIndex += direction;
        if (currentMatchIndex < 0) currentMatchIndex = currentMatches.size() - 1;
        if (currentMatchIndex >= currentMatches.size()) currentMatchIndex = 0;
        updateMatchCountLabel();
        highlightCurrentMatch();
    }

    /**
     * Replace only the currently highlighted occurrence.
     */
    private void replaceCurrentMatch() {
        if (currentMatches.isEmpty() || currentMatchIndex < 0) {
            showAlert(Alert.AlertType.WARNING, "No Match Selected",
                    "Search for a word first, then navigate to the occurrence you want to replace.");
            return;
        }
        String replacement = replaceField.getText();
        if (replacement == null) replacement = "";

        int[] match = currentMatches.get(currentMatchIndex);
        String text = ocrTextArea.getText();

        if (!SearchTextUtils.regionMatchesSearchTerm(text, match[0], match[1], lastSearchWord)) {
            showAlert(Alert.AlertType.WARNING, "Text Modified", "The text was manually modified. Please search again.");
            performSearch();
            return;
        }

        String normalizedReplacement = SearchTextUtils.normalizeForSearch(replacement);
        String updated = text.substring(0, match[0]) + normalizedReplacement + text.substring(match[1]);
        setOcrDisplayText(updated);

        // Update stored text for the current image
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected != null) {
            SharedContext.getInstance().setImageText(selected, updated);
        }

        int targetIndex = currentMatchIndex;

        // Re-run search to refresh match positions
        performSearch();
        
        // Restore match index if possible
        if (!currentMatches.isEmpty()) {
            currentMatchIndex = Math.min(targetIndex, currentMatches.size() - 1);
            updateMatchCountLabel();
            highlightCurrentMatch();
        }
    }

    /**
     * Replace ALL occurrences of the current search word.
     */
    private void replaceAllMatches() {
        if (currentMatches.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Matches",
                    "Search for a word first before replacing.");
            return;
        }
        String replacement = replaceField.getText();
        if (replacement == null) replacement = "";

        String text = ocrTextArea.getText();
        int count = currentMatches.size();

        String normalizedReplacement = SearchTextUtils.normalizeForSearch(replacement);

        for (int[] match : currentMatches) {
            if (!SearchTextUtils.regionMatchesSearchTerm(text, match[0], match[1], lastSearchWord)) {
                showAlert(Alert.AlertType.WARNING, "Text Modified", "The text was manually modified. Please search again.");
                performSearch();
                return;
            }
        }

        for (int i = currentMatches.size() - 1; i >= 0; i--) {
            int[] match = currentMatches.get(i);
            text = text.substring(0, match[0]) + normalizedReplacement + text.substring(match[1]);
        }

        setOcrDisplayText(text);

        // Update stored text for the current image
        Image selected = SharedContext.getInstance().getCurrentlySelectedImage();
        if (selected != null) {
            SharedContext.getInstance().setImageText(selected, text);
        }

        searchInfoLabel.setText("Replaced all " + count + " occurrences with '" + replacement + "'.");
        clearSearchState();
    }

    private void updateMatchCountLabel() {
        if (currentMatches.isEmpty()) {
            matchCountLabel.setText("0 / 0");
        } else {
            matchCountLabel.setText((currentMatchIndex + 1) + " / " + currentMatches.size());
        }
    }

    private void clearSearchState() {
        currentMatches.clear();
        currentMatchIndex = -1;
        matchCountLabel.setText("0 / 0");
    }

    /**
     * Select and scroll to the current match in the TextArea.
     */
    private void highlightCurrentMatch() {
        if (currentMatches.isEmpty() || currentMatchIndex < 0) return;
        int[] match = currentMatches.get(currentMatchIndex);
        Platform.runLater(() -> {
            ocrTextArea.requestFocus();
            ocrTextArea.moveTo(match[0]);
            ocrTextArea.requestFollowCaret();
            applyHighlights();
        });
    }

    private void applyHighlights() {
        int len = ocrTextArea.getLength();
        if (len == 0) return;
        ocrTextArea.setStyle(0, len, BASE_TEXT_STYLE);
        for (int i = 0; i < currentMatches.size(); i++) {
            int[] match = currentMatches.get(i);
            if (match[0] < 0 || match[1] > len || match[0] >= match[1]) continue;
            if (i == currentMatchIndex) {
                ocrTextArea.setStyle(match[0], match[1], CURRENT_MATCH_TEXT_STYLE);
            } else {
                ocrTextArea.setStyle(match[0], match[1], MATCH_TEXT_STYLE);
            }
        }
    }

    /**
     * Find close matches for a word in the document text using Levenshtein distance.
     * Replicates Python's difflib.get_close_matches() behavior.
     *
     * @param word    the search term
     * @param text    the full document text
     * @param n       max number of suggestions to return
     * @param cutoff  minimum similarity ratio (0.0 to 1.0)
     * @return list of close-match words sorted by similarity (best first)
     */
    private List<String> findCloseMatches(String word, String text, int n, double cutoff) {
        word = SearchTextUtils.normalizeForSearch(word);
        text = SearchTextUtils.normalizeForSearch(text);
        Set<String> uniqueWords = new HashSet<>(SearchTextUtils.uniqueWordTokens(text));

        List<String[]> scored = new ArrayList<>();

        for (String candidate : uniqueWords) {
            if (candidate.equalsIgnoreCase(word)) {
                continue;
            }

            int dist = levenshteinDistance(word, candidate);
            int maxLen = Math.max(word.length(), candidate.length());
            double similarity = maxLen == 0 ? 1.0 : 1.0 - ((double) dist / maxLen);

            if (similarity >= cutoff) {
                scored.add(new String[]{candidate, String.valueOf(similarity)});
            }
        }

        // Sort by similarity descending
        scored.sort((a, b) -> Double.compare(Double.parseDouble(b[1]), Double.parseDouble(a[1])));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(n, scored.size()); i++) {
            result.add(scored.get(i)[0]);
        }
        return result;
    }

    /**
     * Compute the Levenshtein (edit) distance between two strings.
     */
    private static int levenshteinDistance(String a, String b) {
        int lenA = a.length(), lenB = b.length();
        int[][] dp = new int[lenA + 1][lenB + 1];
        for (int i = 0; i <= lenA; i++) dp[i][0] = i;
        for (int j = 0; j <= lenB; j++) dp[0][j] = j;
        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[lenA][lenB];
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
            clearOcrDisplayText();
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
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        if (w <= 0 || h <= 0) {
            throw new IOException("Image is still loading; try again in a moment.");
        }
        BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
        if (bi == null) {
            throw new IOException("Could not read image pixels for OCR.");
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(bi, "png", os);
            return os.toByteArray();
        }
    }

    private static String mapLanguageCode(String selectedLanguage) {
        return switch (selectedLanguage.toLowerCase()) {
            case "sinhala" -> "sin";
            case "tamil"   -> "tam";
            case "mixed"   -> "eng+sin+tam";
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

    // ── Text-to-Speech ───────────────────────────────────────────────────────

    @FXML
    private void speakText() {
        switch (ttsState) {
            case IDLE -> generateAndPlaySpeech();
            case PLAYING -> pauseSpeech();
            case PAUSED -> resumeSpeech();
        }
    }

    private void generateAndPlaySpeech() {
        String text = ocrTextArea.getText();
        if (text == null || text.isBlank() || text.equals(PLACEHOLDER)) {
            showAlert(Alert.AlertType.WARNING, "No OCR Text", "Run Recognize first or type some text to speak.");
            return;
        }

        String selectedLanguage = languageBox.getSelectionModel().getSelectedItem();
        if (selectedLanguage == null || selectedLanguage.isBlank()) {
            selectedLanguage = "english";
        }

        final String finalLanguage = selectedLanguage;
        ttsButton.setDisable(true);
        ttsButton.setText("Generating...");

        List<String> chunks = splitTextIntoChunks(text.trim(), TTS_CHUNK_MAX_CHARS);
        totalChunks = chunks.size();
        currentChunkIndex = 0;
        audioChunkQueue.clear();
        generatingChunks = true;

        new Thread(() -> {
            for (int i = 0; i < chunks.size(); i++) {
                try {
                    byte[] audioBytes = fetchTtsAudio(chunks.get(i), finalLanguage);
                    synchronized (audioChunkQueue) {
                        audioChunkQueue.add(audioBytes);
                    }
                    final int idx = i;
                    Platform.runLater(() -> {
                        if (idx == 0) {
                            playChunk(0);
                        } else {
                            onChunkGenerated();
                        }
                    });
                } catch (Exception e) {
                    synchronized (audioChunkQueue) {
                        audioChunkQueue.add(null);
                    }
                    if (i == 0) {
                        Platform.runLater(() -> {
                            resetTtsState();
                            showAlert(Alert.AlertType.ERROR, "TTS Error",
                                "Failed to generate speech: " + e.getMessage());
                        });
                        return;
                    }
                }
            }
            generatingChunks = false;
            Platform.runLater(() -> {
                if (mediaPlayer == null && currentChunkIndex >= audioChunkQueue.size()) {
                    resetTtsState();
                }
            });
        }).start();
    }

    private byte[] fetchTtsAudio(String text, String language) throws Exception {
        String jsonPayload = "{\"text\":" + escapeJson(text) + ",\"language\":\"" + language + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TTS_API_URL))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            String errorMsg = new String(response.body(), StandardCharsets.UTF_8);
            throw new IOException("Backend returned " + response.statusCode() + ": " + errorMsg);
        }
        return response.body();
    }

    private void playChunk(int index) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            cleanupTempFile();

            byte[] audioBytes;
            synchronized (audioChunkQueue) {
                if (index >= audioChunkQueue.size()) return;
                audioBytes = audioChunkQueue.get(index);
            }
            if (audioBytes == null) {
                onChunkFinished();
                return;
            }

            File tempFile = File.createTempFile("tts_", ".mp3");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), audioBytes);
            tempAudioFile = tempFile;

            Media media = new Media(tempFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(this::onChunkFinished));
            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Playback Error",
                    "Audio playback failed: " + mediaPlayer.getError().getMessage());
                onChunkFinished();
            }));
            mediaPlayer.play();
            ttsState = TtsState.PLAYING;
            updateTtsButton();
        } catch (Exception e) {
            resetTtsState();
            showAlert(Alert.AlertType.ERROR, "Playback Error", e.getMessage());
        }
    }

    private void onChunkGenerated() {
        if (mediaPlayer == null && ttsState == TtsState.PLAYING) {
            tryPlayNext();
        }
    }

    private void onChunkFinished() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        cleanupTempFile();
        currentChunkIndex++;
        tryPlayNext();
    }

    private void tryPlayNext() {
        synchronized (audioChunkQueue) {
            if (currentChunkIndex < audioChunkQueue.size()) {
                playChunk(currentChunkIndex);
            } else if (!generatingChunks && currentChunkIndex >= totalChunks) {
                resetTtsState();
            }
        }
    }

    private void cleanupTempFile() {
        if (tempAudioFile != null) {
            tempAudioFile.delete();
            tempAudioFile = null;
        }
    }

    private void pauseSpeech() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            ttsState = TtsState.PAUSED;
            updateTtsButton();
        }
    }

    private void resumeSpeech() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
            ttsState = TtsState.PLAYING;
            updateTtsButton();
        }
    }

    private void updateTtsButton() {
        switch (ttsState) {
            case IDLE -> {
                ttsButton.setText("Speak");
                ttsButton.setDisable(false);
                if (ttsIcon != null) ttsIcon.setContent(SPEAKER_SVG);
            }
            case PLAYING -> {
                ttsButton.setText("Pause");
                ttsButton.setDisable(false);
                if (ttsIcon != null) ttsIcon.setContent(PAUSE_SVG);
            }
            case PAUSED -> {
                ttsButton.setText("Play");
                ttsButton.setDisable(false);
                if (ttsIcon != null) ttsIcon.setContent(PLAY_SVG);
            }
        }
    }

    private void resetTtsState() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        cleanupTempFile();
        audioChunkQueue.clear();
        currentChunkIndex = 0;
        totalChunks = 0;
        generatingChunks = false;
        ttsState = TtsState.IDLE;
        updateTtsButton();
    }

    private static List<String> splitTextIntoChunks(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+|\\n+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.length() > maxChars) {
                if (!current.isEmpty()) {
                    chunks.add(current.toString());
                    current = new StringBuilder();
                }
                int start = 0;
                while (start < trimmed.length()) {
                    int end = Math.min(start + maxChars, trimmed.length());
                    chunks.add(trimmed.substring(start, end));
                    start = end;
                }
                continue;
            }
            if (current.length() + trimmed.length() + 1 > maxChars) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) current.append(" ");
            current.append(trimmed);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        if (chunks.isEmpty()) {
            chunks.add(text);
        }
        return chunks;
    }

    private static String escapeJson(String string) {
        if (string == null || string.isEmpty()) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\').append(c);
                    break;
                case '\b': sb.append("\\b"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\f': sb.append("\\f"); break;
                case '\r': sb.append("\\r"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
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
