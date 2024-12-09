-- Limpia la Tabla banks y reiniciar autoincremento
DELETE FROM banks;
ALTER TABLE banks ALTER COLUMN id RESTART WITH 1;

-- Limpia la Tabla banks y reiniciar autoincremento
DELETE FROM accounts;
ALTER TABLE accounts ALTER COLUMN id RESTART WITH 1;
