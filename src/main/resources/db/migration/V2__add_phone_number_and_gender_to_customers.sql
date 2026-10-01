ALTER TABLE customers
    ADD COLUMN phone_number VARCHAR(20) UNIQUE,
    ADD COLUMN gender VARCHAR(20);