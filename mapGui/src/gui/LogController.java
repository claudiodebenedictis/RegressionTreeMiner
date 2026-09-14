package gui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class LogController {
    @FXML private TextArea logArea;

    @FXML private void initialize() {
        logArea.textProperty().bind(Main.app().state().logs);
    }

    @FXML private void clear() { Main.app().state().clearLogs(); }
}
