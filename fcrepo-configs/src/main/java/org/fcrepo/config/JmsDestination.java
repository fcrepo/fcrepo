/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

/**
 * Indicates if a JMS topic or queue is the destination
 *
 * @author pwinckles
 */
public enum JmsDestination {

    TOPIC("topic"),
    QUEUE("queue");

    private final String value;

    JmsDestination(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static JmsDestination fromString(final String value) {
        for (final var destination : values()) {
            if (destination.value.equalsIgnoreCase(value)) {
                return destination;
            }
        }
        throw new IllegalArgumentException("Unknown JMS destination: " + value);
    }

    @Override
    public String toString() {
        return value;
    }

}
