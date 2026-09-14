# mapGui - configurazione JavaFX

Terzo progetto indipendente e opzionale di Regression Tree Miner.
Per preparazione MySQL, build complessiva e utilizzo consultare il
[README principale](../README.md) e la [guida utente](../docs/GUIDA_UTENTE.md).

## Ambiente incluso

JavaFX SDK **21.0.12 Windows x64**, completo di JAR, librerie native e licenze
in `lib/javafx-sdk-21.0.12/`. Il JDK non è incluso; è richiesto Java 21.
Il SDK è distribuito una sola volta e non deve essere installato globalmente.

Origine: https://download2.gluonhq.com/openjfx/21.0.12/openjfx-21.0.12_windows-x64_bin-sdk.zip

SHA-256 del download utilizzato:
`78AB10816E71ED51808BC5853BB26F38CF35B550A4CEC233DBC4FBF5A3E8574C`.
Il checksum identifica il file ricevuto; non è una firma indipendente.
Lo ZIP di download non è duplicato nel pacchetto. Le licenze originali
del SDK sono conservate; il suo bundle sorgenti è materiale del fornitore.

## Build e run

Dalla directory del pacchetto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File mapGui/scripts/build.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File mapGui/scripts/run.ps1
```

Run ricompila la sola GUI; `-SkipBuild` evita tale passaggio.
Prima di avviare il server dalla GUI, compilare anche il server con lo script
complessivo `scripts/build.ps1`. La GUI non lancia il client console.

Gli script risolvono i percorsi da `$PSScriptRoot`, funzionano da directory
esterne e non scaricano nulla. La configurazione Java è:

```text
--module-path <mapGui>/lib/javafx-sdk-21.0.12/lib
--add-modules javafx.controls,javafx.fxml
-Dmap.root=<directory contenente i tre progetti>
-cp <mapGui>/bin;<mapGui>/resources
gui.Main
```

FXML e CSS restano in resources, inclusa nel classpath.
Nessun Connector JDBC è richiesto nel classpath GUI.

## Eclipse e finestra

Importare mapGui come progetto Java separato con i metadati inclusi e JDK 21.
Avviare gui.Main con il module-path/add-modules sopra indicato; impostare la
directory di lavoro alla radice del pacchetto o utilizzare `-Dmap.root`.
Gli script PowerShell restano il riferimento verificato senza dipendenza dall'IDE.

Stage decorato dal sistema e ridimensionabile, minimo 960×620, avvio 1280×720.
Sidebar persistente, layout gestiti e scorrimento verticale delle pagine lunghe.
Log/processo/connessione sono condivisi da GuiState; le operazioni bloccanti
avvengono fuori dal thread JavaFX e l'interfaccia viene aggiornata via runLater.

## Limiti

Il controllo Database verifica solo TCP, non autenticazione. Il pulsante SQL
apre lo script senza eseguirlo. FileChooser seleziona un archivio locale,
non effettua upload. Il server interpreta il percorso del file da caricare.
Il protocollo non offre salvataggio remoto o trasferimento grafico dell'albero.

La validazione è descritta in [Test e validazione](../docs/TEST_E_VALIDAZIONE.md).
