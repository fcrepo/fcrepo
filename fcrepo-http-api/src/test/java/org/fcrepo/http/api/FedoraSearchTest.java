/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidConditionExpressionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Arrays;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.search.api.Condition.Field.FEDORA_ID;
import static org.fcrepo.search.api.Condition.Operator.EQ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author dbernstein
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FedoraSearchTest {

    private HttpIdentifierConverter converter;

    private static final String uriBase = "http://localhost:8080/rest";

    private static final String uriTemplate = uriBase + "/{path: .*}";

    private UriBuilder uriBuilder;

    @BeforeEach
    public void setUp() {
        uriBuilder = UriBuilder.fromUri(uriTemplate);
        converter = new HttpIdentifierConverter(uriBuilder);
    }

    @Test
    public void testValidConditionsForFedoraId() throws InvalidConditionExpressionException {
        final var conditions = new ArrayList<String>();
        final var objects = new String[]{"test", "/test", uriBase + "/test", FEDORA_ID_PREFIX + "/test"};
        for (final String object : objects) {
            for (final Condition.Operator operator : Condition.Operator.values()) {
                conditions.add(FEDORA_ID.name().toLowerCase() + operator.getStringValue() + object);
            }
        }

        for (final String condition : conditions) {
            final var con = FedoraSearch.parse(condition, converter);
            assertNotNull(con.getField());
            assertNotNull(con.getOperator());
            assertEquals("info:fedora/test", con.getObject(),
                    "unexpected object for condition: " + con);
        }

        verifyEquals(FedoraSearch.parse("fedora_id=*", converter), FEDORA_ID, EQ, "*");
    }

    private void verifyEquals(final Condition condition, final Condition.Field field, final Condition.Operator op,
                              final String s) {
        assertEquals(field, condition.getField());
        assertEquals(op, condition.getOperator());
        assertEquals(s, condition.getObject());
    }

    @Test
    public void testValidConditionsNonFedoraId() throws InvalidConditionExpressionException {
        final var conditions = new ArrayList<String>();
        final var object = "test";
        Arrays.stream(Condition.Field.values()).filter(x -> !x.equals(FEDORA_ID)).forEach(field -> {
            for (final Condition.Operator operator : Condition.Operator.values()) {
                conditions.add(field.name().toLowerCase() + operator.getStringValue() + object);
            }
        });

        for (final String condition : conditions) {
            final var con = FedoraSearch.parse(condition, converter);
            assertNotNull(con.getField());
            assertNotNull(con.getOperator());
            assertEquals("test", con.getObject(), "unexpected object for condition: " + con);
        }
    }

    @Test
    public void testInvalidOperators() {
        final var conditions = new String[]{
                "fedora_id>>/test",
                "invalid_field=/test",
                "fedora_id==/test"
        };

        for (final String condition : conditions) {
            assertThrows(Exception.class, () -> FedoraSearch.parse(condition, converter));
        }
    }


}
