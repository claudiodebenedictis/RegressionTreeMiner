package gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class DatabaseController {
    @FXML private Label mysqlStatus;
    @FXML private Button checkMysql;
    @FXML private Button openSql;

    @FXML private void initialize() {
        GuiState state = Main.app().state();
        mysqlStatus.textProperty().bind(state.mysqlStatus);
        checkMysql.disableProperty().bind(state.checkingMysql);
        openSql.setDisable(!java.nio.file.Files.isRegularFile(state.root().resolve("mapServer/sql/setup_mapdb.sql")));
    }

    @FXML private void check() { Main.app().state().checkMysql(); }
    @FXML private void openScript() {
        Main.app().openLocalFile(Main.app().state().root().resolve("mapServer/sql/setup_mapdb.sql"));
    }
}
