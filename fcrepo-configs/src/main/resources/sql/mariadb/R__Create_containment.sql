-- DDL for setting up containment tables in H2, MariaDB 10.5, and PostgreSQL 12.3
-- MySQL 8 will only supports varchar up to 503 characters

-- Holds the ID and its parent.
CREATE TABLE IF NOT EXISTS containment (
    fedora_id varchar(503) NOT NULL PRIMARY KEY,
    parent varchar(503) NOT NULL,
    start_time datetime NOT NULL,
    end_time datetime NULL,
    updated datetime NULL
);

-- Create an index to speed searches for children of a parent.
CREATE INDEX IF NOT EXISTS containment_idx1
    ON containment (parent, end_time);

CREATE INDEX IF NOT EXISTS containment_idx2
    ON containment (parent, start_time, end_time);

CREATE INDEX IF NOT EXISTS containment_idx3
    ON containment (fedora_id, end_time);

CREATE INDEX IF NOT EXISTS containment_idx4
    ON containment (fedora_id, start_time, end_time);


-- Holds operations to add or delete records from the RESOURCES_TABLE.
CREATE TABLE IF NOT EXISTS containment_transactions (
    fedora_id varchar(503) NOT NULL,
    parent varchar(503) NOT NULL,
    start_time datetime NULL,
    end_time datetime NULL,
    transaction_id varchar(255) NOT NULL,
    operation varchar(10) NOT NULL
);

-- Create an index to speed searches for records related to adding/excluding transaction records
CREATE INDEX IF NOT EXISTS containment_transactions_idx1
    ON containment_transactions (parent, transaction_id, operation);

CREATE INDEX IF NOT EXISTS containment_transactions_idx2
    ON containment_transactions (fedora_id, transaction_id, operation);

-- Create an index to speed finding records related to a transaction.
CREATE INDEX IF NOT EXISTS containment_transactions_idx3
    ON containment_transactions (transaction_id);
