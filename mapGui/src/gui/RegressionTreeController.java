package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class RegressionTreeController {
    @FXML private TextField host;
    @FXML private TextField port;
    @FXML private Button connect;
    @FXML private Button disconnect;
    @FXML private Label connectionStatus;
    @FXML private RadioButton learnMode;
    @FXML private RadioButton loadMode;
    @FXML private VBox learnPane;
    @FXML private VBox loadPane;
    @FXML private TextField table;
    @FXML private TextField archive;
    @FXML private Button learn;
    @FXML private Button load;
    @FXML private Button predict;
    @FXML private Button answer;
    @FXML private ComboBox<String> options;
    @FXML private TextField branch;
    @FXML private TextArea question;
    @FXML private Label prediction;
    @FXML private Label message;

    @FXML private void initialize() {
        GuiState state = Main.app().state();
        connectionStatus.textProperty().bind(state.connectionStatus);
        message.textProperty().bind(state.treeMessage);
        prediction.textProperty().bind(state.prediction);
        question.textProperty().bind(state.query);
        connect.disableProperty().bind(state.connected.or(state.working));
        disconnect.disableProperty().bind(state.connected.or(state.working).not());
        host.disableProperty().bind(state.connected.or(state.working));
        port.disableProperty().bind(state.connected.or(state.working));
        var unavailable = state.connected.not().or(state.working).or(state.waitingAnswer);
        learn.disableProperty().bind(unavailable);
        load.disableProperty().bind(unavailable);
        predict.disableProperty().bind(unavailable.or(state.treeReady.not()));
        answer.disableProperty().bind(state.waitingAnswer.not().or(state.working));
        branch.disableProperty().bind(state.waitingAnswer.not().or(state.working));
        options.disableProperty().bind(state.waitingAnswer.not().or(state.working));
        learnPane.visibleProperty().bind(learnMode.selectedProperty());
        learnPane.managedProperty().bind(learnPane.visibleProperty());
        loadPane.visibleProperty().bind(loadMode.selectedProperty());
        loadPane.managedProperty().bind(loadPane.visibleProperty());
        state.query.addListener((observable, previous, current) -> {
            options.getItems().clear();
            branch.clear();
            for (String line : current.split("\\R")) {
                if (line.matches("\\d+:.*")) {
                    options.getItems().add(line);
                }
            }
            if (!options.getItems().isEmpty()) {
                options.getSelectionModel().selectFirst();
            }
        });
        options.valueProperty().addListener((observable, previous, current) -> {
            if (current != null) {
                branch.setText(current.substring(0, current.indexOf(':')));
            }
        });
    }

    @FXML private void connectServer() {
        try {
            int value = Integer.parseInt(port.getText().trim());
            if (value < 1 || value > 65535 || host.getText().isBlank()) {
                throw new NumberFormatException();
            }
            Main.app().state().connect(host.getText().trim(), value);
        } catch (NumberFormatException exception) {
            Main.app().alert("Connessione", "Inserire un host e una porta valida tra 1 e 65535.");
        }
    }

    @FXML private void disconnectServer() { Main.app().state().disconnect(); }

    @FXML private void learnTree() {
        if (table.getText().isBlank()) {
            Main.app().alert("Apprendimento", "Inserire il nome della tabella.");
            return;
        }
        Main.app().state().learn(table.getText().trim());
    }

    @FXML private void loadTree() {
        if (archive.getText().isBlank()) {
            Main.app().alert("Caricamento", "Inserire il percorso dell'archivio sul server.");
            return;
        }
        Main.app().state().load(archive.getText().trim());
    }

    @FXML private void chooseArchive() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Archivio sul computer locale (utilizzabile con server locale)");
        var selected = chooser.showOpenDialog(archive.getScene().getWindow());
        if (selected != null) {
            archive.setText(selected.getAbsolutePath());
        }
    }

    @FXML private void predictClass() { Main.app().state().predict(); }

    @FXML private void confirmBranch() {
        try {
            Main.app().state().answer(Integer.parseInt(branch.getText().trim()));
        } catch (NumberFormatException exception) {
            Main.app().alert("Scelta ramo", "Inserire un indice intero. Le opzioni valide sono indicate nella domanda.");
        }
    }
}
