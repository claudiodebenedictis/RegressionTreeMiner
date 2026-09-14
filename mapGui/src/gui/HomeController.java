package gui;

import javafx.fxml.FXML;

public class HomeController {
    @FXML private void start() { Main.app().show("menu"); }
    @FXML private void documentation() { Main.app().show("documentation"); }
    @FXML private void exit() { Main.app().requestExit(); }
}
