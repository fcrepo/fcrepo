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

/**
 * A data structure representing a search condition.
 *
 * @author dbernstein
 */
public class Condition {
    /**
     * Default constructor
     *
     * @param field    The search field (condition subject)
     * @param operator The operator (condition predicate)
     * @param object   The object (condition object)
     */
    public Condition(final Field field, final Operator operator, final String object) {
        this.field = field;
        this.operator = operator;
        this.object = object;
    }



    private Field field;
    private Operator operator;
    private String object;

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

        public static final Operator fromString(final String str) {
            for (Operator o : Operator.values()) {
                if (o.value.equals(str)) {
                    return o;
                }
            }

            throw new IllegalArgumentException("Value " + str + " not recognized.");
        }

    }

    /**
     * Field accessor
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
     * Returns the object portion of the condition
     * @return
     */
    public String getObject() {
        return object;
    }

    public enum Field {
        fedora_id,
        mimetype,
        size,
        modified,
        created,
        creator;
    }

    @Override
    public String toString() {
        return "Condition{" +
                "field=" + field +
                ", operator=" + operator +
                ", object='" + object + '\'' +
                '}';
    }
}
