-- Agrega Banco de Prueba
INSERT INTO banks (id, name, total_of_transactions) VALUES (1, 'Banco de Prueba', 0);

-- Actualiza el valor del autoincremento para la tabla "accounts"
ALTER TABLE banks ALTER COLUMN id RESTART WITH 2;

-- Agrega Cuentas de Prueba
INSERT INTO accounts (id, account_number, owner, balance) VALUES (1, '123456', 'Juan Perez', 1000);
INSERT INTO accounts (id, account_number, owner, balance) VALUES (2, '654321', 'Maria Lopez', 2000);

-- Actualiza el valor del autoincremento para la tabla "accounts"
ALTER TABLE accounts ALTER COLUMN id RESTART WITH 3;

