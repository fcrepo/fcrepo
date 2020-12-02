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
package org.fcrepo.search.api;

import java.util.regex.Pattern;

/**
 * A data structure representing a search condition.
 *
 * @author dbernstein
 */
public class Condition {
    public enum Operator {
        LTE("<="),
        GTE(">="),
        EQ("="),
        GT(">"),
        LT("<");

        private String value;

        Operator(final String value) {
            this.value = value;
        }

        public String getStringValue() {
            return this.value;
        }

        public static Operator fromString(final String str) {
            for (final Operator o : Operator.values()) {
                if (o.value.equals(str)) {
                    return o;
                }
            }

            throw new IllegalArgumentException("Value " + str + " not recognized.");
        }

    }

    public enum Field {
        FEDORA_ID,
        MODIFIED,
        CREATED,
        CONTENT_SIZE,
        MIME_TYPE,
        RDF_TYPE;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        public static Field fromString(final String fieldStr) {
            return Field.valueOf(fieldStr.toUpperCase());
        }
    }

    /* A regex for parsing the value of a "condition" query  parameter which follows the format
     * [field_name][operation][object]
     * The field name is composed of at least one character and can contain alpha number characters and underscores.
     * The operation can equal "=", "<", ">", "<=" or ">="
     * The object can be anything but cannot start with >, <, and =.
     */
    final static Pattern CONDITION_REGEX = Pattern.compile("([a-zA-Z0-9_]+)([><=]|<=|>=)([^><=].*)");


    private Field field;
    private Operator operator;
    private String object;

    /**
     * Internal constructor
     *
     * @param field    The search field (condition subject)
     * @param operator The operator (condition predicate)
     * @param object   The object (condition object)
     */
    private Condition(final Field field, final Operator operator, final String object) {
        this.field = field;
        this.operator = operator;
        this.object = object;
    }

    /**
     * Field accessor
     *
     * @return the field
     */
    public Field getField() {
        return field;
    }

    /**
     * Operator accessor
     * @return the operator
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * @return the object portion of the condition
     */
    public String getObject() {
        return object;
    }

    @Override
    public String toString() {
        return this.field.toString().toLowerCase() + operator + object;
    }

    /**
     * Parses a string expression into a Condition object.
     * @param expression The condition as a string expression.
     * @return The condition
     * @throws InvalidConditionExpressionException if we can't parse the string into a Condition.
     */
    public static Condition fromExpression(final String expression) throws InvalidConditionExpressionException {
        final var m = CONDITION_REGEX.matcher(expression);
        if (m.matches()) {
            final var field = Field.fromString(m.group(1));
            final var operation = Operator.fromString(m.group(2));
            final var object = m.group(3);
            return fromEnums(field, operation, object);
        }

        throw new InvalidConditionExpressionException(expression);
    }

    public static Condition fromEnums(final Field field, final Operator operator, final String expression) {
        return new Condition(field, operator, expression);
    }
}
