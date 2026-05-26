package controller.HistoryController;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class historyController implements HistoryService {

    private final String historyDirPath;

    public historyController() {
        historyDirPath = System.getProperty("user.home") + File.separator + "DocScanHistory";
        File dir = new File(historyDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public boolean saveSession(List<Image> images, String sessionName) {
        if (images == null || images.isEmpty()) return false;
        
        File sessionDir = new File(historyDirPath + File.separator + sessionName);
        if (!sessionDir.exists()) {
            sessionDir.mkdirs();
        }
        
        boolean success = true;
        for (int i = 0; i < images.size(); i++) {
            Image img = images.get(i);
            File outputFile = new File(sessionDir, "scan_" + (i + 1) + ".png");
            BufferedImage bImage = SwingFXUtils.fromFXImage(img, null);
            try {
                if (bImage != null) {
                    ImageIO.write(bImage, "png", outputFile);
                } else {
                    System.err.println("Failed to convert JavaFX Image to BufferedImage.");
                    success = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                success = false;
            }
        }
        return success;
    }

    @Override
    public List<File> getAllSessions() {
        File dir = new File(historyDirPath);
        File[] files = dir.listFiles(File::isDirectory);
        List<File> sessions = new ArrayList<>();
        if (files != null) {
            sessions.addAll(Arrays.asList(files));
            // Sort by last modified descending
            sessions.sort(Comparator.comparingLong(File::lastModified).reversed());
        }
        return sessions;
    }

    @Override
    public List<Image> loadSessionImages(File sessionFolder) {
        List<Image> images = new ArrayList<>();
        if (sessionFolder.exists() && sessionFolder.isDirectory()) {
            File[] files = sessionFolder.listFiles((d, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
            if (files != null) {
                // Sort by name so scan_1, scan_2 are in order
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File file : files) {
                    try {
                        BufferedImage bImage = ImageIO.read(file);
                        if (bImage != null) {
                            images.add(SwingFXUtils.toFXImage(bImage, null));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return images;
    }

    @Override
    public boolean exportSession(File sessionFolder, File destinationFolder) {
        if (!sessionFolder.exists() || !sessionFolder.isDirectory() || destinationFolder == null) {
            return false;
        }
        
        File exportDir = new File(destinationFolder, sessionFolder.getName());
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        
        File[] files = sessionFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    Files.copy(file.toPath(), new File(exportDir, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }
}
