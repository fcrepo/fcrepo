-- DDL for setting up membership tables in MySQL 8

-- Non-transaction state of membership properties.
CREATE TABLE IF NOT EXISTS membership (
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    object_id varchar(503) NOT NULL,
    source_id varchar(503) NOT NULL,
    start_time datetime,
    end_time datetime,
    last_updated datetime
);

-- Create an index to speed searches for a resource.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx1 ON membership (subject_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx1a' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx1a ON membership (subject_id, end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx1b' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx1b ON membership (subject_id, start_time, end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed searches for subject of membership.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx2' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx2 ON membership (source_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx3' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx3 ON membership (property)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx4' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx4 ON membership (end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed retrieval of last_updated times
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx5' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx5 ON membership (subject_id, last_updated)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Holds operations to add or delete records from the REFERENCE table.
CREATE TABLE IF NOT EXISTS membership_tx_operations (
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    object_id varchar(503) NOT NULL,
    source_id varchar(503) NOT NULL,
    start_time datetime,
    end_time datetime,
    last_updated datetime,
    tx_id varchar(36) NOT NULL,
    operation varchar(10) NOT NULL,
    force_flag varchar(10)
);

-- Create an index to speed searches for records targeting a resource to adding/excluding transaction records
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership_tx_operations' AND index_name = 'membership_tx_operations_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_tx_operations_idx1 ON membership_tx_operations (subject_id, tx_id, operation)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership_tx_operations' AND index_name = 'membership_tx_operations_idx1a' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_tx_operations_idx1a ON membership_tx_operations (subject_id, tx_id, operation, end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed finding records related to a transaction.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership_tx_operations' AND index_name = 'membership_tx_operations_idx2' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_tx_operations_idx2 ON membership_tx_operations (tx_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership_tx_operations' AND index_name = 'membership_tx_operations_idx3' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_tx_operations_idx3 ON membership_tx_operations (property)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership_tx_operations' AND index_name = 'membership_tx_operations_idx4' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_tx_operations_idx4 ON membership_tx_operations (end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed retrieval of last_updated times
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership_tx_operations' AND index_name = 'membership_tx_operations_idx5' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_tx_operations_idx5 ON membership_tx_operations (subject_id, last_updated)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;