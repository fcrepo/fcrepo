/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.lock;

/**
 * @author whikloj
 * @since 6.3.1
 */
public enum ResourceLockType {

    EXCLUSIVE("exclusive"),
    NONEXCLUSIVE("non-exclusive");

    private final String value;

    ResourceLockType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ResourceLockType fromString(final String value) {
        for (final var mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown resource lock type: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
