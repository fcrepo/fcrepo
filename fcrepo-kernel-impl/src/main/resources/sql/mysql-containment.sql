-- DDL for setting up containment tables in MySQL 8
-- MySQL 8 will only supports varchar up to 503 characters

-- Holds the ID and its parent.
CREATE TABLE IF NOT EXISTS resources (
    fedora_id  varchar(503) NOT NULL PRIMARY KEY,
    parent varchar(503) NOT NULL,
    is_deleted boolean NOT NULL DEFAULT(FALSE)
);

-- Create an index to speed searches for children of a parent.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'resources' AND index_name = 'resources_idx' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX resources_idx ON resources (parent, is_deleted)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Holds operations to add or delete records from the RESOURCES_TABLE.
CREATE TABLE IF NOT EXISTS transaction_operations (
    fedora_id varchar(503) NOT NULL,
    parent varchar(503) NOT NULL,
    transaction_id varchar(255) NOT NULL,
    operation varchar(10) NOT NULL
);

-- Create an index to speed searches for records related to adding/excluding transaction records
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'transaction_operations' AND index_name = 'transaction_operations_idx1'
    AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX transaction_operations_idx1 ON transaction_operations (parent, transaction_id, operation)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed finding records related to a transaction.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'transaction_operations' AND index_name = 'transaction_operations_idx2'
    AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX transaction_operations_idx2 ON transaction_operations (transaction_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

