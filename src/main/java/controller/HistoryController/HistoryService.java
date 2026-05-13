package controller.HistoryController;

import javafx.scene.image.Image;
import java.io.File;
import java.util.List;

public interface HistoryService {
    /**
     * Saves a list of images as a new session in the history directory.
     * @param images The images to save.
     * @param sessionName The name of the session (usually a timestamp).
     * @return true if successful, false otherwise.
     */
    boolean saveSession(List<Image> images, String sessionName);

    /**
     * Retrieves all session folders from the history directory.
     * @return List of session folders.
     */
    List<File> getAllSessions();

    /**
     * Loads all images from a specific session folder.
     * @param sessionFolder The folder containing the images.
     * @return List of images loaded from the folder.
     */
    List<Image> loadSessionImages(File sessionFolder);

    /**
     * Exports a session folder to a specific destination.
     * @param sessionFolder The source session folder.
     * @param destinationFolder The destination directory to export to.
     * @return true if successful, false otherwise.
     */
    boolean exportSession(File sessionFolder, File destinationFolder);
}
