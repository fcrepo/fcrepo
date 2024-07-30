/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

/**
 * Display OCFL path mode enum
 * @author whikloj
 */
public enum DisplayOcflPath {
    NONE("none"),
    RELATIVE("relative"),
    ABSOLUTE("absolute");

    private final String value;

    DisplayOcflPath(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DisplayOcflPath fromString(final String value) {
        for (final var mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown display OCFL path mode: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
