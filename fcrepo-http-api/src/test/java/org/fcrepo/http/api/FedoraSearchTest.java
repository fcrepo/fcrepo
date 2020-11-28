/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.http.api;

import org.fcrepo.http.commons.api.rdf.HttpIdentifierConverter;
import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.InvalidConditionExpressionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Arrays;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.fcrepo.search.api.Condition.Field.FEDORA_ID;
import static org.fcrepo.search.api.Condition.Operator.EQ;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author dbernstein
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FedoraSearchTest {

    private HttpIdentifierConverter converter;

    private static final String uriBase = "http://localhost:8080/rest";

    private static final String uriTemplate = uriBase + "/{path: .*}";

    private UriBuilder uriBuilder;

    @Before
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
            assertEquals("unexpected object for condition: " + con, "info:fedora/test",
                    con.getObject());
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
            assertEquals("unexpected object for condition: " + con, "test",
                    con.getObject());
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
            try {
                FedoraSearch.parse(condition, converter);
                fail("Condition should have failed: " + condition);
            } catch (final Exception ex) {
            }
        }
    }


}
