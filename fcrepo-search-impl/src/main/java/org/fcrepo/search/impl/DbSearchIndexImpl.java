/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.impl;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.stream.Collectors.toList;
import static org.fcrepo.common.db.DbPlatform.POSTGRESQL;
import static org.fcrepo.common.db.DbPlatform.H2;
import static org.fcrepo.search.api.Condition.Field.CONTENT_SIZE;
import static org.fcrepo.search.api.Condition.Field.FEDORA_ID;
import static org.fcrepo.search.api.Condition.Field.MIME_TYPE;
import static org.fcrepo.search.api.Condition.Field.RDF_TYPE;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import com.google.common.collect.Sets;
import org.fcrepo.common.db.DbPlatform;
import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.models.ResourceHeaders;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.PaginationInfo;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * An implementation of the {@link SearchIndex}
 *
 * @author dbernstein
 * @author whikloj
 */
@Component("searchIndexImpl")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DbSearchIndexImpl implements SearchIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbSearchIndexImpl.class);

    private static final String TRANSACTION_ID_COLUMN = "transaction_id";
    private static final String SIMPLE_SEARCH_TABLE = "simple_search";
    private static final String SIMPLE_SEARCH_TRANSACTIONS_TABLE = "simple_search_transactions";
    private static final String SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE = "search_resource_rdf_type_transactions";
    public static final String SEARCH_RESOURCE_RDF_TYPE_TABLE = "search_resource_rdf_type";
    public static final String SEARCH_RDF_TYPE_TABLE = "search_rdf_type";

    private static final String FEDORA_ID_COLUMN = "fedora_id";
    private static final String MODIFIED_COLUMN = "modified";
    private static final String CREATED_COLUMN = "created";
    private static final String CONTENT_SIZE_COLUMN = "content_size";
    private static final String MIME_TYPE_COLUMN = "mime_type";
    private static final String RESOURCE_ID_COLUMN = "resource_id";
    public static final String RDF_TYPE_ID_COLUMN = "rdf_type_id";
    public static final String ID_COLUMN = "id";
    private static final String OPERATION_COLUMN = "operation";
    private static final String RDF_TYPE_URI_COLUMN = "rdf_type_uri";

    private static final String FEDORA_ID_PARAM = "fedora_id";
    private static final String RDF_TYPE_ID_PARAM = "rdf_type_id";
    private static final String MODIFIED_PARAM = "modified";
    private static final String CONTENT_SIZE_PARAM = "content_size";
    private static final String MIME_TYPE_PARAM = "mime_type";
    private static final String CREATED_PARAM = "created";
    public static final String RDF_TYPE_URI_PARAM = "rdf_type_uri";
    public static final String RESOURCE_SEARCH_ID_PARAM = "resource_search_id";

    public static final String TRANSACTION_ID_PARAM = "transaction_id";
    private static final String OPERATION_PARAM = "operation";

    private static final String POSTGRES_GROUP_CONCAT_FUNCTION = "STRING_AGG(b.rdf_type_uri, ',')";
    private static final String DEFAULT_GROUP_CONCAT_FUNCTION = "GROUP_CONCAT(distinct b.rdf_type_uri " +
            "ORDER BY b.rdf_type_uri ASC SEPARATOR ',')";

    private static final String UPSERT_SIMPLE_SEARCH_TRANSACTION_H2 =
            "MERGE INTO " + SIMPLE_SEARCH_TRANSACTIONS_TABLE + " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + "," +
                    FEDORA_ID_COLUMN + "," + OPERATION_COLUMN + ", " + TRANSACTION_ID_COLUMN +
                    ") KEY (" + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ") VALUES ( :" + MODIFIED_PARAM +
                    ", :" + CREATED_PARAM + ", :" + CONTENT_SIZE_PARAM + ", :" + MIME_TYPE_PARAM + "," +
                    ":" + FEDORA_ID_PARAM + ", :" + OPERATION_PARAM + ", :" + TRANSACTION_ID_PARAM + ")";

    private static final String UPSERT_SIMPLE_SEARCH_H2 =
            "MERGE INTO " + SIMPLE_SEARCH_TABLE + " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + "," +
                    FEDORA_ID_COLUMN + ") KEY (" + FEDORA_ID_COLUMN + ") VALUES ( :" + MODIFIED_PARAM +
                    ", :" + CREATED_PARAM + ", :" + CONTENT_SIZE_PARAM + ", :" + MIME_TYPE_PARAM + "," +
                    ":" + FEDORA_ID_PARAM + ")";

    private static final String UPSERT_SIMPLE_SEARCH_TRANSACTION_MYSQL_MARIA =
            "INSERT INTO " + SIMPLE_SEARCH_TRANSACTIONS_TABLE + " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + "," +
                    FEDORA_ID_COLUMN + "," + OPERATION_COLUMN + ", " + TRANSACTION_ID_COLUMN +
                    ")  VALUES ( :" + MODIFIED_PARAM + ", :" + CREATED_PARAM + ", :" + CONTENT_SIZE_PARAM +
                    ", :" + MIME_TYPE_PARAM + "," + ":" + FEDORA_ID_PARAM + ", :" + OPERATION_PARAM +
                    ", :" + TRANSACTION_ID_PARAM + ") ON DUPLICATE KEY " +
                    "UPDATE " + MODIFIED_COLUMN + " = VALUES(" + MODIFIED_COLUMN + "), " +
                    CREATED_COLUMN + "= VALUES(" + CREATED_COLUMN + ")," +
                    CONTENT_SIZE_COLUMN + "= VALUES(" + CONTENT_SIZE_COLUMN + ")," +
                    MIME_TYPE_COLUMN + "= VALUES(" + MIME_TYPE_COLUMN + ")," +
                    OPERATION_COLUMN + "= VALUES(" + OPERATION_COLUMN + ")";

    private static final String UPSERT_SIMPLE_SEARCH_MYSQL =
            "INSERT INTO " + SIMPLE_SEARCH_TABLE + " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + "," +
                    FEDORA_ID_COLUMN + ")  VALUES ( :" + MODIFIED_PARAM + ", :" + CREATED_PARAM +
                    ", :" + CONTENT_SIZE_PARAM + ", :" + MIME_TYPE_PARAM + "," + ":" + FEDORA_ID_PARAM + ") " +
                    "ON DUPLICATE KEY UPDATE " + MODIFIED_COLUMN + " = VALUES(" + MODIFIED_COLUMN + "), " +
                    CREATED_COLUMN + "= VALUES(" + CREATED_COLUMN + ")," +
                    CONTENT_SIZE_COLUMN + "= VALUES(" + CONTENT_SIZE_COLUMN + ")," +
                    MIME_TYPE_COLUMN + "= VALUES(" + MIME_TYPE_COLUMN + ")";

    private static final String UPSERT_SIMPLE_SEARCH_MARIA =
            "INSERT INTO " + SIMPLE_SEARCH_TABLE + " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + "," +
                    FEDORA_ID_COLUMN + ")  VALUES ( :" + MODIFIED_PARAM + ", :" + CREATED_PARAM +
                    ", :" + CONTENT_SIZE_PARAM + ", :" + MIME_TYPE_PARAM + "," + ":" + FEDORA_ID_PARAM + ") " +
                    "ON DUPLICATE KEY UPDATE " + MODIFIED_COLUMN + " = VALUES(" + MODIFIED_COLUMN + "), " +
                    CREATED_COLUMN + "= VALUES(" + CREATED_COLUMN + ")," +
                    CONTENT_SIZE_COLUMN + "= VALUES(" + CONTENT_SIZE_COLUMN + ")," +
                    MIME_TYPE_COLUMN + "= VALUES(" + MIME_TYPE_COLUMN + ")" +
                    " RETURNING " + ID_COLUMN;

    private static final String UPSERT_SIMPLE_SEARCH_TRANSACTION_POSTGRESQL =
            "INSERT INTO " + SIMPLE_SEARCH_TRANSACTIONS_TABLE + " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + "," +
                    FEDORA_ID_COLUMN + "," + OPERATION_COLUMN + ", " + TRANSACTION_ID_COLUMN +
                    ")  VALUES ( :" + MODIFIED_PARAM +
                    ", :" + CREATED_PARAM + ", :" + CONTENT_SIZE_PARAM + ", :" + MIME_TYPE_PARAM + "," +
                    ":" + FEDORA_ID_PARAM + ", :" + OPERATION_PARAM + ", :" + TRANSACTION_ID_PARAM + ") ON CONFLICT " +
                    "( " + FEDORA_ID_COLUMN + ", " + TRANSACTION_ID_COLUMN + ") " +
                    "DO UPDATE SET " + MODIFIED_COLUMN + " = EXCLUDED." + MODIFIED_COLUMN + ", " +
                    CREATED_COLUMN + " = EXCLUDED." + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + " = EXCLUDED." + CONTENT_SIZE_COLUMN + ", " +
                    MIME_TYPE_COLUMN + " = EXCLUDED." + MIME_TYPE_COLUMN + ", " +
                    OPERATION_COLUMN + " = EXCLUDED." + OPERATION_COLUMN;

    private static final String UPSERT_SIMPLE_SEARCH_POSTGRESQL =
            "INSERT INTO " + SIMPLE_SEARCH_TABLE + " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + "," +
                    FEDORA_ID_COLUMN + ")  VALUES ( :" + MODIFIED_PARAM +
                    ", :" + CREATED_PARAM + ", :" + CONTENT_SIZE_PARAM + ", :" + MIME_TYPE_PARAM + "," +
                    ":" + FEDORA_ID_PARAM + ") ON CONFLICT ( " + FEDORA_ID_COLUMN + ") " +
                    "DO UPDATE SET " + MODIFIED_COLUMN + " = EXCLUDED." + MODIFIED_COLUMN + ", " +
                    CREATED_COLUMN + " = EXCLUDED." + CREATED_COLUMN + ", " +
                    CONTENT_SIZE_COLUMN + " = EXCLUDED." + CONTENT_SIZE_COLUMN + ", " +
                    MIME_TYPE_COLUMN + " = EXCLUDED." + MIME_TYPE_COLUMN +
                    " RETURNING " + ID_COLUMN;

    private static final String UPSERT_COMMIT_SIMPLE_SEARCH_H2 =
            "MERGE INTO " + SIMPLE_SEARCH_TABLE +
                    " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " + CONTENT_SIZE_COLUMN + "," +
                    MIME_TYPE_COLUMN + ", " + FEDORA_ID_COLUMN +
                    ") KEY (" + FEDORA_ID_COLUMN + ") SELECT " + MODIFIED_COLUMN + ", " + CREATED_COLUMN +
                    ", " + CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + ", " + FEDORA_ID_COLUMN + " FROM " +
                    SIMPLE_SEARCH_TRANSACTIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + "= :" +
                    TRANSACTION_ID_PARAM + " AND " + OPERATION_COLUMN + "='add'";

    private static final String UPSERT_COMMIT_SIMPLE_SEARCH_MYSQL_MARIA = "INSERT INTO " + SIMPLE_SEARCH_TABLE +
            " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " + CONTENT_SIZE_COLUMN + "," +
            MIME_TYPE_COLUMN + ", " + FEDORA_ID_COLUMN +
            ") SELECT " + MODIFIED_COLUMN + ", " + CREATED_COLUMN +
            ", " + CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + ", " + FEDORA_ID_COLUMN + " FROM " +
            SIMPLE_SEARCH_TRANSACTIONS_TABLE + " a WHERE " + TRANSACTION_ID_COLUMN + "= :" +
            TRANSACTION_ID_PARAM + " AND " + OPERATION_COLUMN + "='add' " +
            "ON DUPLICATE KEY UPDATE " + MODIFIED_COLUMN + " = a." + MODIFIED_COLUMN + ", " +
            CREATED_COLUMN + " = a." + CREATED_COLUMN + ", " +
            CONTENT_SIZE_COLUMN + " = a." + CONTENT_SIZE_COLUMN + ", " +
            MIME_TYPE_COLUMN + " = a." + MIME_TYPE_COLUMN;

    private static final String UPSERT_COMMIT_SIMPLE_SEARCH_POSTGRESQL = "INSERT INTO " + SIMPLE_SEARCH_TABLE +
            " (" + MODIFIED_COLUMN + "," + CREATED_COLUMN + ", " + CONTENT_SIZE_COLUMN + "," +
            MIME_TYPE_COLUMN + ", " + FEDORA_ID_COLUMN +
            ") SELECT " + MODIFIED_COLUMN + ", " + CREATED_COLUMN +
            ", " + CONTENT_SIZE_COLUMN + "," + MIME_TYPE_COLUMN + ", " + FEDORA_ID_COLUMN + " FROM " +
            SIMPLE_SEARCH_TRANSACTIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + "= :" +
            TRANSACTION_ID_PARAM + " AND " + OPERATION_COLUMN + "='add' ON CONFLICT (" + FEDORA_ID_COLUMN + ") " +
            "DO UPDATE SET " + MODIFIED_COLUMN + " = EXCLUDED." + MODIFIED_COLUMN + ", " +
            CREATED_COLUMN + " = EXCLUDED." + CREATED_COLUMN + ", " +
            CONTENT_SIZE_COLUMN + " = EXCLUDED." + CONTENT_SIZE_COLUMN + ", " +
            MIME_TYPE_COLUMN + " = EXCLUDED." + MIME_TYPE_COLUMN;

    private static final String COMMIT_RDF_TYPES_MYSQL_MARIA =
            "INSERT IGNORE INTO " + SEARCH_RDF_TYPE_TABLE + " (" + RDF_TYPE_URI_COLUMN + ") " +
                    "SELECT DISTINCT " + RDF_TYPE_URI_COLUMN + " FROM " + SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE +
                    " WHERE " + TRANSACTION_ID_COLUMN + " = :" + TRANSACTION_ID_PARAM;
    private static final String COMMIT_RDF_TYPES_POSTGRES =
            "INSERT INTO " + SEARCH_RDF_TYPE_TABLE + " (" + RDF_TYPE_URI_COLUMN + ")" +
                    "SELECT DISTINCT " + RDF_TYPE_URI_COLUMN + " FROM " + SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE +
                    " WHERE " + TRANSACTION_ID_COLUMN + " = :" + TRANSACTION_ID_PARAM + " ON CONFLICT " +
                    "(" + RDF_TYPE_URI_COLUMN + ") DO NOTHING";
    private static final String COMMIT_RDF_TYPES_H2 =
            "INSERT INTO " + SEARCH_RDF_TYPE_TABLE + " (" + RDF_TYPE_URI_COLUMN + ") " +
                    "SELECT DISTINCT tx." + RDF_TYPE_URI_COLUMN + " " +
                    "FROM " + SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE + " tx " +
                    "WHERE tx." + TRANSACTION_ID_COLUMN + " = :" + TRANSACTION_ID_PARAM + " " +
                    "AND NOT EXISTS ( " +
                    "  SELECT 1 FROM " + SEARCH_RDF_TYPE_TABLE + " existing " +
                    "  WHERE existing." + RDF_TYPE_URI_COLUMN + " = tx." + RDF_TYPE_URI_COLUMN +
                    ")";

    private static final Map<DbPlatform, String> COMMIT_RDF_TYPES_MAP = Map.of(
            DbPlatform.H2, COMMIT_RDF_TYPES_H2,
            DbPlatform.MYSQL, COMMIT_RDF_TYPES_MYSQL_MARIA,
            DbPlatform.MARIADB, COMMIT_RDF_TYPES_MYSQL_MARIA,
            DbPlatform.POSTGRESQL, COMMIT_RDF_TYPES_POSTGRES
    );

    private static final String INSERT_RDF_TYPE_H2 =
            "INSERT INTO " + SEARCH_RDF_TYPE_TABLE + " (" + RDF_TYPE_URI_COLUMN + ")" +
                    " VALUES (:" + RDF_TYPE_URI_PARAM + ")";

    // postgres spoils the entire tx on duplicate keys
    private static final String INSERT_RDF_TYPE_POSTGRES =
            "INSERT INTO " + SEARCH_RDF_TYPE_TABLE + " (" + RDF_TYPE_URI_COLUMN + ")" +
                    " VALUES (:" + RDF_TYPE_URI_PARAM + ")" +
                    " ON CONFLICT (" + RDF_TYPE_URI_COLUMN + ") DO NOTHING";

    // MySQL and MariaDB use INSERT IGNORE to avoid duplicate key errors
    private static final String INSERT_RDF_TYPE_MYSQL_MARIA =
            "INSERT IGNORE INTO " + SEARCH_RDF_TYPE_TABLE + " (" + RDF_TYPE_URI_COLUMN + ")" +
                    " VALUES (:" + RDF_TYPE_URI_PARAM + ")";

    private static final Map<DbPlatform, String> INSERT_RDF_TYPE_MAPPING = Map.of(
            H2, INSERT_RDF_TYPE_H2,
            DbPlatform.MYSQL, INSERT_RDF_TYPE_MYSQL_MARIA,
            DbPlatform.MARIADB, INSERT_RDF_TYPE_MYSQL_MARIA,
            POSTGRESQL, INSERT_RDF_TYPE_POSTGRES
    );

    private static final String COMMIT_RDF_TYPE_ASSOCIATIONS =
            "INSERT INTO " + SEARCH_RESOURCE_RDF_TYPE_TABLE +
                    " (" + RESOURCE_ID_COLUMN + "," + RDF_TYPE_ID_COLUMN + ")" +
                    " SELECT a." + ID_COLUMN + ", b." + ID_COLUMN + " FROM " + SIMPLE_SEARCH_TABLE + " a, " +
                    SEARCH_RDF_TYPE_TABLE + " b, " + SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE + " c WHERE c." +
                    TRANSACTION_ID_COLUMN + "= :" + TRANSACTION_ID_PARAM + " AND b." + RDF_TYPE_URI_COLUMN +
                    "= c." + RDF_TYPE_URI_COLUMN + " AND c." + FEDORA_ID_COLUMN + " = a." + FEDORA_ID_COLUMN +
                    " GROUP BY a." + ID_COLUMN + ", b." + ID_COLUMN;

    private static final String COMMIT_DELETE_RESOURCES_IN_TRANSACTION_MYSQL_MARIA =
            "DELETE s FROM " + SIMPLE_SEARCH_TABLE + " s JOIN " + SIMPLE_SEARCH_TRANSACTIONS_TABLE + " tx ON s." +
                    FEDORA_ID_COLUMN + " = tx." + FEDORA_ID_COLUMN + " WHERE tx." + TRANSACTION_ID_COLUMN +
                    " = :" + TRANSACTION_ID_PARAM + " AND tx." + OPERATION_COLUMN + " = 'delete'";
    private static final String COMMIT_DELETE_RESOURCES_IN_TRANSACTION_POSTGRES =
            "DELETE FROM " + SIMPLE_SEARCH_TABLE + " s " +
                    "USING " + SIMPLE_SEARCH_TRANSACTIONS_TABLE + " tx " +
                    "WHERE s." + FEDORA_ID_COLUMN + " = tx." + FEDORA_ID_COLUMN + " " +
                    "AND tx." + TRANSACTION_ID_COLUMN + " = :" + TRANSACTION_ID_PARAM + " " +
                    "AND tx." + OPERATION_COLUMN + " = 'delete'";
    private static final String COMMIT_DELETE_RESOURCES_IN_TRANSACTION_H2 =
            "DELETE FROM " + SIMPLE_SEARCH_TABLE + " WHERE " + FEDORA_ID_COLUMN + " IN (" +
                    "SELECT " + FEDORA_ID_COLUMN + " FROM " + SIMPLE_SEARCH_TRANSACTIONS_TABLE +
                    " WHERE " + TRANSACTION_ID_COLUMN + " = :" + TRANSACTION_ID_PARAM +
                    " AND " + OPERATION_COLUMN + " = 'delete')";
    private static final Map<DbPlatform, String> COMMIT_DELETE_RESOURCES_IN_TRANSACTION_MAP = Map.of(
            DbPlatform.H2, COMMIT_DELETE_RESOURCES_IN_TRANSACTION_H2,
            DbPlatform.MYSQL, COMMIT_DELETE_RESOURCES_IN_TRANSACTION_MYSQL_MARIA,
            DbPlatform.MARIADB, COMMIT_DELETE_RESOURCES_IN_TRANSACTION_MYSQL_MARIA,
            DbPlatform.POSTGRESQL, COMMIT_DELETE_RESOURCES_IN_TRANSACTION_POSTGRES
    );

    private static final String COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_MYSQL_MARIA =
            "DELETE s FROM " + SEARCH_RESOURCE_RDF_TYPE_TABLE + " s JOIN " + SIMPLE_SEARCH_TABLE + " a ON s." +
                    RESOURCE_ID_COLUMN + " = a." + ID_COLUMN + " JOIN " + SIMPLE_SEARCH_TRANSACTIONS_TABLE +
                    " b ON a." + FEDORA_ID_COLUMN + "= b." + FEDORA_ID_COLUMN + " WHERE b." +
                    TRANSACTION_ID_COLUMN + "= :" + TRANSACTION_ID_PARAM;
    private static final String COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_POSTGRES =
            "DELETE FROM " + SEARCH_RESOURCE_RDF_TYPE_TABLE + " s " +
                    "USING " + SIMPLE_SEARCH_TABLE + " a, " + SIMPLE_SEARCH_TRANSACTIONS_TABLE + " b " +
                    "WHERE s." + RESOURCE_ID_COLUMN + " = a." + ID_COLUMN + " " +
                    "AND a." + FEDORA_ID_COLUMN + " = b." + FEDORA_ID_COLUMN + " " +
                    "AND b." + TRANSACTION_ID_COLUMN + " = :" + TRANSACTION_ID_PARAM;
    private static final String COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_H2 =
            "DELETE FROM " + SEARCH_RESOURCE_RDF_TYPE_TABLE + " s " +
                    "WHERE EXISTS ( " +
                    "SELECT 1 FROM " + SIMPLE_SEARCH_TABLE + " a " +
                    "JOIN " + SIMPLE_SEARCH_TRANSACTIONS_TABLE + " b " +
                    "ON a." + FEDORA_ID_COLUMN + " = b." + FEDORA_ID_COLUMN + " " +
                    "WHERE s." + RESOURCE_ID_COLUMN + " = a." + ID_COLUMN + " " +
                    "AND b." + TRANSACTION_ID_COLUMN + " = :" + TRANSACTION_ID_PARAM +
                    ")";

    private static final Map<DbPlatform, String> COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_MAP = Map.of(
            DbPlatform.H2, COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_H2,
            DbPlatform.MYSQL, COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_MYSQL_MARIA,
            DbPlatform.MARIADB, COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_MYSQL_MARIA,
            DbPlatform.POSTGRESQL, COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_POSTGRES
    );

    private static final String DELETE_TRANSACTION =
            "DELETE FROM " + SIMPLE_SEARCH_TRANSACTIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :" +
                    TRANSACTION_ID_PARAM;

    private static final String DELETE_RESOURCE_FROM_SEARCH =
            "DELETE FROM " + SIMPLE_SEARCH_TABLE + " WHERE " + FEDORA_ID_COLUMN + " = :" +
                    FEDORA_ID_PARAM;

    private static final String DELETE_RDF_TYPE_ASSOCIATIONS_IN_TRANSACTION =
            "DELETE FROM " + SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE + " WHERE " + TRANSACTION_ID_COLUMN + " = :" +
                    TRANSACTION_ID_PARAM;


    private static final Map<DbPlatform, String> DIRECT_UPSERT_MAPPING = Map.of(
            H2, UPSERT_SIMPLE_SEARCH_H2,
            DbPlatform.MYSQL, UPSERT_SIMPLE_SEARCH_MYSQL,
            DbPlatform.MARIADB, UPSERT_SIMPLE_SEARCH_MARIA,
            POSTGRESQL, UPSERT_SIMPLE_SEARCH_POSTGRESQL
    );

    private static final Map<DbPlatform, String> TRANSACTION_UPSERT_MAPPING = Map.of(
            H2, UPSERT_SIMPLE_SEARCH_TRANSACTION_H2,
            DbPlatform.MYSQL, UPSERT_SIMPLE_SEARCH_TRANSACTION_MYSQL_MARIA,
            DbPlatform.MARIADB, UPSERT_SIMPLE_SEARCH_TRANSACTION_MYSQL_MARIA,
            POSTGRESQL, UPSERT_SIMPLE_SEARCH_TRANSACTION_POSTGRESQL
    );

    private static final Map<DbPlatform, String> UPSERT_COMMIT_MAPPING = Map.of(
            H2, UPSERT_COMMIT_SIMPLE_SEARCH_H2,
            DbPlatform.MYSQL, UPSERT_COMMIT_SIMPLE_SEARCH_MYSQL_MARIA,
            DbPlatform.MARIADB, UPSERT_COMMIT_SIMPLE_SEARCH_MYSQL_MARIA,
            POSTGRESQL, UPSERT_COMMIT_SIMPLE_SEARCH_POSTGRESQL
    );

    /*
     * Insert an association between a RDF type and a resource.
     */
    private static final String INSERT_RDF_TYPE_ASSOC_IN_TRANSACTION = "INSERT INTO " +
            SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE + " (" + FEDORA_ID_COLUMN + ", " + RDF_TYPE_URI_COLUMN + ", " +
            TRANSACTION_ID_COLUMN + ") VALUES (:" + FEDORA_ID_PARAM + ", :" + RDF_TYPE_URI_PARAM + ", :" +
            TRANSACTION_ID_PARAM + ")";

    private static final String SELECT_RESOURCE_SEARCH_ID = "SELECT " + ID_COLUMN + " FROM " + SIMPLE_SEARCH_TABLE +
            " WHERE " + FEDORA_ID_COLUMN + " = :" + FEDORA_ID_PARAM;

    private static final String SELECT_RDF_TYPE_ID = "SELECT " + ID_COLUMN + " FROM " + SEARCH_RDF_TYPE_TABLE +
            " WHERE " + RDF_TYPE_URI_COLUMN + "= :" + RDF_TYPE_URI_PARAM;

    private static final String INSERT_RDF_TYPE_ASSOC = "INSERT INTO " + SEARCH_RESOURCE_RDF_TYPE_TABLE +
            " (" + RESOURCE_ID_COLUMN + ", " + RDF_TYPE_ID_COLUMN + ")" +
            " VALUES (:" + RESOURCE_SEARCH_ID_PARAM + ", :" + RDF_TYPE_ID_PARAM + ")";

    private static final String DELETE_RESOURCE_TYPE_ASSOCIATIONS_IN_TRANSACTION =
            "DELETE FROM " + SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE + " WHERE " +
                    FEDORA_ID_COLUMN + "= :" + FEDORA_ID_PARAM + " AND " + TRANSACTION_ID_COLUMN + "= :" +
                    TRANSACTION_ID_PARAM;

    private static final String DELETE_RDF_TYPE_ASSOCIATIONS_MYSQL_MARIA =
            "DELETE s FROM " + SEARCH_RESOURCE_RDF_TYPE_TABLE + " s JOIN " + SIMPLE_SEARCH_TABLE + " a ON s." +
                    RESOURCE_ID_COLUMN + " = a." + ID_COLUMN + " WHERE a." + FEDORA_ID_COLUMN + "= :" +
                    FEDORA_ID_PARAM;
    private static final String DELETE_RDF_TYPE_ASSOCIATIONS_POSTGRES =
            "DELETE FROM " + SEARCH_RESOURCE_RDF_TYPE_TABLE + " s " +
                    "USING " + SIMPLE_SEARCH_TABLE + " a " +
                    "WHERE s." + RESOURCE_ID_COLUMN + " = a." + ID_COLUMN + " " +
                    "AND a." + FEDORA_ID_COLUMN + "= :" + FEDORA_ID_PARAM;
    private static final String DELETE_RDF_TYPE_ASSOCIATIONS_H2 =
            "DELETE FROM " + SEARCH_RESOURCE_RDF_TYPE_TABLE + " s " +
                    "WHERE EXISTS ( " +
                    "SELECT 1 FROM " + SIMPLE_SEARCH_TABLE + " a " +
                    "WHERE s." + RESOURCE_ID_COLUMN + " = a." + ID_COLUMN + " " +
                    "AND a." + FEDORA_ID_COLUMN + "= :" + FEDORA_ID_PARAM +
                    ")";
    private static final Map<DbPlatform, String> DELETE_RDF_TYPE_ASSOCIATIONS_MAP = Map.of(
            DbPlatform.H2, DELETE_RDF_TYPE_ASSOCIATIONS_H2,
            DbPlatform.MYSQL, DELETE_RDF_TYPE_ASSOCIATIONS_MYSQL_MARIA,
            DbPlatform.MARIADB, DELETE_RDF_TYPE_ASSOCIATIONS_MYSQL_MARIA,
            DbPlatform.POSTGRESQL, DELETE_RDF_TYPE_ASSOCIATIONS_POSTGRES
    );

    private static final List<String> COUNT_QUERY_COLUMNS = List.of("count(0) as count");

    @Inject
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private RepositoryInitializationStatus initializationStatus;

    private DbPlatform dbPlatForm;

    private final Map<URI, Long> rdfTypeIdCache;

    /**
     * Setup database table and connection
     */
    @PostConstruct
    public void setup() {
        this.dbPlatForm = DbPlatform.fromDataSource(this.dataSource);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(this.dataSource);
    }

    public DbSearchIndexImpl() {
        this.rdfTypeIdCache = new ConcurrentHashMap<>();
    }

    @Override
    public SearchResult doSearch(final SearchParameters parameters) throws InvalidQueryException {
        //translate parameters into a SQL query
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        final var fields = parameters.getFields().stream().map(Condition.Field::toString).collect(toList());
        final var selectQuery = createSearchQuery(parameters, parameterSource, fields, false);
        final RowMapper<Map<String, Object>> rowMapper = createRowMapper(fields);

        Integer totalResults = -1;
        if (parameters.isIncludeTotalResultCount()) {
            final var countQuery = createSearchQuery(parameters, parameterSource, Collections.emptyList(), true);
            LOGGER.debug("countQuery={}, parameterSource={}", countQuery, parameterSource);
            totalResults = jdbcTemplate.queryForObject(countQuery.toString(), parameterSource, Integer.class);
        }

        final var selectQueryStr = selectQuery.toString();
        LOGGER.debug("selectQueryStr={}, parameterSource={}", selectQueryStr, parameterSource);

        final List<Map<String, Object>> items = jdbcTemplate.query(selectQueryStr, parameterSource, rowMapper);
        final var pagination = new PaginationInfo(parameters.getMaxResults(), parameters.getOffset(),
                (totalResults != null ? totalResults : 0));
        LOGGER.debug("Search query with parameters: {} - {}", selectQuery, parameters);
        return new SearchResult(items, pagination);
    }

    private RowMapper<Map<String, Object>> createRowMapper(final List<String> fields) {
        return new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final Map<String, Object> map = new HashMap<>();
                for (final String fieldStr : fields) {
                    var value = rs.getObject(fieldStr);
                    if (value instanceof Timestamp) {
                        //format as iso instant if timestamp
                        value = ISO_INSTANT.format(Instant.ofEpochMilli(((Timestamp) value).getTime()));
                    } else if (fieldStr.equals(RDF_TYPE.toString())) {
                        //convert the comma-separate string to an array for rdf_type
                        value = value.toString().split(",");
                    }
                    map.put(fieldStr, value);
                }
                return map;
            }
        };
    }

    private StringBuilder createSearchQuery(final SearchParameters parameters,
                                            final MapSqlParameterSource parameterSource,
                                            final List<String> selectedFields, final boolean isCountQuery)
            throws InvalidQueryException {

        final List<String> queryFields = new ArrayList<>(selectedFields);
        final String fedoraIdStr = FEDORA_ID.toString();

        if (isCountQuery) {
            queryFields.clear();
            queryFields.add("count(0)");
        } else {
            if (!queryFields.contains(fedoraIdStr)) {
                queryFields.addFirst(fedoraIdStr);
            }
            queryFields.addFirst("id");
        }

        final List<String> whereClauses = new ArrayList<>();
        final List<Condition> conditions = parameters.getConditions();
        final boolean returnRdfType = queryFields.contains(RDF_TYPE.toString());
        final List<String> returnFields = queryFields.stream()
                .filter(x -> !x.equals(RDF_TYPE.toString())).collect(toList());

        final var sql = new StringBuilder("SELECT ")
                .append(String.join(",", returnFields))
                .append(" FROM ").append(SIMPLE_SEARCH_TABLE).append(" s ");

        conditions.stream().filter(c -> c.getField().equals(RDF_TYPE)).findFirst()
                .ifPresent(rdfTypeCondition -> {
                    final String rdfTypeOperator = rdfTypeCondition.getObject().contains("*") ? " LIKE " : " = ";
                    sql.append(" JOIN (SELECT srrt.").append(RESOURCE_ID_COLUMN).append(" FROM ")
                    .append(SEARCH_RESOURCE_RDF_TYPE_TABLE).append(" srrt JOIN ").append(SEARCH_RDF_TYPE_TABLE)
                            .append(" srt ON srrt.").append(RDF_TYPE_ID_PARAM).append(" = srt.")
                            .append(ID_COLUMN).append(" WHERE srt.").append(RDF_TYPE_URI_COLUMN).append(rdfTypeOperator)
                    .append(":").append(RDF_TYPE_URI_PARAM)
                            .append(") rdf_type_filter ON rdf_type_filter.resource_id = s.id");
                addRdfTypeParam(parameterSource, conditions);
        });

        // Add general conditions
        for (int i = 0; i < conditions.size(); i++) {
            addWhereClause(i, parameterSource, whereClauses, conditions.get(i));
        }

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", whereClauses));
        }

        if (!isCountQuery) {
            if (parameters.getOrderBy() != null) {
                sql.append(" ORDER BY ").append(parameters.getOrderBy())
                        .append(" ").append(parameters.getOrder());
            }
            sql.append(" LIMIT :limit OFFSET :offset");
            parameterSource.addValue("limit", parameters.getMaxResults());
            parameterSource.addValue("offset", parameters.getOffset());
        }

        if (!returnRdfType) {
            return sql;
        } else {
            final StringBuilder rdfTypeWrapperSql = new StringBuilder()
                    .append("SELECT a.*, ")
                    .append(isPostgres() ? POSTGRES_GROUP_CONCAT_FUNCTION : DEFAULT_GROUP_CONCAT_FUNCTION)
                    .append(" as rdf_type FROM (")
                    .append(sql).append(") a ")
                    .append("JOIN (SELECT rrt.resource_id , rt.rdf_type_uri FROM ")
                    .append(SEARCH_RESOURCE_RDF_TYPE_TABLE).append(" rrt, ")
                    .append(SEARCH_RDF_TYPE_TABLE)
                    .append(" rt WHERE rrt.rdf_type_id = rt.id) b ON a.id = b.resource_id GROUP BY ")
                    .append(String.join(", ", returnFields));

            if (parameters.getOrderBy() != null) {
                //add order by limit and offset to selectquery.
                rdfTypeWrapperSql.append(" ORDER BY ").append(parameters.getOrderBy()).append(" ")
                        .append(parameters.getOrder());
            }

            return rdfTypeWrapperSql;
        }
    }

    private void addRdfTypeParam(final MapSqlParameterSource parameterSource, final List<Condition> conditions) {
        var rdfTypeUriParamValue = "*";
        for (final Condition condition : conditions) {
            if (condition.getField().equals(RDF_TYPE)) {
                rdfTypeUriParamValue = condition.getObject();
                break;
            }
        }
        parameterSource.addValue(RDF_TYPE_URI_PARAM, convertToSqlLikeWildcard(rdfTypeUriParamValue));
    }

    private void addWhereClause(final int paramCount, final MapSqlParameterSource parameterSource,
                                final List<String> whereClauses,
                                final Condition condition) throws InvalidQueryException {
        final var field = condition.getField();
        final var operation = condition.getOperator();
        var object = condition.getObject();
        final var paramName = "param" + paramCount;
        if ((field.equals(FEDORA_ID) || field.equals(MIME_TYPE)) &&
                condition.getOperator().equals(Condition.Operator.EQ)) {
            if (!object.equals("*")) {
                final String whereClause;
                if (object.contains("*")) {
                    object = convertToSqlLikeWildcard(object);
                    whereClause = field + " LIKE :" + paramName;
                } else {
                    whereClause = field + " = :" + paramName;
                }

                whereClauses.add("s." +  whereClause);
                parameterSource.addValue(paramName, object);
            }
        } else if (field.equals(Condition.Field.CREATED) || field.equals(Condition.Field.MODIFIED)) {
            //parse date
            try {
                final var instant = InstantParser.parse(object);
                whereClauses.add("s." + field + " " + operation.getStringValue() + " :" + paramName);
                parameterSource.addValue(paramName, new Timestamp(instant.toEpochMilli()), Types.TIMESTAMP);
            } catch (final Exception ex) {
                throw new InvalidQueryException(ex.getMessage());
            }
        } else if (field.equals(CONTENT_SIZE)) {
            try {
                whereClauses.add(field + " " + operation.getStringValue() +
                        " :" + paramName);
                parameterSource.addValue(paramName, Long.parseLong(object), Types.INTEGER);
            } catch (final Exception ex) {
                throw new InvalidQueryException(ex.getMessage());
            }
        } else if (field.equals(RDF_TYPE) && condition.getOperator().equals(Condition.Operator.EQ) ) {
           //allowed but no where clause added here.
        } else {
            throw new InvalidQueryException("Condition not supported: \"" + condition + "\"");
        }
    }

    private String convertToSqlLikeWildcard(final String value) {
        return value.replaceAll("_", "\\\\_") // escape underscores
                .replaceAll("%", "\\\\%") // escape percent signs
                .replaceAll("(?<!\\\\)\\*", "%") // convert unescaped * to %
                .replaceAll("\\\\\\*", "*"); // convert \* back to *
    }

    @Override
    public void addUpdateIndex(final Transaction transaction, final ResourceHeaders resourceHeaders) {
        addUpdateIndex(transaction, resourceHeaders, null);
    }

    @Override
    public void addUpdateIndex(final Transaction transaction, final ResourceHeaders resourceHeaders,
                               final List<URI> rdfTypes) {
        final var fedoraId = resourceHeaders.getId();
        if (fedoraId.isAcl() || fedoraId.isMemento()) {
            LOGGER.debug("The search index does not include acls or mementos. Ignoring resource {}",
                    fedoraId.getFullId());
            return;
        }
        LOGGER.debug("Updating search index for {}", fedoraId);
        transaction.doInTx(() -> {
            if (!transaction.isShortLived()) {
                doUpsertWithTransaction(transaction, resourceHeaders, fedoraId);
            } else {
                doDirectUpsert(transaction, resourceHeaders, fedoraId, rdfTypes);
            }
        });

    }

    private void doDirectUpsert(final Transaction transaction, final ResourceHeaders resourceHeaders,
                                final FedoraId fedoraId, final List<URI> providedRdfTypes) {
        final var fullId = fedoraId.getFullId();
        try {
            // If no RDF types were provided, we need to fetch the resource to get them.
            List<URI> rdfTypes = providedRdfTypes;
            if (rdfTypes == null) {
                final var fedoraResource = resourceFactory.getResource(transaction, resourceHeaders);
                rdfTypes = new ArrayList<>(Sets.newHashSet(fedoraResource.getTypes()));
            }

            final Long searchId = doUpsertIntoSimpleSearch(fedoraId, resourceHeaders);
            insertRdfTypes(rdfTypes);

            // Only need to delete existing type associations for live indexing
            if (initializationStatus.isInitializationComplete()) {
                deleteRdfTypeAssociations(fedoraId);
            }
            insertRdfTypeAssociations(rdfTypes, searchId);
        } catch (final Exception e) {
            throw new RepositoryRuntimeException("Failed add/updated the search index for : " + fullId, e);
        }
    }

    /**
     * Adds the list of RDF types to the db, if they aren't already there, and returns a set of types that were
     * actually added.
     *
     * @param rdfTypes the types to attempt to add
     * @return the types that were added
     */
    private void insertRdfTypes(final List<URI> rdfTypes) {
        final MapSqlParameterSource[] params = rdfTypes.stream()
                .filter(rdfType -> !rdfTypeIdCache.containsKey(rdfType))
                .map(r -> new MapSqlParameterSource().addValue(RDF_TYPE_URI_PARAM, r.toString()))
                .toArray(MapSqlParameterSource[]::new);
        try {
            if (params.length == 0) {
                return;
            }
            jdbcTemplate.batchUpdate(INSERT_RDF_TYPE_MAPPING.get(dbPlatForm), params);
        } catch (final DuplicateKeyException e) {
            // ignore duplicate keys
        } catch (final CannotAcquireLockException e) {
            // if we can't get a lock
            throw new RepositoryRuntimeException("Failed to add RDF type(s) to search index", e);
        }
    }

    private void doUpsertWithTransaction(final Transaction transaction, final ResourceHeaders resourceHeaders,
                                         final FedoraId fedoraId) {
        final var fullId = fedoraId.getFullId();
        try {
            final var txId = transaction.getId();
            final var fedoraResource = resourceFactory.getResource(transaction, resourceHeaders);
            doUpsertIntoTransactionTables(txId, fedoraId, resourceHeaders, "add");
            // add rdf type associations to the rdf type association table
            final var rdfTypes = Sets.newHashSet(fedoraResource.getTypes());
            insertRdfTypeAssociationsInTransaction(rdfTypes, txId, fedoraId);
        } catch (final Exception e) {
            throw new RepositoryRuntimeException("Failed add/updated the search index for : " + fullId, e);
        }
    }

    /**
     * Do the upsert action to the transaction table.
     *
     * @param txId            the transaction id
     * @param fedoraId        the resourceId
     * @param resourceHeaders the resources headers
     * @param operation       the operation to perform.
     */
    private void doUpsertIntoTransactionTables(final String txId, final FedoraId fedoraId,
                                               final ResourceHeaders resourceHeaders, final String operation) {
        var mimetype = "";
        long contentSize = 0;
        var modified = Instant.now();
        var created = Instant.now();
        if (resourceHeaders != null) {
            contentSize = resourceHeaders.getContentSize();
            mimetype = resourceHeaders.getMimeType();
            modified = resourceHeaders.getLastModifiedDate();
            created = resourceHeaders.getCreatedDate();
        }

        final var params = new MapSqlParameterSource();
        params.addValue(FEDORA_ID_PARAM, fedoraId.getFullId());
        params.addValue(MIME_TYPE_PARAM, mimetype);
        params.addValue(CONTENT_SIZE_PARAM, contentSize);
        params.addValue(CREATED_PARAM, formatInstant(created));
        params.addValue(MODIFIED_PARAM, formatInstant(modified));
        params.addValue(OPERATION_PARAM, operation);
        params.addValue(TRANSACTION_ID_PARAM, txId);
        jdbcTemplate.update(TRANSACTION_UPSERT_MAPPING.get(dbPlatForm), params);
    }

    /**
     * Do direct upsert into simpl search table.
     *
     * @param fedoraId        the resourceId
     * @param resourceHeaders the resources headers
     */
    private Long doUpsertIntoSimpleSearch(final FedoraId fedoraId,
                                          final ResourceHeaders resourceHeaders) {
        var mimetype = "";
        long contentSize = 0;
        var modified = Instant.now();
        var created = Instant.now();
        if (resourceHeaders != null) {
            contentSize = resourceHeaders.getContentSize();
            mimetype = resourceHeaders.getMimeType();
            modified = resourceHeaders.getLastModifiedDate();
            created = resourceHeaders.getCreatedDate();
        }

        final var params = new MapSqlParameterSource();
        params.addValue(FEDORA_ID_PARAM, fedoraId.getFullId());
        params.addValue(MIME_TYPE_PARAM, mimetype);
        params.addValue(CONTENT_SIZE_PARAM, contentSize);
        params.addValue(CREATED_PARAM, formatInstant(created));
        params.addValue(MODIFIED_PARAM, formatInstant(modified));

        if (dbPlatForm == DbPlatform.MYSQL || dbPlatForm == DbPlatform.H2) {
            jdbcTemplate.update(DIRECT_UPSERT_MAPPING.get(dbPlatForm), params);
            return jdbcTemplate.queryForObject(SELECT_RESOURCE_SEARCH_ID,
                    Map.of(FEDORA_ID_PARAM, fedoraId.getFullId()), Long.class);
        } else {
            return jdbcTemplate.queryForObject(DIRECT_UPSERT_MAPPING.get(dbPlatForm), params, Long.class);
        }
    }

    private Timestamp formatInstant(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.from(instant.truncatedTo(ChronoUnit.MILLIS));
    }

    private void insertRdfTypeAssociationsInTransaction(final Set<URI> rdfTypes,
                                                        final String txId,
                                                        final FedoraId fedoraId) {
        //remove and add type associations for the fedora id.
        final List<MapSqlParameterSource> parameterSourcesList = new ArrayList<>(rdfTypes.size());
        final var parameterSource = new MapSqlParameterSource();
        parameterSource.addValue(TRANSACTION_ID_PARAM, txId);
        parameterSource.addValue(FEDORA_ID_PARAM, fedoraId.getFullId());
        jdbcTemplate.update(DELETE_RESOURCE_TYPE_ASSOCIATIONS_IN_TRANSACTION, parameterSource);

        for (final var rdfType : rdfTypes) {
            final var assocParams = new MapSqlParameterSource();
            assocParams.addValue(TRANSACTION_ID_PARAM, txId);
            assocParams.addValue(FEDORA_ID_PARAM, fedoraId.getFullId());
            assocParams.addValue(RDF_TYPE_URI_PARAM, rdfType.toString());
            parameterSourcesList.add(assocParams);
        }
        final MapSqlParameterSource[] psArray = parameterSourcesList.toArray(new MapSqlParameterSource[0]);
        jdbcTemplate.batchUpdate(INSERT_RDF_TYPE_ASSOC_IN_TRANSACTION, psArray);
    }

    private void deleteRdfTypeAssociations(final FedoraId fedoraId) {
        final var deleteParams = new MapSqlParameterSource();
        deleteParams.addValue(FEDORA_ID_PARAM, fedoraId.getFullId());
        jdbcTemplate.update(DELETE_RDF_TYPE_ASSOCIATIONS_MAP.get(dbPlatForm),
                deleteParams);
    }

    private void insertRdfTypeAssociations(final List<URI> rdfTypes,
                                           final Long resourceSearchId) {
        //add rdf type associations
        final List<MapSqlParameterSource> parameterSourcesList = new ArrayList<>();
        for (final var rdfType : rdfTypes) {
            final Long rdfTypeId = getRdfTypeId(rdfType);

            final var assocParams = new MapSqlParameterSource();
            assocParams.addValue(RESOURCE_SEARCH_ID_PARAM, resourceSearchId);
            assocParams.addValue(RDF_TYPE_ID_PARAM, rdfTypeId);
            parameterSourcesList.add(assocParams);
        }

        final MapSqlParameterSource[] psArray = parameterSourcesList.toArray(new MapSqlParameterSource[0]);
        jdbcTemplate.batchUpdate(INSERT_RDF_TYPE_ASSOC, psArray);
    }

    private Long getRdfTypeId(final URI rdfType) {
        return rdfTypeIdCache.computeIfAbsent(rdfType, uri ->
                jdbcTemplate.queryForObject(
                        SELECT_RDF_TYPE_ID,
                        Map.of(RDF_TYPE_URI_PARAM, uri.toString()),
                        Long.class)
        );
    }

    @Override
    public void removeFromIndex(final Transaction transaction, final FedoraId fedoraId) {
        transaction.doInTx(() -> {
            if (!transaction.isShortLived()) {
                try {
                    doUpsertIntoTransactionTables(transaction.getId(), fedoraId, null, "delete");
                } catch (final Exception e) {
                    throw new RepositoryRuntimeException("Failed to remove " + fedoraId + " from search index", e);
                }
            } else {
                doDirectRemove(fedoraId);
            }
        });
    }

    private void doDirectRemove(final FedoraId fedoraId) {
        deleteRdfTypeAssociations(fedoraId);
        deleteResource(fedoraId);
    }

    private void deleteResource(final FedoraId fedoraId) {
        final var params = new MapSqlParameterSource();
        params.addValue(FEDORA_ID_PARAM, fedoraId.getFullId());
        jdbcTemplate.update(DELETE_RESOURCE_FROM_SEARCH, params);
    }

    @Override
    public void reset() {
        rdfTypeIdCache.clear();

        executeTruncationBatches(
                SEARCH_RESOURCE_RDF_TYPE_TABLE,
                SEARCH_RDF_TYPE_TABLE,
                SIMPLE_SEARCH_TABLE,
                SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE,
                SIMPLE_SEARCH_TRANSACTIONS_TABLE
        );
    }

    private void executeTruncationBatches(final String... truncateQueries) {
        try (final var conn = this.dataSource.getConnection();
             final var statement = conn.createStatement()) {
            // Disable foreign key checks before truncate operations
            for (final var sql : toggleForeignKeyChecks(false)) {
                statement.addBatch(sql);
            }
            // Truncate tables
            for (final String query : truncateQueries) {
                statement.addBatch(truncateTable(query));
            }
            // Re-enable foreign key checks after truncate operations
            for (final var sql : toggleForeignKeyChecks(true)) {
                statement.addBatch(sql);
            }
            statement.executeBatch();
        } catch (final SQLException e) {
            throw new RepositoryRuntimeException("Failed to truncate search index tables", e);
        }
    }

    @Override
    public void clearAllTransactions() {
        executeTruncationBatches(
                SEARCH_RESOURCE_RDF_TYPE_TRANSACTIONS_TABLE,
                SIMPLE_SEARCH_TRANSACTIONS_TABLE
        );
    }

    @Override
    public void commitTransaction(final Transaction tx) {
        if (!tx.isShortLived()) {
            tx.ensureCommitting();
            final var txId = tx.getId();
            try {
                final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
                parameterSource.addValue(TRANSACTION_ID_PARAM, txId);
                final int deletedAssociations = jdbcTemplate.update(
                        COMMIT_DELETE_RDF_TYPE_ASSOCIATIONS_MAP.get(dbPlatForm),
                        parameterSource);
                final int deletedResources = jdbcTemplate.update(
                        COMMIT_DELETE_RESOURCES_IN_TRANSACTION_MAP.get(dbPlatForm),
                        parameterSource);
                final int addedRdfTypes = jdbcTemplate.update(
                        COMMIT_RDF_TYPES_MAP.get(dbPlatForm),
                        parameterSource);
                final int addedResources = jdbcTemplate.update(UPSERT_COMMIT_MAPPING.get(dbPlatForm),
                        parameterSource);
                final int addRdfTypeAssociations = jdbcTemplate.update(COMMIT_RDF_TYPE_ASSOCIATIONS, parameterSource);
                cleanupTransaction(txId);
                LOGGER.debug("Commit of tx {} complete with {} resource adds, {} resource associations adds, " +
                                "{} rdf types adds{},  resource deletes, {} resource/rdf type associations deletes",
                        txId, addedResources, addRdfTypeAssociations, addedRdfTypes, deletedResources,
                        deletedAssociations);
            } catch (final Exception e) {
                LOGGER.warn("Unable to commit search index transaction {}: {}", txId, e.getMessage());
                throw new RepositoryRuntimeException("Unable to commit search index transaction", e);
            }
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void rollbackTransaction(final Transaction tx) {
        if (!tx.isShortLived()) {
            cleanupTransaction(tx.getId());
        }
    }

    private void cleanupTransaction(final String txId) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue(TRANSACTION_ID_PARAM, txId);
        jdbcTemplate.update(DELETE_TRANSACTION, parameterSource);
        jdbcTemplate.update(DELETE_RDF_TYPE_ASSOCIATIONS_IN_TRANSACTION, parameterSource);
        LOGGER.debug("Transaction data has been removed from the search transaction tables for txId={} ", txId);
    }

    private List<String> toggleForeignKeyChecks(final boolean enable) {

        if (isPostgres()) {
            return Collections.emptyList();
        } else if (isH2()) {
            return List.of("SET REFERENTIAL_INTEGRITY  " + (enable ? "TRUE" : "FALSE") + ";");
        } else {
            return List.of("SET FOREIGN_KEY_CHECKS = " + (enable ? 1 : 0) + ";");
        }
    }

    private boolean isPostgres() {
        return dbPlatForm.equals(POSTGRESQL);
    }

    private boolean isH2() {
        return dbPlatForm.equals(H2);
    }

    private String truncateTable(final String tableName) {
        final var addCascade = isPostgres();
        return "TRUNCATE TABLE " + tableName + (addCascade ? " CASCADE" : "") + ";";
    }

}
