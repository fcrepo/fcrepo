/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.ok;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_HTML_WITH_CHARSET;
import static org.fcrepo.http.commons.domain.RDFMediaType.TEXT_PLAIN_WITH_CHARSET;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import io.micrometer.core.annotation.Timed;
import org.apache.commons.lang3.StringUtils;
import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidConditionExpressionException;
import org.fcrepo.search.api.InvalidQueryException;
import org.fcrepo.search.api.SearchIndex;
import org.fcrepo.search.api.SearchParameters;
import org.fcrepo.search.api.SearchResult;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;

/**
 * @author dbernstein
 * @since 05/06/20
 */
@Timed
@Scope("request")
@Path("/fcr:search")
public class FedoraSearch extends FedoraBaseResource {

    private static final Logger LOGGER = getLogger(FedoraSearch.class);

    @Autowired
    @Qualifier("searchIndex")
    private SearchIndex searchIndex;

    /**
     * Default JAX-RS entry point
     */
    public FedoraSearch() {
        super();
    }

    /**
     * Perform simple search on the repository
     *
     * @param conditions The conditions constraining the query
     * @param fields     The fields to return in results
     * @param maxResults The max number of results to return
     * @param offset     The zero-based offset of the first result to be returned
     * @param order      The order: ie "asc" or "desc"
     * @param orderBy    The field by which to order the results
     * @param includeTotalResultCount A flag for including total result count (false by default)
     * @return A response object with the search results
     */
    @GET
    @Produces({APPLICATION_JSON + ";qs=1.0",
            TEXT_PLAIN_WITH_CHARSET,
            TEXT_HTML_WITH_CHARSET})
    public Response doSearch(@QueryParam(value = "condition") final List<String> conditions,
                             @QueryParam(value = "fields") final String fields,
                             @DefaultValue("100") @QueryParam("max_results") final int maxResults,
                             @DefaultValue("0") @QueryParam("offset") final int offset,
                             @DefaultValue("asc") @QueryParam("order") final String order,
                             @QueryParam("order_by") final String orderBy,
                             @DefaultValue("false") @QueryParam("include_total_result_count")
                                         final boolean includeTotalResultCount) {

        LOGGER.info("GET on search with conditions: {}, and fields: {}", conditions, fields);
        try {
            final var conditionList = new ArrayList<Condition>();
            for (final String condition : conditions) {
                final var parsedCondition = parse(condition, identifierConverter());
                conditionList.add(parsedCondition);
            }

            List<Condition.Field> parsedFields = null;
            if (StringUtils.isBlank(fields) || fields.equals("*")) {
                parsedFields = Arrays.asList(Condition.Field.values());
            } else {
                parsedFields = new ArrayList<>();
                for (final String field : fields.split(",")) {
                    try {
                        parsedFields.add(Condition.Field.fromString(field));
                    } catch (final Exception e) {
                        throw new InvalidQueryException("The field \"" + field + "\" is not a valid output field.");
                    }
                }
            }

            final Condition.Field orderByField;
            try {
                if (!StringUtils.isBlank(orderBy)) {
                    orderByField = Condition.Field.fromString(orderBy);
                } else {
                    orderByField = null;
                }
            } catch (final Exception e) {
                throw new InvalidQueryException("The order_by field must contain a valid value such as " +
                        StringUtils.join(Condition.Field.values(), ","));
            }

            if (!(order.equalsIgnoreCase("asc") || order.equalsIgnoreCase("desc"))) {
                throw new InvalidQueryException("The order field is invalid:  valid values are \"asc\" and \"desc\"");
            }

            final var params = new SearchParameters(parsedFields, conditionList, maxResults, offset, orderByField,
                    order, includeTotalResultCount);
            final Response.ResponseBuilder builder = ok();
            final var result = this.searchIndex.doSearch(params);
            final var translatedResults = translateResults(result);

            builder.entity(translatedResults);
            return builder.build();
        } catch (final InvalidConditionExpressionException | InvalidQueryException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    private SearchResult translateResults(final SearchResult result) {
        result.getItems().forEach(item -> {
            final var key = Condition.Field.FEDORA_ID.toString();
            final var fedoraId = item.get(key);
            if (fedoraId != null) {
                item.put(key, identifierConverter().toExternalId(fedoraId.toString()));
            }
        });
        return result;
    }

    /**
     * Parses the url decoded value of a single parameter passed by the
     * http layer into a {@link Condition}.
     *
     * @param expression The url decoded value of the condition parameter.
     * @return the parsed {@link Condition} object.
     */
    protected static Condition parse(final String expression, final HttpIdentifierConverter converter)
            throws InvalidConditionExpressionException {
        final Condition condition = Condition.fromExpression(expression);
        if (condition.getField().equals(Condition.Field.FEDORA_ID)) {
            //convert the object value to an internal identifier stem where appropriate
            final var object = condition.getObject();
            final var field = condition.getField();
            final var operator = condition.getOperator();
            if (!object.startsWith(FEDORA_ID_PREFIX) && isExternalUrl(object)) {
                return Condition.fromEnums(field, operator, converter.toInternalId(object));
            } else if (object.startsWith("/")) {
                return Condition.fromEnums(field, operator, FEDORA_ID_PREFIX + object);
            } else if (!object.startsWith(FEDORA_ID_PREFIX) && !object.equals("*")) {
                return Condition.fromEnums(field, operator, FEDORA_ID_PREFIX + "/" + object);
            }
        }

        return condition;
    }

    private static boolean isExternalUrl(final String str) {
        try {
            new URL(str);
            return true;
        } catch (final Exception ex) {
            return false;
        }
    }

}

