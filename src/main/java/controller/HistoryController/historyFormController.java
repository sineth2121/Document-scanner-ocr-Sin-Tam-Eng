package controller.HistoryController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
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
        card.setStyle("-fx-background-color: #1D2A44; -fx-background-radius: 12; -fx-border-color: #0D99FF; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 10;");
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
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-size: 14px;");

        // 3-dot Menu
        MenuButton menuButton = new MenuButton("⋮");
        menuButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px; -fx-cursor: hand;");
        
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
