# Regression Tree Miner

Progetto per l'esame di Metodi Avanzati di Programmazione

## Informazioni sulla consegna

| Campo | Valore |
| --- | --- |
| Studente | Claudio De Benedictis |
| Matricola | 777774 |
| Insegnamento | Metodi Avanzati di Programmazione |
| Progetto | Regression Tree Miner |
| Scadenza | 17 settembre 2026 |
| Modalità di consegna | WeTransfer |
| Email | c.debenedictis@studenti.uniba.it |

## 1. Descrizione

Regression Tree Miner è un sistema Java client-server che apprende alberi di
regressione da un training set e li utilizza per predire un target numerico.
Gli attributi esplicativi possono essere discreti o continui. La scelta dello
split minimizza la **SSE** (somma degli errori quadratici); la foglia restituisce
la media dei valori target del sottoinsieme raggiunto.

Il progetto comprende acquisizione dei dati da MySQL tramite JDBC, gestione
delle eccezioni, Collections/Generics, RTTI per gli attributi continui,
serializzazione/deserializzazione degli alberi e servizio multiclient tramite
Socket, ServerSocket e thread. La GUI JavaFX è un'estensione opzionale delle
esercitazioni MAP1-MAP6; il client console originale rimane indipendente.

## 2. Architettura

```text
+-----------------------+
|        mapGui         |
|        JavaFX         |
+-----------+-----------+
            |
            | Socket TCP - localhost:8080
            v
+-----------+-----------+       +-----------------------+
|       mapServer       | <---- |       mapClient       |
| Regression Tree Miner |  TCP  |    Client console     |
+-----------+-----------+       +-----------------------+
            |
            | JDBC - localhost:3306
            v
+-----------+-----------+
|        MySQL          |
|    MapDB / provaC     |
+-----------------------+
```

La GUI comunica direttamente con il server: non avvia il client console e
non incorpora gli algoritmi di apprendimento. Può avviare il server esistente
come processo separato e arrestare soltanto il processo da essa creato.

## 3. Struttura del pacchetto

```text
RegressionTreeMiner/
├── README.md
├── scripts/                    build complessiva e avvii console
├── mapServer/                  sorgenti, Connector, metadati Eclipse
│   └── sql/setup_mapdb.sql      script nel percorso usato dalla GUI
├── mapClient/                  client console e Keyboard
├── mapGui/                     sorgenti, FXML/CSS, SDK e script JavaFX
├── database/setup.sql          preparazione database documentata
└── docs/
    ├── ARCHITETTURA.md
    ├── GUIDA_UTENTE.md
    ├── TEST_E_VALIDAZIONE.md
    └── screenshots/
```

Sono distribuiti i sorgenti, non le classi compilate dell'applicazione.
Le cartelle `bin/` vengono rigenerate dalla build.

## 4. Tecnologie

Java 21, JavaFX 21.0.12, FXML, CSS, JDBC, MySQL, Socket, ServerSocket, Thread,
ObjectInputStream/ObjectOutputStream, Collections/Generics e Serializable.

Il Connector/J 8.0.17 è incluso **solo** in
`mapServer/lib/mysql-connector-java-8.0.17.jar`. Client e GUI non richiedono
direttamente JDBC. JavaFX è incluso una sola volta come SDK Windows x64 completo,
con librerie native e licenze.

## 5. Requisiti

- Windows x64 e Windows PowerShell per gli script forniti.
- JDK 21; collaudo eseguito con Temurin 21.0.2, con `java` e `javac` nel PATH.
- MySQL Server 8, disponibile su `localhost:3306`.
- Porta TCP 8080 libera per avviare il server locale.
- Account MySQL autorizzato alla preparazione iniziale del database.

JavaFX non va installato globalmente: il SDK locale è già incluso in
`mapGui/lib/javafx-sdk-21.0.12/`. Nessuna build scarica dipendenze; Maven, Gradle
e un IDE specifico non sono necessari. Verificare il JDK con `java -version`
e `javac -version`.

## 6. Preparazione del database

1. Avviare MySQL Server.
2. Leggere gli avvisi di `database/setup.sql`.
3. Aprire il file con MySQL Workbench ed eseguirlo con un account amministrativo.

In alternativa, da **Prompt dei comandi Windows**, nella cartella del pacchetto:

```bat
mysql -u root -p < database\setup.sql
```

La password amministrativa viene richiesta dal client MySQL, non salvata negli
script. Il nome dell'account amministrativo può essere diverso da `root`.

**Attenzione:** lo script elimina e ricrea soltanto `MapDB.provaC` e riallinea
l'account didattico. Non eseguirlo su una tabella da conservare senza backup.
Le operazioni DDL MySQL non sono rese annullabili dal successivo START TRANSACTION.

La configurazione del server prevista dall'esercitazione è:

| Voce | Valore |
| --- | --- |
| URL | `jdbc:mysql://localhost:3306/MapDB?serverTimezone=UTC` |
| Account didattico | `MapUser`@`localhost` |
| Password didattica | `map` |
| Permessi applicativi | `SELECT` su `MapDB.*` |
| Tabella | `provaC`: X discreto, Y continuo, C target |

`MapUser/map` è una configurazione didattica della specifica MAP6, non una
credenziale personale o una scelta per un servizio di produzione.
Il setup inserisce le 15 tuple ufficiali; i controlli finali devono mostrare
`physical_rows = 15` e `distinct_rows = 9`. L'acquisizione di Data usa le tuple
distinte, quindi il training set applicativo contiene 9 esempi.

`mapServer/sql/setup_mapdb.sql` è conservato byte-per-byte nel percorso già
utilizzato dal pulsante GUI. Prepara gli stessi dati e account. È sufficiente
eseguire **uno solo** dei due script, non entrambi.

## 7. Compilazione

Aprire PowerShell nella directory `RegressionTreeMiner` estratta dallo ZIP:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1
```

Lo script compila separatamente server, client e GUI e interrompe la sequenza
se una compilazione fallisce. `ExecutionPolicy Bypass` vale solo per il
processo lanciato: non modifica la policy di sistema.

Comandi equivalenti per server e client:

```powershell
$serverSources = @(Get-ChildItem mapServer/src -Recurse -File -Filter '*.java' | ForEach-Object FullName)
javac -encoding UTF-8 -d mapServer/bin @serverSources
$clientSources = @(Get-ChildItem mapClient/src -Recurse -File -Filter '*.java' | ForEach-Object FullName)
javac -encoding UTF-8 -d mapClient/bin @clientSources
```

Per la sola GUI:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File mapGui/scripts/build.ps1
```

La build server segnala una nota su operazioni unchecked in Example. Il controllo
aggiuntivo con `-Xlint:all` produce 19 warning noti (serial, unchecked e this-escape),
senza errori. Client e GUI compilano senza warning.

## 8. Avvio

Percorso consigliato, dopo la build e la preparazione MySQL:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File mapGui/scripts/run.ps1
```

Nella GUI: **Avvia applicazione → Server → Avvia server**.
Quindi aprire Regression Tree e premere Connetti con `localhost`, `8080`.
Non avviare un secondo server manualmente se la GUI ne ha già avviato uno.

Percorso manuale, in terminali distinti:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/run-server.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/run-client.ps1
```

Comandi Java equivalenti:

```powershell
java -cp "mapServer/bin;mapServer/lib/mysql-connector-java-8.0.17.jar" Server.MultiServer
java -cp "mapClient/bin" map7Client.MainTest localhost 8080
```

Si può usare la GUI anche con un server avviato manualmente: connetterla dalla
schermata Regression Tree, senza premere Avvia server. La GUI non arresta
processi esterni; il server manuale si chiude dal suo terminale con Ctrl+C.

## 9. Utilizzo della GUI

- **Home:** accesso alla dashboard, documentazione e uscita.
- **Dashboard:** sidebar e card per accedere alle funzioni.
- **Server:** avvio/arresto locale, stato e log del processo.
- **Regression Tree:** connessione, apprendimento o caricamento e predizione.
- **Database:** controllo TCP su 3306; non verifica autenticazione o MapDB.
- **Log:** output server e diagnostica; area read-only, con pulsante Pulisci log.
- **Documentazione:** descrizione del sistema e apertura dei README locali.

Finestra standard ridimensionabile, minimo 960×620. Le pagine lunghe scorrono
verticalmente; la sidebar e il risultato della predizione rimangono accessibili.
Cambiare schermata non perde il server, i log o la connessione.

## 10. Apprendimento

In Regression Tree, dopo Connetti, selezionare **Apprendi nuovo albero**,
lasciare `provaC` nel campo tabella e premere **Apprendi**.
La GUI esegue acquisizione e apprendimento sul server; un esito positivo
abilita Nuova predizione. Il server non invia la struttura grafica dell'albero.

## 11. Predizione

Premere **Nuova predizione**. Per ogni QUERY leggere i rami ricevuti, scegliere
l'opzione dalla ComboBox o digitare l'indice e premere Conferma.
I numeri sono gli **indici dei rami mostrati**, non valori da conoscere a priori.

| Sequenza di indici per provaC | Classe prevista |
| --- | --- |
| 0, 0 | 1.0 |
| 0, 1 | 1.5 |
| 1 | 10.0 |

Alla fine il valore appare accanto a Classe prevista. Si può iniziare una
nuova predizione nella stessa connessione. Per annullare una domanda pendente,
premere Disconnetti.

## 12. Serializzazione e caricamento

RegressionTree offre `salva(String)` e `carica(String)` tramite serializzazione
Java. **Learn non salva automaticamente un archivio.** Il protocollo remoto
consente Load Archive ma non espone un comando di salvataggio; la GUI non ne
aggiunge uno. Un archivio deve essere stato creato tramite l'API server.

In modalità Carica albero, inserire il percorso dell'archivio e premere Carica.
Il file deve essere accessibile al **filesystem del server**; un percorso
relativo è risolto dalla directory di lavoro del server. Sfoglia sceglie un
file locale e non effettua upload: è utile con un server sullo stesso computer.
Caricare solo archivi affidabili e compatibili con le classi distribuite.

## 13. Client console

`mapClient` è il client della consegna MAP6 e funziona senza GUI. Il menu
propone `1` per Learn e `2` per Load. Con Learn digitare `provaC`; poi rispondere
alle QUERY con gli indici ricevuti. Alla richiesta di ripetizione, usare `y`
o `n`. Per ottenere 1.5 da provaC, scegliere i rami `0`, poi `1`.

## 14. Gestione degli errori

| Problema | Controllo consigliato |
| --- | --- |
| MySQL non disponibile | Avviare il servizio e verificare localhost:3306 |
| TCP MySQL raggiungibile ma Learn fallisce | Verificare MapDB, account, permessi e tabella |
| Server non raggiungibile | Verificare avvio, host, porta e firewall |
| Porta 8080 occupata | Evitare un secondo server; usare quello già attivo o chiuderlo manualmente |
| Tabella inesistente | Usare provaC dopo la preparazione database |
| Archivio inesistente | Verificare il percorso sul computer del server |
| Ramo non valido | Avviare una nuova predizione e scegliere un indice mostrato |
| Classi server assenti | Eseguire scripts/build.ps1 |

Gli errori GUI sono mostrati tramite messaggi/Alert comprensibili; la
diagnostica tecnica resta nella schermata Log. Il protocollo è didattico:
non comprende autenticazione, cifratura o uso come servizio pubblico.

## 15. Documentazione e note finali

- [Architettura tecnica](docs/ARCHITETTURA.md)
- [Guida operativa](docs/GUIDA_UTENTE.md)
- [Test e validazione](docs/TEST_E_VALIDAZIONE.md)
- [Configurazione JavaFX](mapGui/README.md)

Eclipse può importare i tre progetti separati tramite i metadati inclusi.
Gli script sono il riferimento per una build riproducibile senza IDE.
La GUI aggiunge soltanto una modalità grafica di utilizzo: **non sostituisce
l'architettura client-server originale né modifica le funzionalità MAP1-MAP6**.
