import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Starter extends Application {
    public static void main(String[] args) {
        startBackend();
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setMinWidth(1200);
        stage.setMinHeight(780);
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/view/Dashboard.fxml"))));
        stage.show();
    }

    private static Process backendProcess;

    private static void startBackend() {
            if (backendProcess != null && backendProcess.isAlive()) {
                return;
            }

            String pythonCmd = findPythonCommand();
            if (pythonCmd == null) {
                System.err.println("Unable to find Python runtime on PATH. Backend OCR server will not start automatically.");
                return;
            }

            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            File backendScript = projectRoot.resolve("Backend").resolve("api_server.py").toFile();

            if (!backendScript.exists()) {
                System.err.println("Backend OCR script not found at: " + backendScript.getAbsolutePath());
                return;
            }

            ProcessBuilder processBuilder = new ProcessBuilder(pythonCmd, "-u", backendScript.getAbsolutePath());
            processBuilder.directory(projectRoot.toFile());
            processBuilder.redirectError(Redirect.INHERIT);
            processBuilder.redirectOutput(Redirect.INHERIT);

        try {
            backendProcess = processBuilder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (backendProcess != null && backendProcess.isAlive()) {
                    backendProcess.destroy();
                }
            }));
            System.out.println("Started backend OCR server using: " + pythonCmd);
        } catch (IOException e) {
            System.err.println("Failed to launch backend OCR server: " + e.getMessage());
        }
    }

    private static String findPythonCommand() {
            List<String> candidates = List.of("python", "python3");
            for (String candidate : candidates) {
                try {
                    Process process = new ProcessBuilder(candidate, "--version").start();
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        return candidate;
                    }
                } catch (IOException | InterruptedException ignored) {
                    // try next candidate
                }
            }
            return null;
        }
}
