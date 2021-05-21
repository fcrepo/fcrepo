-- simple search
CREATE TABLE IF NOT EXISTS simple_search (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    fedora_id  varchar(503) NOT NULL,
    created timestamp NOT NULL,
    modified timestamp NOT NULL,
    content_size bigint DEFAULT NULL,
    mime_type varchar(255) DEFAULT NULL,
    UNIQUE KEY fedora_id (fedora_id)
);

CREATE TABLE IF NOT EXISTS simple_search_transactions (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    fedora_id  varchar(503) NOT NULL,
    created timestamp NOT NULL,
    modified timestamp NOT NULL,
    content_size bigint DEFAULT NULL,
    mime_type varchar(255) DEFAULT NULL,
    transaction_id varchar(37) NOT NULL,
    operation varchar(10) NOT NULL,
    UNIQUE (fedora_id, transaction_id)
);

CREATE TABLE IF NOT EXISTS search_rdf_type (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    rdf_type_uri varchar(228) NOT NULL,
    UNIQUE KEY rdf_type_uri (rdf_type_uri)
);

CREATE TABLE IF NOT EXISTS search_resource_rdf_type (
    resource_id bigint NOT NULL,
    rdf_type_id bigint NOT NULL,
    UNIQUE (resource_id, rdf_type_id),
    FOREIGN KEY (resource_id) REFERENCES simple_search(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS search_resource_rdf_type_transactions (
    fedora_id  varchar(503) NOT NULL,
    rdf_type_uri varchar(228) NOT NULL,
    transaction_id varchar(37) NOT NULL,
    PRIMARY KEY(fedora_id, rdf_type_uri, transaction_id)
);

CREATE INDEX IF NOT EXISTS search_resource_rdf_type_transactions_idx1
    ON search_resource_rdf_type_transactions (fedora_id, transaction_id);

CREATE INDEX IF NOT EXISTS simple_search_idx1
    ON simple_search (created);

CREATE INDEX IF NOT EXISTS simple_search_idx2
    ON simple_search (modified);

CREATE INDEX IF NOT EXISTS simple_search_idx3
    ON simple_search (content_size);

CREATE INDEX IF NOT EXISTS simple_search_idx4
    ON simple_search (mime_type);


