-- DDL for setting up membership tables in MySQL 8

-- Non-transaction state of membership properties.
CREATE TABLE membership (
    id integer UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    object_id varchar(503) NOT NULL,
    source_id varchar(503) NOT NULL,
    start_time timestamp,
    end_time timestamp
);

-- Create an index to speed searches for a resource.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx1 ON membership (subject_id)');
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
    'CREATE INDEX membership_idx3 ON membership (source_id, subject_id, property, object_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership' AND index_name = 'membership_idx4' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_idx4 ON membership (end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Holds operations to add or delete records from the REFERENCE table.
CREATE TABLE membership_tx_operations (
    id integer UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    object_id varchar(503) NOT NULL,
    source_id varchar(503) NOT NULL,
    start_time timestamp,
    end_time timestamp,
    tx_id varchar(255) NOT NULL,
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
    'CREATE INDEX membership_tx_operations_idx3 ON membership_tx_operations (source_id, subject_id, property, object_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'membership_tx_operations' AND index_name = 'membership_tx_operations_idx4' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX membership_tx_operations_idx4 ON membership_tx_operations (end_time)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;