package gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Unico stato condiviso. Un executor serializza i comandi del protocollo. */
public class GuiState {
    final StringProperty serverStatus = new SimpleStringProperty("Server arrestato");
    final StringProperty logs = new SimpleStringProperty("");
    final BooleanProperty serverStarting = new SimpleBooleanProperty(false);
    final BooleanProperty serverOwned = new SimpleBooleanProperty(false);
    final StringProperty connectionStatus = new SimpleStringProperty("Disconnesso");
    final BooleanProperty connected = new SimpleBooleanProperty(false);
    final BooleanProperty working = new SimpleBooleanProperty(false);
    final BooleanProperty treeReady = new SimpleBooleanProperty(false);
    final BooleanProperty waitingAnswer = new SimpleBooleanProperty(false);
    final StringProperty query = new SimpleStringProperty("");
    final StringProperty prediction = new SimpleStringProperty("—");
    final StringProperty treeMessage = new SimpleStringProperty("Connettersi al server per iniziare.");
    final StringProperty mysqlStatus = new SimpleStringProperty("Disponibilità non verificata");
    final BooleanProperty checkingMysql = new SimpleBooleanProperty(false);
    private final Path root;
    private final ExecutorService workers = Executors.newCachedThreadPool(runnable -> daemon(runnable, "gui-worker"));
    private final ExecutorService network = Executors.newSingleThreadExecutor(runnable -> daemon(runnable, "gui-socket"));
    private volatile Process server;
    private volatile Connection connection;
    private volatile Socket connectingSocket;
    private volatile boolean closed;
    private long connectionVersion;
    private CompletableFuture<Void> shutdown;

    private static Thread daemon(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }

    public GuiState(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    public void background(Runnable action) {
        if (!closed) {
            workers.execute(action);
        }
    }

    private void fx(Runnable action) {
        Platform.runLater(() -> {
            if (!closed) {
                action.run();
            }
        });
    }

    public void log(String message) {
        fx(() -> {
            String normalized = message.replace("\r\n", "\n").replace('\r', '\n');
            String updated = logs.get() + "[" + LocalTime.now().withNano(0) + "] " + normalized + "\n";
            // Evita una crescita illimitata nelle sessioni molto lunghe.
            logs.set(updated.length() > 100_000 ? updated.substring(updated.length() - 100_000) : updated);
        });
    }

    public void logException(String context, Throwable error) {
        StringWriter text = new StringWriter();
        error.printStackTrace(new PrintWriter(text));
        log(context + "\n" + text);
    }

    public void clearLogs() {
        logs.set("");
    }

    public void startServer() {
        if (closed || serverStarting.get() || serverOwned.get()) {
            return;
        }
        serverStarting.set(true);
        serverStatus.set("Avvio del server…");
        background(() -> {
            Process created = null;
            try {
                if (!portAvailable(8080)) {
                    throw new IOException("Porta 8080 occupata: nessun processo esterno verrà arrestato.");
                }
                Path classes = root.resolve("mapServer/bin");
                Path connector = root.resolve("mapServer/lib/mysql-connector-java-8.0.17.jar");
                if (!Files.isRegularFile(classes.resolve("Server/MultiServer.class")) || !Files.isRegularFile(connector)) {
                    throw new IOException("Classi server o Connector assenti. Compilare mapServer seguendo il README.");
                }
                String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
                created = new ProcessBuilder(java, "-cp", classes + File.pathSeparator + connector, "Server.MultiServer")
                    .directory(root.toFile()).redirectErrorStream(true).start();
                synchronized (this) {
                    server = created;
                    if (closed) {
                        terminate(created);
                        return;
                    }
                }
                Process owned = created;
                log("Avviato Server.MultiServer, PID " + owned.pid() + ".");
                background(() -> captureOutput(owned));
                boolean listening = false;
                for (int attempt = 0; attempt < 30 && owned.isAlive() && !closed; attempt++) {
                    if (listenerPid(8080) == owned.pid()) {
                        listening = true;
                        break;
                    }
                    Thread.sleep(100);
                }
                if (!listening || !owned.isAlive()) {
                    throw new IOException("Il server non ha aperto la porta 8080. Consultare il log.");
                }
                fx(() -> {
                    if (server == owned && owned.isAlive()) {
                        serverOwned.set(true);
                        serverStarting.set(false);
                        serverStatus.set("Server in esecuzione · localhost:8080");
                    }
                });
                log("Porta 8080 in ascolto, PID verificato.");
            } catch (IOException | InterruptedException exception) {
                if (created != null) {
                    terminate(created);
                }
                logException("Avvio server", exception);
                fx(() -> {
                    serverStarting.set(false);
                    serverOwned.set(false);
                    serverStatus.set("Server non avviato");
                    Main.app().alert("Avvio server non riuscito", exception.getMessage());
                });
            }
        });
    }

    private void captureOutput(Process owned) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(owned.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log("SERVER | " + line);
            }
            int exitCode = owned.waitFor();
            log("Processo server terminato, exit code " + exitCode + ".");
            fx(() -> {
                if (server == owned) {
                    serverOwned.set(false);
                    serverStatus.set("Server arrestato");
                }
            });
        } catch (IOException | InterruptedException exception) {
            if (!closed) {
                logException("Lettura output server", exception);
            }
        }
    }

    public void stopServer() {
        Process owned = server;
        if (owned == null || serverStarting.get() || !serverOwned.get()) {
            return;
        }
        serverStarting.set(true);
        serverStatus.set("Arresto del server…");
        disconnect();
        background(() -> {
            terminate(owned);
            log("Arrestato esclusivamente il server creato dalla GUI.");
            fx(() -> {
                serverOwned.set(false);
                serverStarting.set(false);
                serverStatus.set("Server arrestato");
            });
        });
    }

    static boolean portAvailable(int port) {
        try (ServerSocket probe = new ServerSocket()) {
            probe.setReuseAddress(false);
            probe.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    /** Osserva il listener senza aprire connessioni TCP incomplete al server. */
    static long listenerPid(int port) throws IOException, InterruptedException {
        Process probe = new ProcessBuilder("netstat", "-ano", "-p", "tcp").redirectErrorStream(true).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(probe.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 5 && parts[0].equals("TCP")
                    && parts[1].endsWith(":" + port) && parts[3].equals("LISTENING")) {
                    return Long.parseLong(parts[4]);
                }
            }
        } finally {
            if (!probe.waitFor(2, TimeUnit.SECONDS)) {
                probe.destroyForcibly();
            }
        }
        return -1;
    }

    public void checkMysql() {
        if (checkingMysql.get()) {
            return;
        }
        checkingMysql.set(true);
        mysqlStatus.set("Verifica della porta…");
        background(() -> {
            boolean reachable;
            try (Socket probe = new Socket()) {
                probe.connect(new InetSocketAddress("localhost", 3306), 1500);
                reachable = true;
            } catch (IOException exception) {
                reachable = false;
                log("MySQL: porta 3306 non raggiungibile.");
            }
            boolean result = reachable;
            fx(() -> {
                mysqlStatus.set(result ? "Porta MySQL raggiungibile" : "Porta non raggiungibile");
                checkingMysql.set(false);
            });
            if (reachable) {
                log("MySQL: porta 3306 raggiungibile (autenticazione non verificata).");
            }
        });
    }

    public void connect(String host, int port) {
        if (closed || connected.get() || working.get()) {
            return;
        }
        working.set(true);
        connectionStatus.set("Connessione…");
        final long version;
        synchronized (this) {
            version = ++connectionVersion;
        }
        network.execute(() -> {
            Socket socket = new Socket();
            synchronized (this) {
                if (closed || connectionVersion != version) {
                    closeSocket(socket);
                    return;
                }
                connectingSocket = socket;
            }
            try {
                socket.connect(new InetSocketAddress(host, port), 3000);
                socket.setSoTimeout(15000);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                in.setObjectInputFilter(ObjectInputFilter.Config.createFilter("java.lang.String;java.lang.Integer;java.lang.Double;java.lang.Number;!*"));
                Connection session = new Connection(socket, in, out);
                synchronized (this) {
                    if (closed || socket.isClosed() || connectionVersion != version) {
                        socket.close();
                        return;
                    }
                    connection = session;
                }
                fx(() -> {
                    if (connection == session) {
                        connected.set(true);
                        working.set(false);
                        connectionStatus.set("Connesso · " + host + ":" + port);
                        treeMessage.set("Connessione pronta. Apprendere o caricare un albero.");
                    }
                });
                log("Socket connesso a " + host + ":" + port + ".");
            } catch (IOException exception) {
                closeSocket(socket);
                logException("Connessione Socket", exception);
                fx(() -> {
                    if (connectionVersion == version) {
                        working.set(false);
                        connectionStatus.set("Disconnesso");
                        treeMessage.set("Server non raggiungibile o connessione interrotta.");
                    }
                });
            } finally {
                synchronized (this) {
                    if (connectingSocket == socket) {
                        connectingSocket = null;
                    }
                }
            }
        });
    }

    public void disconnect() {
        Connection session;
        synchronized (this) {
            connectionVersion++;
            session = connection;
            connection = null;
            closeSocket(connectingSocket);
        }
        if (session != null) {
            closeSocket(session.socket);
        }
        connected.set(false);
        working.set(false);
        treeReady.set(false);
        waitingAnswer.set(false);
        query.set("");
        prediction.set("—");
        connectionStatus.set("Disconnesso");
        treeMessage.set("Connessione chiusa.");
    }

    public void learn(String table) {
        command(session -> {
            send(session, 0, table);
            String result = response(session);
            if (result.equals("OK")) {
                send(session, 1);
                result = response(session);
            }
            modelResult(session, result, "Albero appreso dalla tabella " + table + ".");
        }, true);
    }

    public void load(String archive) {
        command(session -> {
            send(session, 2, archive);
            modelResult(session, response(session), "Albero caricato dall'archivio.");
        }, true);
    }

    private void modelResult(Connection session, String result, String success) {
        fx(() -> {
            if (connection == session) {
                treeReady.set(result.equals("OK"));
                treeMessage.set(result.equals("OK") ? success : friendly(result));
            }
        });
        log(result.equals("OK") ? success : "Risposta server: " + result);
    }

    public void predict() {
        if (!treeReady.get()) {
            return;
        }
        prediction.set("—");
        command(session -> {
            send(session, 3);
            predictionResponse(session);
        }, false);
    }

    public void answer(int branch) {
        if (!waitingAnswer.get() || working.get() || connection == null) {
            return;
        }
        waitingAnswer.set(false);
        command(session -> {
            send(session, branch);
            predictionResponse(session);
        }, false);
    }

    private void predictionResponse(Connection session) throws IOException, ClassNotFoundException {
        String result = response(session);
        if (result.equals("QUERY")) {
            String question = response(session);
            fx(() -> {
                if (connection == session) {
                    query.set(question);
                    waitingAnswer.set(true);
                    treeMessage.set("Selezionare un ramo e confermare l'indice.");
                }
            });
        } else if (result.equals("OK")) {
            Object value = session.in.readObject();
            if (!(value instanceof Double)) {
                throw new IOException("Risposta di predizione non valida");
            }
            fx(() -> {
                if (connection == session) {
                    query.set("");
                    prediction.set(value.toString());
                    treeMessage.set("Predizione completata.");
                }
            });
            log("Classe prevista: " + value);
        } else {
            fx(() -> {
                if (connection == session) {
                    query.set("");
                    treeMessage.set(friendly(result));
                }
            });
            log("Predizione: " + result);
        }
    }

    private interface SocketAction {
        void run(Connection session) throws IOException, ClassNotFoundException;
    }

    private void command(SocketAction action, boolean replacesModel) {
        Connection session = connection;
        if (closed || session == null || working.get() || waitingAnswer.get()) {
            return;
        }
        working.set(true);
        treeMessage.set("Operazione in corso…");
        if (replacesModel) {
            treeReady.set(false);
            query.set("");
            prediction.set("—");
        }
        network.execute(() -> {
            try {
                action.run(session);
            } catch (IOException | ClassNotFoundException | RuntimeException exception) {
                logException("Protocollo Socket", exception);
                closeSocket(session.socket);
                fx(() -> {
                    if (connection == session) {
                        disconnect();
                        treeMessage.set("Connessione interrotta o risposta non valida. Riconnettersi al server.");
                    }
                });
            } finally {
                fx(() -> {
                    if (connection == session) {
                        working.set(false);
                    }
                });
            }
        });
    }

    private static void send(Connection session, Object... objects) throws IOException {
        for (Object object : objects) {
            session.out.writeObject(object);
        }
        session.out.flush();
    }

    private static String response(Connection session) throws IOException, ClassNotFoundException {
        Object response = session.in.readObject();
        if (!(response instanceof String text)) {
            throw new IOException("Risposta del server non testuale");
        }
        return text;
    }

    private static String friendly(String response) {
        if (response.contains("UnknownValueException")) {
            return "Indice del ramo non valido. Avviare una nuova predizione e scegliere uno degli indici mostrati.";
        }
        if (response.contains("TrainingDataException")) {
            return "Training set non disponibile. Verificare MySQL, la configurazione del server e il nome della tabella.";
        }
        if (response.contains("FileNotFoundException")) {
            return "Archivio non trovato sul computer del server. Verificare il percorso.";
        }
        if (response.contains("Exception")) {
            return "Operazione non riuscita sul server. Verificare l'archivio o consultare i log tecnici.";
        }
        return "Operazione non riuscita: " + response;
    }

    private record Connection(Socket socket, ObjectInputStream in, ObjectOutputStream out) { }

    private static void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // La chiusura della sessione interrompe anche letture bloccanti.
            }
        }
    }

    private static void terminate(Process process) {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    public synchronized CompletableFuture<Void> shutdown() {
        if (shutdown != null) {
            return shutdown;
        }
        closed = true;
        closeSocket(connectingSocket);
        if (connection != null) {
            closeSocket(connection.socket);
        }
        shutdown = CompletableFuture.runAsync(() -> {
            network.shutdownNow();
            workers.shutdownNow();
            terminate(server);
            try {
                // Attende anche un eventuale ProcessBuilder ancora in avvio.
                workers.awaitTermination(5, TimeUnit.SECONDS);
                network.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            terminate(server);
        });
        return shutdown;
    }
}
