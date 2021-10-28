/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

/**
 * Indicates what storage backend to use.
 *
 * @author pwinckles
 */
public enum Storage {

    OCFL_FILESYSTEM("ocfl-fs"),
    OCFL_S3("ocfl-s3");

    private final String value;

    Storage(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Storage fromString(final String value) {
        for (final var storage : values()) {
            if (storage.value.equalsIgnoreCase(value)) {
                return storage;
            }
        }
        throw new IllegalArgumentException("Unknown storage: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
