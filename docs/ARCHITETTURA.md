# Architettura di Regression Tree Miner

## Evoluzione MAP1-MAP6

| Fase | Introduzione principale |
| --- | --- |
| MAP1 | Classi Attribute e Data, aggregazione/composizione, array e ordinamento |
| MAP2 | Gerarchia Node, split discreti, foglie e apprendimento ricorsivo |
| MAP3 | Package, Keyboard, eccezioni e predizione |
| MAP4 | Collections/Generics, Iterable, Comparable e TreeSet |
| MAP5 | Attributi continui, RTTI, serializzazione e deserializzazione |
| MAP6 | Acquisizione JDBC/MySQL e separazione client-server multithread |

La distribuzione contiene la versione finale, non implementazioni duplicate
delle fasi intermedie. La GUI JavaFX è una successiva estensione opzionale.

## Tre progetti indipendenti

- **mapServer:** modello dati, apprendimento, predizione, serializzazione,
  accesso JDBC e gestione delle connessioni.
- **mapClient:** client console originale, con Socket e Keyboard.
- **mapGui:** client grafico JavaFX/FXML, controllo del processo server locale
  e visualizzazione dei log. Non importa classi interne del server.

Il solo server include MySQL Connector/J; il solo progetto GUI usa JavaFX.
I package server sono `data`, `database`, `tree` e `Server` (maiuscola significativa).

## Package data

| Classe | Responsabilità |
| --- | --- |
| Attribute | Base astratta serializzabile: nome e posizione di un attributo |
| DiscreteAttribute | Dominio discreto in Set<String>, iterabile; i domini costruiti da Data sono TreeSet |
| ContinuousAttribute | Specializzazione numerica di Attribute, senza ulteriori campi |
| Data | Acquisisce il training set, organizza schema/esempi e offre accesso e ordinamento |
| TrainingDataException | Segnala problemi nell'acquisizione o validità del training set |

Data conserva gli esempi in `List<Example>` e gli attributi in `List<Attribute>`.
L'ultima colonna della tabella è il target numerico; le precedenti sono gli
attributi esplicativi. L'ordinamento sceglie la partition discreta o continua
tramite `instanceof` e cast: è l'applicazione di RTTI richiesta da MAP5.
Gli scambi preservano sempre gli esempi completi, compreso il target.

## Package tree

| Classe | Responsabilità |
| --- | --- |
| Node | Base dei nodi: numero esempi, intervallo e SSE, memorizzata nel campo variance |
| SplitNode | Base degli split; informazioni dei rami e SSE complessiva dei figli |
| DiscreteNode | Raggruppa gli esempi per valore discreto e costruisce i relativi rami |
| ContinuousNode | Valuta soglie numeriche e partizioni <= soglia / > soglia |
| LeafNode | Calcola la media target del sottoinsieme, usata come classe prevista |
| RegressionTree | Costruisce ricorsivamente l'albero, predice e gestisce salva/carica |

Ereditarietà e polimorfismo permettono di trattare split e foglie come nodi,
mantenendo diversi comportamenti concreti. RegressionTree aggrega un nodo
radice e gli eventuali sottoalberi; il numero di rami determina i figli.

Per un sottoinsieme S, con media target m:

```text
SSE(S) = somma, per ogni esempio i in S, di (target_i - m)^2
SSE(split) = somma delle SSE dei sottoinsiemi figli
```

Il candidato con SSE minore è preferito. SplitNode implementa Comparable e
i candidati sono raccolti in un TreeSet; a parità di SSE il confronto restituisce
0, conservando il primo candidato inserito. Il sottoinsieme viene ordinato
nuovamente sull'attributo vincente prima della costruzione ricorsiva dei figli.

```text
Training Data
      |
      v
     Data
      |
      v
RegressionTree
      |
      v
 Split / Leaf
      |
      v
 Prediction
```

Serializable consente di conservare il grafo dell'albero e i suoi attributi.
`salva(String)` usa ObjectOutputStream; `carica(String)` usa ObjectInputStream.
Non esiste un comando remoto di salvataggio automatico dopo Learn.

## Package database

| Classe | Responsabilità |
| --- | --- |
| DbAccess | Configurazione e apertura/chiusura della connessione JDBC |
| TableSchema | Ricava lo schema della tabella dai metadati del database |
| Column | Nome/tipo di una colonna e riconoscimento dei tipi numerici |
| TableData | Esegue letture di tuple e valori distinti tramite JDBC |
| Example | Tupla di Object, iterabile e confrontabile |
| DatabaseConnectionException | Problemi di connessione al database |
| EmptySetException | Risultato senza esempi |

TableData usa SELECT DISTINCT: le 15 righe fisiche di provaC diventano
9 esempi distinti. I valori numerici sono letti come Double; X rimane String.
L'account didattico dispone del solo permesso applicativo SELECT.

## Package Server e client console

**MultiServer** apre ServerSocket sulla porta 8080 e accetta connessioni.
Per ogni client costruisce **ServerOneClient**, che mantiene training set e
albero della propria sessione e gestisce i comandi in un thread distinto.
Più sessioni possono lavorare contemporaneamente con modelli indipendenti.
**Server.UnknownValueException** segnala un indice che non corrisponde a un ramo.

**map7Client.MainTest** usa Socket e stream di oggetti, acquisisce scelte tramite
**utility.Keyboard** e stampa QUERY/risultati sul terminale. Il nome del package
è quello del codice fornito; il client appartiene all'architettura finale MAP6.

```text
GUI / Client
      | Socket
      v
ServerOneClient
      |
      +----> Data ---- JDBC ----> MySQL
      |
      v
RegressionTree
      |
      v
QUERY / Prediction
```

## Protocollo applicativo

Gli stream scambiano oggetti Java, non righe di testo. All'apertura si crea
ObjectOutputStream e si invia il suo header prima di attendere ObjectInputStream.

| Richiesta client | Risposta server |
| --- | --- |
| Integer 0, String tabella | String OK oppure errore di acquisizione |
| Integer 1, dopo acquisizione riuscita | String OK, modello appreso |
| Integer 2, String archivio | String OK oppure errore di caricamento |
| Integer 3 | QUERY + String domanda, ripetute; infine OK + Double oppure errore |
| Integer indice, dopo QUERY | Successiva QUERY, risultato o errore di ramo |

La sequenza delle richieste è vincolante. Non si manda comando 1 dopo un
fallimento del comando 0, né un altro comando mentre il server attende un ramo.
La rappresentazione dell'albero non viene trasferita. Il protocollo didattico
non include autenticazione, TLS, upload o identificatori di richiesta.

## GUI JavaFX

**Main extends Application** gestisce Stage standard decorato, Scene, caricamento
FXML, navigazione, documentazione e chiusura. I controller Home/Menu instradano
la navigazione; Server controlla il processo, RegressionTree gestisce i comandi
grafici, Database controlla TCP, Log mostra il testo e Documentation apre i file.

**GuiState** è l'unica classe condivisa: mantiene proprietà JavaFX, processo,
log e sessione Socket. Un executor serializza i comandi, mentre altri thread
gestiscono letture del processo e controlli di rete. Gli aggiornamenti UI
passano tramite Platform.runLater(). Tra una QUERY e la risposta umana nessun
thread resta bloccato in attesa di un controllo grafico.

ProcessBuilder lancia il server originale con argomenti distinti e percorsi
risolti dalla root. `netstat` verifica il PID in ascolto. La GUI impedisce doppi
avvii e arresta solo il processo posseduto; chiusura/disconnessione interrompono
le letture Socket e rilasciano gli executor.

## Limiti noti

- ContinuousNode conserva l'algoritmo didattico: un attributo continuo costante
  in un sottoinsieme può non produrre una soglia valida; il dataset ufficiale
  collaudato non presenta quel caso problematico.
- Il server presuppone la sequenza valida dei client previsti; comandi arbitrari
  fuori sequenza non costituiscono un protocollo di produzione.
- Gli archivi Java vanno considerati affidabili e compatibili con le classi.
- Gli script e il SDK inclusi sono per Windows x64; il JDK e MySQL sono prerequisiti
  esterni e non sono distribuiti nel pacchetto.
