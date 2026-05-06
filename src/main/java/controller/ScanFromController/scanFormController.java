package controller.ScanFromController;

import com.jfoenix.controls.JFXComboBox;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class scanFormController implements Initializable {

    @FXML
    private JFXComboBox<String> languageBox;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        languageBox.getItems().setAll("Sinhala", "English", "Tamil");
    }
}
