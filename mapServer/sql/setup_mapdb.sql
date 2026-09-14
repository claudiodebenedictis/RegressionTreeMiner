-- Regression Tree Miner - preparazione MySQL per MAP6
--
-- ATTENZIONE: questo script elimina e ricrea esclusivamente MapDB.provaC.
-- Eseguirlo con un account MySQL autorizzato a creare database e utenti.

CREATE DATABASE IF NOT EXISTS MapDB;

CREATE USER IF NOT EXISTS 'MapUser'@'localhost' IDENTIFIED BY 'map';
ALTER USER 'MapUser'@'localhost' IDENTIFIED BY 'map';
GRANT SELECT ON MapDB.* TO 'MapUser'@'localhost';

DROP TABLE IF EXISTS MapDB.provaC;

CREATE TABLE MapDB.provaC (
    X VARCHAR(10),
    Y FLOAT(5,2),
    C FLOAT(5,2)
);

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

SELECT COUNT(*) AS physical_rows
FROM MapDB.provaC;

SELECT COUNT(*) AS distinct_rows
FROM (
    SELECT DISTINCT X, Y, C
    FROM MapDB.provaC
) AS distinct_provaC;
