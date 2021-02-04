-- DDL for setting up containment tables in MySQL 8
-- MySQL 8 will only supports varchar up to 503 characters

-- Holds the ID and its parent.
CREATE TABLE IF NOT EXISTS containment (
    fedora_id varchar(503) PRIMARY KEY,
    parent varchar(503) NOT NULL,
    start_time datetime NOT NULL,
    end_time datetime NULL,
    updated datetime NULL
);

-- Create an index to speed searches for children of a parent.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'containment' AND index_name = 'containment_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX containment_idx1 ON containment (parent, end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'containment' AND index_name = 'containment_idx2' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX containment_idx2 ON containment (parent, start_time, end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'containment' AND index_name = 'containment_idx3' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX containment_idx3 ON containment (fedora_id, end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'containment' AND index_name = 'containment_idx4' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX containment_idx4 ON containment (fedora_id, start_time, end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

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
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'containment_transactions' AND index_name = 'containment_transactions_idx1'
    AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX containment_transactions_idx1 ON containment_transactions (parent, transaction_id, operation)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'containment_transactions' AND index_name = 'containment_transactions_idx2'
    AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX containment_transactions_idx2 ON containment_transactions (fedora_id, transaction_id, operation)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed finding records related to a transaction.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'containment_transactions' AND index_name = 'containment_transactions_idx3'
    AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX containment_transactions_idx3 ON containment_transactions (transaction_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
