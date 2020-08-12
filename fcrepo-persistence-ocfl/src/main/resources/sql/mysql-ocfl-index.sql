-- DDL for setting up the table that maps Fedora IDs to OCFL IDs
-- MySQL 8 will only supports varchar up to 503 characters

-- Maps FedoraID to OCFL ID
CREATE TABLE IF NOT EXISTS ocfl_id_map (
    fedora_id varchar(503) NOT NULL PRIMARY KEY,
    fedora_root_id varchar(503) NOT NULL,
    ocfl_id varchar(503) NOT NULL
);

-- Holds operations to add or delete mappings from the ocfl_id_map table.
CREATE TABLE IF NOT EXISTS ocfl_id_map_session_operations (
    fedora_id varchar(503) NOT NULL,
    fedora_root_id varchar(503) NULL,
    ocfl_id varchar(503) NULL,
    session_id varchar(255) NOT NULL,
    operation varchar(10) NOT NULL
);

-- Create an index to speed searches for records related to adding/excluding transaction records
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'ocfl_id_map_session_operations' AND index_name = 'ocfl_id_map_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX ocfl_id_map_idx1 ON ocfl_id_map_session_operations (fedora_id, session_id, operation)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Create an index to speed finding records related to a transaction.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'ocfl_id_map_session_operations' AND index_name = 'ocfl_id_map_idx2' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE UNIQUE INDEX ocfl_id_map_idx2 ON ocfl_id_map_session_operations (fedora_id, session_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

-- Index on session_id to avoid deadlocks
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'ocfl_id_map_session_operations' AND index_name = 'ocfl_id_map_idx3' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX ocfl_id_map_idx3 ON ocfl_id_map_session_operations (session_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
