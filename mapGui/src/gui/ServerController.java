package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class ServerController {
    @FXML private Label serverStatus;
    @FXML private Button startServer;
    @FXML private Button stopServer;
    @FXML private TextArea serverLog;

    @FXML private void initialize() {
        GuiState state = Main.app().state();
        serverStatus.textProperty().bind(state.serverStatus);
        startServer.disableProperty().bind(state.serverStarting.or(state.serverOwned));
        stopServer.disableProperty().bind(state.serverStarting.or(state.serverOwned.not()));
        serverLog.textProperty().bind(state.logs);
    }

    @FXML private void start() { Main.app().state().startServer(); }
    @FXML private void stop() { Main.app().state().stopServer(); }
}
