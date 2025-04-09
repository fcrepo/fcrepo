/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.search.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.fcrepo.search.api.Condition;
import org.fcrepo.search.api.Condition.Field;
import org.fcrepo.search.api.Condition.Operator;
import org.fcrepo.search.api.InvalidConditionExpressionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;





/**
 * @author bbpennel
 */
public class ConditionTest {

    @Test
    public void testFromExpressionValid() throws InvalidConditionExpressionException {
        final var condition = Condition.fromExpression("fedora_id=test123");
        assertEquals(Field.FEDORA_ID, condition.getField());
        assertEquals(Operator.EQ, condition.getOperator());
        assertEquals("test123", condition.getObject());
    }

    @ParameterizedTest
    @CsvSource({
            "fedora_id=test123, FEDORA_ID, EQ, test123",
            "modified>2021-01-01, MODIFIED, GT, 2021-01-01",
            "created<=2021-12-31, CREATED, LTE, 2021-12-31",
            "content_size>=1024, CONTENT_SIZE, GTE, 1024",
            "mime_type=text/plain, MIME_TYPE, EQ, text/plain",
            "rdf_type=http://example.org/type, RDF_TYPE, EQ, http://example.org/type"
    })
    public void testFromExpressionVariations(final String expression, final String fieldStr,
                                             final String opStr, final String object)
            throws InvalidConditionExpressionException {
        final var condition = Condition.fromExpression(expression);
        assertEquals(Field.valueOf(fieldStr), condition.getField());
        assertEquals(Operator.valueOf(opStr), condition.getOperator());
        assertEquals(object, condition.getObject());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid=value",
            "fedora_id==value",
            "fedora_id>>value",
            "=value",
            "fedora_id=",
            "fedora_id==>="
    })
    public void testFromExpressionInvalid(final String expression) {
        assertThrows(InvalidConditionExpressionException.class,
                () -> Condition.fromExpression(expression));
    }

    @Test
    public void testFromEnums() {
        final var condition = Condition.fromEnums(Field.MODIFIED, Operator.GT, "2021-01-01");
        assertEquals(Field.MODIFIED, condition.getField());
        assertEquals(Operator.GT, condition.getOperator());
        assertEquals("2021-01-01", condition.getObject());
    }

    @Test
    public void testToString() {
        final var condition = Condition.fromEnums(Field.CONTENT_SIZE, Operator.GTE, "1024");
        assertEquals("content_sizeGTE1024", condition.toString());
    }

    @ParameterizedTest
    @CsvSource({
            "fedora_id, FEDORA_ID",
            "FEDORA_ID, FEDORA_ID",
            "modified, MODIFIED",
            "MODIFIED, MODIFIED",
            "content_size, CONTENT_SIZE"
    })
    public void testFieldFromStringValid(final String input, final String expected)
            throws InvalidConditionExpressionException {
        assertEquals(Field.valueOf(expected), Field.fromString(input));
    }

    @Test
    public void testFieldFromStringInvalid() {
        assertThrows(InvalidConditionExpressionException.class,
                () -> Field.fromString("nonexistent_field"));
    }

    @ParameterizedTest
    @CsvSource({
            "=, EQ",
            "<, LT",
            ">, GT",
            "<=, LTE",
            ">=, GTE"
    })
    public void testOperatorFromStringValid(final String input, final String expected) {
        assertEquals(Operator.valueOf(expected), Operator.fromString(input));
    }

    @Test
    public void testOperatorFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Operator.fromString("!="));
    }

    @Test
    public void testOperatorGetStringValue() {
        assertEquals("<=", Operator.LTE.getStringValue());
        assertEquals(">=", Operator.GTE.getStringValue());
        assertEquals("=", Operator.EQ.getStringValue());
        assertEquals(">", Operator.GT.getStringValue());
        assertEquals("<", Operator.LT.getStringValue());
    }

    @Test
    public void testFieldToString() {
        assertEquals("fedora_id", Field.FEDORA_ID.toString());
        assertEquals("modified", Field.MODIFIED.toString());
        assertEquals("created", Field.CREATED.toString());
    }

    @Test
    public void testGetters() {
        final var field = Field.RDF_TYPE;
        final var operator = Operator.EQ;
        final var object = "http://example.org/type";
        final var condition = Condition.fromEnums(field, operator, object);

        assertEquals(field, condition.getField());
        assertEquals(operator, condition.getOperator());
        assertEquals(object, condition.getObject());
    }
}