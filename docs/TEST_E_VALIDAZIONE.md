# Test e validazione

## Ambiente e metodo

Collaudo finale eseguito il **14 settembre 2026**, su Windows x64, con
Temurin JDK 21.0.2, JavaFX SDK 21.0.12, Connector/J 8.0.17 e MySQL 8.

Il pacchetto è stato copiato in una directory temporanea isolata, partendo
senza classi applicative compilate. Build e avvii hanno utilizzato esclusivamente
i sorgenti e le librerie della copia. Il JDK e il servizio MySQL sono prerequisiti
esterni; non sono state usate classi dell'ambiente di sviluppo.
L'infrastruttura automatica di collaudo è stata usata solo nella copia di
verifica e non è distribuita nel pacchetto finale.

## Compilazione

| Progetto | Sorgenti | Esito |
| --- | --- | --- |
| mapServer | 21 | Exit code 0; nota unchecked in Example |
| mapClient | 2 | Exit code 0, nessun warning |
| mapGui | 9 | Exit code 0, nessun warning |

La build standard è quella fornita da `scripts/build.ps1`. Un controllo
aggiuntivo del server con `-Xlint:all` ha prodotto **19 warning noti**, senza
errori, nelle categorie serial, unchecked e this-escape. Non è stato modificato
il codice didattico per eliminarli. Nessun warning runtime FXML/CSS nel collaudo GUI.

## GUI: 105 controlli superati

Il collaudo automatico corrente ha confermato **105 controlli GUI superati**,
con exit code 0. Le verifiche comprendono:

- caricamento dei 7 FXML, Home/Dashboard, sidebar e card;
- finestra decorata, spostamento, minimizzazione e massimizzazione;
- geometria e contenuti a 960×620, 1024×650 e 1280×720, crescita TextArea,
  sidebar utilizzabile e risultato della predizione visibile;
- errori FXML e classi server assenti gestiti tramite messaggi;
- avvio del server esistente, PID in ascolto sulla porta 8080, doppio avvio
  impedito, acquisizione output e conservazione di stato/log;
- connessione Socket, Learn su provaC, caricamento e predizione;
- ramo, tabella e archivio non validi, con recupero della sessione;
- controllo TCP MySQL, log read-only e Pulisci log;
- annullamento della connessione pendente, arresto e chiusura senza processi
  posseduti o listener 8080 residui.

I casi MySQL non disponibile e perdita Socket durante predizione sono
stati **simulati** con endpoint di test locali. Il servizio MySQL reale non
è stato fermato e il database non è stato alterato. Gli screenshot della
guida sono selezioni reali della GUI, senza copiare l'intera raccolta di test.

## JDBC, dati e ordinamento

Sono verificati l'accesso autenticato con l'account didattico e le **15 righe
ufficiali**, con tutte le molteplicità dei duplicati, corrispondenti a **9 tuple
distinte**. Data riconosce X discreto, Y continuo e C target numerico.

L'ordinamento crescente su X e Y conserva il multinsieme degli esempi completi:
ogni target resta associato agli attributi della propria tupla durante gli scambi.
Le verifiche sono di sola lettura.

`database/setup.sql` è confrontato staticamente con lo script e le tuple
MAP6 già verificate. Contiene le sezioni database, account/SELECT, tabella e
15 INSERT ufficiali. Non è stato rieseguito sul database esistente, perché
contiene DROP TABLE e ALTER USER. Non si dichiara un nuovo collaudo distruttivo.

## Predizioni e serializzazione

| Risposte alle QUERY per provaC | Valore verificato |
| --- | --- |
| 0, 0 | 1.0 |
| 0, 1 | 1.5 |
| 1 | 10.0 |

La serializzazione tramite l'API originale del server e il successivo
caricamento mantengono invariata la rappresentazione dell'albero.
Sono verificati anche il comando remoto Load Archive e una predizione
successiva pari a 1.5. Nessun archivio di prova è incluso nella consegna.

## Multiclient e client console

Il collaudo aggiuntivo ha superato **12 controlli JDBC/multiclient/console**,
con exit code 0. Tre connessioni reali simultanee hanno appreso modelli di
sessione indipendenti e predetto rispettivamente 1.0, 1.5 e 10.0.

Il client console originale è stato avviato con lo script distribuito;
input: Learn, provaC, rami 0 e 1, risposta n alla ripetizione.
Risultato: `Predicted class:1.5`, exit code 0.
Il processo server creato dal collaudo è stato poi terminato.

## Ripetizione pratica del collaudo

1. Preparare MySQL seguendo il README; verificare 15 righe fisiche e 9 distinte.
2. Eseguire `scripts/build.ps1` partendo senza cartelle bin applicative.
3. Avviare la GUI e controllare tutte le schermate.
4. Avviare server, connettere Tree e apprendere da provaC.
5. Ripetere i tre percorsi della tabella sopra; provare un ramo non valido.
6. Aprire più client console per verificare sessioni simultanee.
7. Arrestare il server e chiudere la GUI; verificare che 8080 non sia più
   in ascolto se il server era stato avviato dalla GUI.

I sorgenti e le risorse applicative sono confrontati tramite SHA-256 tra
originali, staging e copia verificata. I sorgenti server/client non sono
stati modificati dal packaging. Lo ZIP definitivo viene inoltre estratto
e verificato prima della consegna.

## Limiti del collaudo

Gli script e il SDK sono collaudati su Windows x64. L'apertura dei documenti
dipende dalle associazioni file del sistema; il caricamento degli archivi
richiede file affidabili accessibili sul server. I limiti algoritmici e del
protocollo didattico sono descritti in [Architettura](ARCHITETTURA.md).
