package gui;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Punto di ingresso della sola GUI: non importa classi del server. */
public class Main extends Application {
    private static Main application;
    private final Map<String, Parent> views = new HashMap<>();
    private Stage stage;
    private Scene homeScene;
    private Scene menuScene;
    private BorderPane menu;
    private Parent dashboard;
    private GuiState state;
    private boolean closing;

    public static Main app() {
        return application;
    }

    public GuiState state() {
        return state;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        application = this;
        Platform.setImplicitExit(false);
        stage = primaryStage;
        state = new GuiState(findProjectRoot());
        stage.setTitle("Regression Tree Miner");
        stage.setResizable(true);
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setOnCloseRequest(event -> {
            event.consume();
            requestExit();
        });
        try {
            navigate("home");
            stage.show();
        } catch (IOException exception) {
            state.logException("Caricamento iniziale FXML", exception);
            alert("Avvio non riuscito", "Impossibile caricare l'interfaccia. Verificare resources e consultare i log.");
            requestExit();
            throw exception;
        }
    }

    public void navigate(String view) throws IOException {
        if (view.equals("home")) {
            if (homeScene == null) {
                homeScene = scene(load("home"));
            }
            switchScene(homeScene);
            return;
        }
        if (menuScene == null) {
            menu = (BorderPane) load("menu");
            dashboard = (Parent) menu.getCenter();
            menuScene = scene(menu);
        }
        menu.setCenter(view.equals("menu") ? dashboard : load(view));
        switchScene(menuScene);
        MenuController.select(menu, view);
    }

    private void switchScene(Scene scene) {
        if (stage.getScene() == scene) {
            return;
        }
        double width = stage.getWidth();
        double height = stage.getHeight();
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);
    }

    public void show(String view) {
        try {
            navigate(view);
        } catch (IOException exception) {
            state.logException("FXML " + view, exception);
            alert("Schermata non disponibile", "Impossibile aprire la schermata richiesta. Consultare i log tecnici.");
        }
    }

    private Parent load(String view) throws IOException {
        if (!views.containsKey(view)) {
            var resource = Main.class.getResource("/fxml/" + view + ".fxml");
            if (resource == null) {
                throw new IOException("Risorsa FXML assente: " + view);
            }
            views.put(view, new FXMLLoader(resource).load());
        }
        return views.get(view);
    }

    private Scene scene(Parent root) throws IOException {
        Scene scene = new Scene(root);
        var css = Main.class.getResource("/css/style.css");
        if (css == null) {
            throw new IOException("Risorsa CSS assente");
        }
        scene.getStylesheets().add(css.toExternalForm());
        return scene;
    }

    public void openLocalFile(Path file) {
        state.background(() -> {
            try {
                if (!Files.isRegularFile(file)) {
                    throw new IOException("File non disponibile: " + file.getFileName());
                }
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    throw new IOException("Apertura file non supportata dal sistema");
                }
                Desktop.getDesktop().open(file.toFile());
            } catch (IOException | SecurityException exception) {
                state.logException("Apertura documentazione", exception);
                Platform.runLater(() -> alert("Documento non aperto", "Aprire manualmente il file: " + file));
            }
        });
    }

    public void alert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
    }

    public void requestExit() {
        if (closing) {
            return;
        }
        closing = true;
        stage.hide();
        state.shutdown().whenComplete((ignored, error) -> Platform.runLater(Platform::exit));
    }

    @Override
    public void stop() {
        if (state != null) {
            state.shutdown();
        }
    }

    static Path findProjectRoot() throws IOException {
        String configured = System.getProperty("map.root");
        Path location = configured == null
            ? Path.of(System.getProperty("user.dir")) : Path.of(configured);
        while (location != null) {
            if (Files.isDirectory(location.resolve("mapServer/src"))
                && Files.isDirectory(location.resolve("mapClient/src"))) {
                return location.toRealPath();
            }
            location = location.toAbsolutePath().getParent();
        }
        throw new IOException("Root non trovata: avviare tramite mapGui/scripts/run.ps1");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
