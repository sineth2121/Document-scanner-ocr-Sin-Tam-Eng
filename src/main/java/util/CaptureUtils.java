package util;

import com.github.sarxos.webcam.Webcam;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class CaptureUtils {

    public static void browseImage(Stage ownerStage, Consumer<Image> onCaptured) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Image File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(ownerStage);
        if (selectedFile != null) {
            try {
                BufferedImage img = ImageIO.read(selectedFile);
                if (img != null && onCaptured != null) {
                    Image fxImage = SwingFXUtils.toFXImage(img, null);
                    onCaptured.accept(fxImage);
                }
            } catch (Exception e) {
                System.err.println("Browse image error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void takeScreenshot(Consumer<Image> onCaptured) {
        new Thread(() -> {
            try {
                Thread.sleep(200); // Brief delay
                Robot robot = new Robot();
                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage screenFullImage = robot.createScreenCapture(screenRect);
                
                Platform.runLater(() -> {
                    try {
                        WritableImage fxImage = SwingFXUtils.toFXImage(screenFullImage, null);
                        
                        Stage snipStage = new Stage();
                        snipStage.initStyle(StageStyle.TRANSPARENT);
                        snipStage.setX(screenRect.getX());
                        snipStage.setY(screenRect.getY());
                        snipStage.setWidth(screenRect.getWidth());
                        snipStage.setHeight(screenRect.getHeight());
                        snipStage.setAlwaysOnTop(true);
                        
                        AnchorPane root = new AnchorPane();
                        root.setStyle("-fx-background-color: transparent;");
                        
                        ImageView imageView = new ImageView(fxImage);
                        imageView.setFitWidth(screenRect.getWidth());
                        imageView.setFitHeight(screenRect.getHeight());
                        
                        Canvas canvas = new Canvas(screenRect.getWidth(), screenRect.getHeight());
                        GraphicsContext gc = canvas.getGraphicsContext2D();
                        
                        gc.setFill(Color.rgb(0, 0, 0, 0.4));
                        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                        
                        root.getChildren().addAll(imageView, canvas);
                        
                        Scene scene = new Scene(root, screenRect.getWidth(), screenRect.getHeight(), Color.TRANSPARENT);
                        scene.setCursor(javafx.scene.Cursor.CROSSHAIR);
                        
                        final double[] startX = new double[1];
                        final double[] startY = new double[1];
                        
                        scene.setOnMousePressed(e -> {
                            startX[0] = e.getX();
                            startY[0] = e.getY();
                        });
                        
                        scene.setOnMouseDragged(e -> {
                            double x = Math.min(startX[0], e.getX());
                            double y = Math.min(startY[0], e.getY());
                            double width = Math.abs(e.getX() - startX[0]);
                            double height = Math.abs(e.getY() - startY[0]);
                            
                            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                            gc.setFill(Color.rgb(0, 0, 0, 0.4));
                            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                            gc.clearRect(x, y, width, height);
                            
                            gc.setStroke(Color.RED);
                            gc.setLineWidth(2);
                            gc.setLineDashes(5);
                            gc.strokeRect(x, y, width, height);
                        });
                        
                        scene.setOnMouseReleased(e -> {
                            double x = Math.min(startX[0], e.getX());
                            double y = Math.min(startY[0], e.getY());
                            double width = Math.abs(e.getX() - startX[0]);
                            double height = Math.abs(e.getY() - startY[0]);
                            
                            snipStage.close();
                            
                            if (width > 5 && height > 5) {
                                new Thread(() -> {
                                    try {
                                        BufferedImage croppedImage = screenFullImage.getSubimage((int)x, (int)y, (int)width, (int)height);
                                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                                        String fileName = "snippet_" + timestamp + ".png";
                                        String desktopPath = System.getProperty("user.home") + "/Desktop/" + fileName;
                                        
                                        ImageIO.write(croppedImage, "PNG", new File(desktopPath));
                                        System.out.println("Snippet saved: " + desktopPath);
                                        
                                        if (onCaptured != null) {
                                            Image croppedFx = SwingFXUtils.toFXImage(croppedImage, null);
                                            Platform.runLater(() -> onCaptured.accept(croppedFx));
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }).start();
                            }
                        });
                        
                        scene.setOnKeyPressed(e -> {
                            if (e.getCode() == KeyCode.ESCAPE) {
                                snipStage.close();
                            }
                        });
                        
                        snipStage.setScene(scene);
                        snipStage.show();
                        
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("Error taking screenshot: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    public static void openLiveCamera(Consumer<Image> onCaptured) {
        Stage liveStage = new Stage();
        liveStage.setTitle("Live Camera");
        
        AnchorPane root = new AnchorPane();
        ImageView liveView = new ImageView();
        liveView.setFitWidth(640);
        liveView.setFitHeight(480);
        liveView.setPreserveRatio(true);
        
        Button snapBtn = new Button("Take Photo");
        snapBtn.setStyle("-fx-background-color: #0D99FF; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8;");
        snapBtn.setPrefWidth(150);
        snapBtn.setPrefHeight(40);
        snapBtn.setLayoutX(245);
        snapBtn.setLayoutY(490);
        
        root.getChildren().addAll(liveView, snapBtn);
        Scene scene = new Scene(root, 640, 540);
        liveStage.setScene(scene);
        
        try {
            Webcam tempWebcam = Webcam.getDefault();
            if (tempWebcam != null) {
                tempWebcam.setViewSize(new Dimension(640, 480));
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
                    BufferedImage capturedImage = tempWebcam.getImage();
                    if (capturedImage != null && onCaptured != null) {
                        Image fxImage = SwingFXUtils.toFXImage(capturedImage, null);
                        onCaptured.accept(fxImage);
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
}
