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

-- Create an index to speed searches for Fedora IDs.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'simple_search' AND index_name = 'simple_search_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX simple_search_idx1 ON simple_search (fedora_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

CREATE TABLE IF NOT EXISTS search_rdf_type (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    rdf_type_uri varchar(255) NOT NULL,
    UNIQUE KEY rdf_type_uri (rdf_type_uri)
);

-- Create an index to speed searches for RDF types.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'search_rdf_type' AND index_name = 'search_rdf_type_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX search_rdf_type_idx1 ON search_rdf_type (rdf_type_uri)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

CREATE TABLE IF NOT EXISTS search_resource_rdf_type (
    resource_id bigint NOT NULL,
    rdf_type_id bigint NOT NULL,
    PRIMARY KEY(resource_id, rdf_type_id),
    FOREIGN KEY (resource_id) REFERENCES simple_search(id) ON DELETE CASCADE,
    FOREIGN KEY (rdf_type_id) REFERENCES search_rdf_type(id)  ON DELETE CASCADE
);

-- Create an index to speed searches for Resource IDs.
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'search_resource_rdf_type' AND
        index_name = 'search_resource_rdf_type_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX search_resource_rdf_type_idx1 ON search_resource_rdf_type (resource_id)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;


