-- DDL for setting up reference tables in H2, MariaDB 10.5, and PostgreSQL 12.3
-- MySQL 8 will only supports varchar up to 503 characters

-- Holds the ID and the item it references.
CREATE TABLE IF NOT EXISTS reference (
    fedora_id varchar(503) NOT NULL,
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    target_id varchar(503) NOT NULL
);

-- Create an index to speed searches for a resource.
CREATE INDEX IF NOT EXISTS reference_idx1
    ON reference (fedora_id);

-- Create an index to speed searches for the subject of a reference.
CREATE INDEX IF NOT EXISTS reference_idx2
    ON reference (subject_id);

-- Create an index to speed searches for target of a reference.
CREATE INDEX IF NOT EXISTS reference_idx3
    ON reference (target_id);

-- Holds operations to add or delete records from the REFERENCE table.
CREATE TABLE IF NOT EXISTS reference_transaction_operations (
    fedora_id varchar(503) NOT NULL,
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    target_id varchar(503) NOT NULL,
    transaction_id varchar(255) NOT NULL,
    operation varchar(10) NOT NULL
);

-- Create an index to speed searches for records targeting a resource to adding/excluding transaction records
CREATE INDEX IF NOT EXISTS reference_transaction_operations_idx1
    ON reference_transaction_operations (target_id, transaction_id, operation);

-- Create an index to speed finding records related to a transaction.
CREATE INDEX IF NOT EXISTS reference_transaction_operations_idx2
    ON reference_transaction_operations (transaction_id);