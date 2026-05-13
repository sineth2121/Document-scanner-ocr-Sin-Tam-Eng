package controller.HistoryController;

import javafx.animation.ScaleTransition;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.SharedContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class historyFormController implements Initializable {

    @FXML
    private FlowPane historyGrid;

    private HistoryService historyService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        historyService = new historyController();
        loadHistoryItems();
    }

    private void loadHistoryItems() {
        historyGrid.getChildren().clear();
        List<File> sessions = historyService.getAllSessions();

        for (File sessionFolder : sessions) {
            VBox itemBox = createSessionCard(sessionFolder);
            historyGrid.getChildren().add(itemBox);
        }
    }

    private VBox createSessionCard(File sessionFolder) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(15,23,42,0.78); -fx-background-radius: 18; -fx-border-color: rgba(99,102,241,0.45); -fx-border-radius: 18; -fx-border-width: 1; -fx-padding: 10; -fx-cursor: hand;");
        card.setPrefWidth(200);
        card.setPrefHeight(250);

        List<Image> images = historyService.loadSessionImages(sessionFolder);
        Image thumbnail = images.isEmpty() ? null : images.get(0);

        ImageView imageView = new ImageView();
        if (thumbnail != null) {
            imageView.setImage(thumbnail);
        }
        imageView.setFitWidth(160);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(sessionFolder.getName());
        nameLabel.setStyle("-fx-text-fill: rgba(220,220,255,0.9); -fx-font-family: 'Poppins'; -fx-font-size: 13px;");

        // 3-dot Menu
        MenuButton menuButton = new MenuButton("⋮");
        menuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(200,200,255,0.8); -fx-font-size: 18px; -fx-cursor: hand;");

        // Hover glow animation on card
        DropShadow idleGlow = new DropShadow(12, Color.web("#6366F188"));
        DropShadow hoverGlow = new DropShadow(28, Color.web("#6366F1"));
        card.setEffect(idleGlow);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(170), card);
        scaleUp.setToX(1.04); scaleUp.setToY(1.04);
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(170), card);
        scaleDown.setToX(1.0); scaleDown.setToY(1.0);
        card.setOnMouseEntered(e -> {
            scaleUp.playFromStart();
            card.setEffect(hoverGlow);
            card.setStyle("-fx-background-color: rgba(25,33,55,0.92); -fx-background-radius: 18; -fx-border-color: #6366F1; -fx-border-radius: 18; -fx-border-width: 1.5; -fx-padding: 10; -fx-cursor: hand;");
        });
        card.setOnMouseExited(e -> {
            scaleDown.playFromStart();
            card.setEffect(idleGlow);
            card.setStyle("-fx-background-color: rgba(15,23,42,0.78); -fx-background-radius: 18; -fx-border-color: rgba(99,102,241,0.45); -fx-border-radius: 18; -fx-border-width: 1; -fx-padding: 10; -fx-cursor: hand;");
        });
        
        MenuItem editItem = new MenuItem("Edit");
        editItem.setStyle("-fx-font-family: 'Poppins';");
        editItem.setOnAction(e -> handleEdit(sessionFolder, images));
        
        MenuItem exportItem = new MenuItem("Export");
        exportItem.setStyle("-fx-font-family: 'Poppins';");
        exportItem.setOnAction(e -> handleExport(sessionFolder));
        
        menuButton.getItems().addAll(editItem, exportItem);

        HBox bottomBox = new HBox(10);
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.getChildren().addAll(nameLabel, menuButton);
        HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        bottomBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(imageView, bottomBox);
        
        // Make the whole card clickable for Edit
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof MenuButton)) {
                handleEdit(sessionFolder, images);
            }
        });

        return card;
    }

    private void handleEdit(File sessionFolder, List<Image> images) {
        if (images.isEmpty()) {
            System.out.println("No images to edit in session: " + sessionFolder.getName());
            return;
        }

        // Clear current context and add all images from this session
        SharedContext.getInstance().getCapturedImages().clear();
        for (Image img : images) {
            SharedContext.getInstance().addImage(img);
        }

        // Open ScanForm
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ScanForm.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Scan Form - " + sessionFolder.getName());
            stage.setScene(new Scene(root));
            stage.show();
            
            // Close history form
            handleBack();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleExport(File sessionFolder) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Destination Folder");
        Stage stage = (Stage) historyGrid.getScene().getWindow();
        File destination = directoryChooser.showDialog(stage);

        if (destination != null) {
            boolean success = historyService.exportSession(sessionFolder, destination);
            if (success) {
                System.out.println("Exported " + sessionFolder.getName() + " to " + destination.getAbsolutePath());
            } else {
                System.err.println("Export failed for " + sessionFolder.getName());
            }
        }
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) historyGrid.getScene().getWindow();
        stage.close();
    }
}
