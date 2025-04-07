CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    balance NUMERIC(12, 2) DEFAULT 0.00
);
INSERT INTO accounts(balance)
VALUES (1000.00),
    (500.00),
    (750.00);