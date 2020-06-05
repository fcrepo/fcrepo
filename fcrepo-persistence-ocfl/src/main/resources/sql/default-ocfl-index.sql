-- DDL for setting up the table that maps Fedora IDs to OCFL IDs
-- MySQL 8 will only supports varchar up to 503 characters

-- Maps FedoarID to OCFL ID
CREATE TABLE IF NOT EXISTS ocfl_id_map (
    fedora_id varchar(503) NOT NULL PRIMARY KEY,
    fedora_root_id varchar(503) NOT NULL,
    ocfl_id varchar(503) NOT NULL
);
