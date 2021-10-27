/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

/**
 * @author pwinckles
 */
public enum ServerManagedPropsMode {

    STRICT("strict"),
    RELAXED("relaxed");

    private final String value;

    ServerManagedPropsMode(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ServerManagedPropsMode fromString(final String value) {
        for (final var mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown server managed properties mode: " + value);
    }

    @Override
    public String toString() {
        return value;
    }

}
