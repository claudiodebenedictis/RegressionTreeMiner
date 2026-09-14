-- Regression Tree Miner - preparazione del database didattico MAP6
-- Eseguire con MySQL 8 e un account amministrativo autorizzato.
-- ATTENZIONE: la sezione 3 elimina e ricrea SOLO MapDB.provaC.
-- La sezione 2 riallinea la password dell'account didattico MapUser.
-- Non eseguire se la tabella esistente o quell'account devono essere conservati.

-- 1. Database richiesto dall'applicazione.
CREATE DATABASE IF NOT EXISTS MapDB;

-- 2. Account e permessi didattici, separati dalle operazioni sulla tabella.
-- MapUser/map sono credenziali di esempio della specifica MAP6,
-- non credenziali personali e non una configurazione di produzione.
CREATE USER IF NOT EXISTS 'MapUser'@'localhost' IDENTIFIED BY 'map';
ALTER USER 'MapUser'@'localhost' IDENTIFIED BY 'map';
GRANT SELECT ON MapDB.* TO 'MapUser'@'localhost';

-- 3. Tabella ufficiale: X discreto; Y continuo; C target numerico.
DROP TABLE IF EXISTS MapDB.provaC;
CREATE TABLE MapDB.provaC (
    X VARCHAR(10),
    Y FLOAT(5,2),
    C FLOAT(5,2)
);

-- 4. Le 15 tuple ufficiali MAP6, senza dati aggiuntivi.
-- I duplicati sono intenzionali; Data utilizza le 9 tuple distinte.
START TRANSACTION;
INSERT INTO MapDB.provaC (X, Y, C) VALUES
    ('A', 2, 1),
    ('A', 2, 1),
    ('A', 1, 1),
    ('A', 2, 1),
    ('A', 5, 1.5),
    ('A', 5, 1.5),
    ('A', 6, 1.5),
    ('B', 6, 10),
    ('A', 6, 1.5),
    ('A', 6, 1.5),
    ('B', 10, 10),
    ('B', 5, 10),
    ('B', 12, 10),
    ('B', 14, 10),
    ('A', 1, 1);
COMMIT;

-- 5. Risultati attesi: physical_rows = 15, distinct_rows = 9.
SELECT COUNT(*) AS physical_rows FROM MapDB.provaC;
SELECT COUNT(*) AS distinct_rows
FROM (SELECT DISTINCT X, Y, C FROM MapDB.provaC) AS distinct_provaC;
