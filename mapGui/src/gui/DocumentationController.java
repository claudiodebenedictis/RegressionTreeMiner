package gui;

import javafx.fxml.FXML;

public class DocumentationController {
    @FXML private void openReadme() {
        Main.app().openLocalFile(Main.app().state().root().resolve("README.md"));
    }

    @FXML private void openGuiReadme() {
        Main.app().openLocalFile(Main.app().state().root().resolve("mapGui/README.md"));
    }
}
