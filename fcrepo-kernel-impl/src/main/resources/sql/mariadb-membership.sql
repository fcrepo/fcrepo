-- DDL for setting up membership tables inMariaDB 10.5

-- Non-transaction state of membership properties.
CREATE TABLE IF NOT EXISTS membership (
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    object_id varchar(503) NOT NULL,
    source_id varchar(503) NOT NULL,
    start_time datetime,
    end_time datetime
);

-- Create an index to speed searches for a resource.
CREATE INDEX IF NOT EXISTS membership_idx1
    ON membership (subject_id);

-- Create an index to speed searches for subject of membership.
CREATE INDEX IF NOT EXISTS membership_idx2
    ON membership (source_id);

CREATE INDEX IF NOT EXISTS membership_idx3
    ON membership (property);
    
CREATE INDEX IF NOT EXISTS membership_idx4
    ON membership (end_time);

-- Holds operations to add or delete records from the REFERENCE table.
CREATE TABLE IF NOT EXISTS membership_tx_operations (
    subject_id varchar(503) NOT NULL,
    property varchar(503) NOT NULL,
    object_id varchar(503) NOT NULL,
    source_id varchar(503) NOT NULL,
    start_time datetime,
    end_time datetime,
    tx_id varchar(36) NOT NULL,
    operation varchar(10) NOT NULL,
    force_flag varchar(10)
);

-- Create an index to speed searches for records targeting a resource to adding/excluding transaction records
CREATE INDEX IF NOT EXISTS membership_tx_operations_idx1
    ON membership_tx_operations (subject_id, tx_id, operation);

-- Create an index to speed finding records related to a transaction.
CREATE INDEX IF NOT EXISTS membership_tx_operations_idx2
    ON membership_tx_operations (tx_id);

CREATE INDEX IF NOT EXISTS membership_tx_operations_idx3
    ON membership_tx_operations (property);
    
CREATE INDEX IF NOT EXISTS membership_tx_operations_idx4
    ON membership_tx_operations (end_time);