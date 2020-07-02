-- DDL for setting up containment tables in H2, MariaDB 10.5, and PostgreSQL 12.3
-- MySQL 8 will only supports varchar up to 503 characters

-- Holds the ID and its parent.
CREATE TABLE IF NOT EXISTS resources (
    fedora_id varchar(503) NOT NULL PRIMARY KEY,
    parent varchar(503) NOT NULL,
    is_deleted boolean NOT NULL DEFAULT(FALSE)
);

-- Create an index to speed searches for children of a parent.
CREATE INDEX IF NOT EXISTS resources_idx
    ON resources (parent, is_deleted);

-- Holds operations to add or delete records from the RESOURCES_TABLE.
CREATE TABLE IF NOT EXISTS transaction_operations (
    fedora_id varchar(503) NOT NULL,
    parent varchar(503) NOT NULL,
    transaction_id varchar(255) NOT NULL,
    operation varchar(10) NOT NULL
);

-- Create an index to speed searches for records related to adding/excluding transaction records
CREATE INDEX IF NOT EXISTS transaction_operations_idx1
    ON transaction_operations (parent, transaction_id, operation);

-- Create an index to speed finding records related to a transaction.
CREATE INDEX IF NOT EXISTS transaction_operations_idx2
    ON transaction_operations (transaction_id);
