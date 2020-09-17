-- DDL for setting up containment tables in MySQL 8
-- MySQL 8 will only supports varchar up to 503 characters

-- Holds the ID and the item it references.
CREATE TABLE IF NOT EXISTS reference (
    fedora_id varchar(503) NOT NULL,
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    target_id varchar(503) NOT NULL
);

-- Create an index to speed searches for a resource.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'reference' AND index_name = 'reference_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX reference_idx1 ON reference (fedora_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed searches for the subject of a reference.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'reference' AND index_name = 'reference_idx2' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX reference_idx2 ON reference (subject_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed searches for target of a reference.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'reference' AND index_name = 'reference_idx3' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX reference_idx3 ON reference (target_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Holds operations to add or delete records from the REFERENCE table.
CREATE TABLE IF NOT EXISTS reference_transaction_operations (
    fedora_id varchar(503) NOT NULL,
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    target_id varchar(503) NOT NULL,
    transaction_id varchar(255) NOT NULL,
    operation varchar(10) NOT NULL
);

-- Create an index to speed searches for records related to adding/excluding transaction records
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'reference_transaction_operations' AND index_name = 'reference_transaction_operations_idx1' AND
    table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX reference_transaction_operations_idx1 ON reference_transaction_operations (target_id, transaction_id, operation)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed finding records related to a transaction.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'reference_transaction_operations' AND index_name = 'reference_transaction_operations_idx2' AND
    table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX reference_transaction_operations_idx2 ON reference_transaction_operations (transaction_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
