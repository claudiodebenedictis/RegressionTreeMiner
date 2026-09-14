package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class MenuController {
    @FXML private Label serverBadge;

    @FXML private void initialize() {
        serverBadge.textProperty().bind(Main.app().state().serverStatus);
    }

    @FXML private void home() { Main.app().show("home"); }
    @FXML private void dashboard() { Main.app().show("menu"); }
    @FXML private void server() { Main.app().show("server"); }
    @FXML private void tree() { Main.app().show("regression-tree"); }
    @FXML private void database() { Main.app().show("database"); }
    @FXML private void log() { Main.app().show("log"); }
    @FXML private void documentation() { Main.app().show("documentation"); }
    @FXML private void exit() { Main.app().requestExit(); }

    static void select(BorderPane menu, String view) {
        for (var node : menu.lookupAll(".nav-button")) {
            Button button = (Button) node;
            button.getStyleClass().remove("nav-selected");
            if (view.equals(button.getUserData())) {
                button.getStyleClass().add("nav-selected");
            }
        }
    }
}
