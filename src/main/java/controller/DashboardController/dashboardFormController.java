package controller.DashboardController;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class dashboardFormController implements Initializable {
    @FXML private AnchorPane dropPane;
    @FXML private com.jfoenix.controls.JFXButton btnCapture;
    @FXML private com.jfoenix.controls.JFXButton btnScreenshot;
    @FXML private com.jfoenix.controls.JFXButton btnBrowse;
    @FXML private com.jfoenix.controls.JFXButton btnHistory;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDragAndDrop();
        setupAnimations();
    }

    // ─── Smooth Hover Animations ───────────────────────────────────────────
    private void setupAnimations() {
        addGlowHover(btnCapture, "#6366F1", 22.0, true);
        addGlowHover(btnScreenshot, "#6366F1", 16.0, false);
        addGlowHover(btnBrowse, "#6366F1", 16.0, false);
        addGlowHover(btnHistory, "#A855F7", 14.0, false);
    }

    private void addGlowHover(Node node, String glowColor, double radius, boolean primary) {
        DropShadow idleGlow = new DropShadow(radius, Color.web(glowColor + "88"));
        DropShadow hoverGlow = new DropShadow(radius * 1.8, Color.web(glowColor));

        if (primary) {
            node.setEffect(idleGlow);
        }

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(180), node);
        scaleUp.setToX(1.07);
        scaleUp.setToY(1.07);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(180), node);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        node.setOnMouseEntered(e -> {
            scaleUp.playFromStart();
            node.setEffect(hoverGlow);
        });
        node.setOnMouseExited(e -> {
            scaleDown.playFromStart();
            node.setEffect(primary ? idleGlow : null);
        });
    }

    // ─── Drag & Drop ───────────────────────────────────────────────────────
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

    @FXML
    private void handleHistory() {
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/view/HistoryForm.fxml"));
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Scan History");
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
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
