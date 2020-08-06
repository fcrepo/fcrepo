-- simple search
CREATE TABLE IF NOT EXISTS simple_search (
    id bigserial PRIMARY KEY,
    fedora_id  varchar(503) NOT NULL UNIQUE,
    created timestamp NOT NULL,
    modified timestamp NOT NULL,
    content_size bigint DEFAULT NULL,
    mime_type varchar(255) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS search_rdf_type (
    id bigserial PRIMARY KEY,
    rdf_type_uri varchar(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS search_resource_rdf_type (
    resource_id bigint NOT NULL,
    rdf_type_id bigint NOT NULL,
    PRIMARY KEY(resource_id, rdf_type_id),
    FOREIGN KEY (resource_id) REFERENCES simple_search(id) ON DELETE CASCADE,
    FOREIGN KEY (rdf_type_id) REFERENCES search_rdf_type(id)  ON DELETE CASCADE
);


