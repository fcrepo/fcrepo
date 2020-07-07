-- simple search
CREATE TABLE IF NOT EXISTS simple_search (
    fedora_id  varchar(503) NOT NULL PRIMARY KEY,
    created timestamp NOT NULL,
    modified timestamp NOT NULL,
    content_size bigint DEFAULT NULL,
    mime_type varchar(255) DEFAULT NULL
);