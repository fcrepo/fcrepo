/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services.functions;

import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;

import java.util.StringJoiner;
import java.util.stream.IntStream;

/**
 * Unique value minter that creates hierarchical IDs from a UUID
 *
 * @author rdfloyd
 * @author whikloj
 */
@Component
public class ConfigurableHierarchicalSupplier implements UniqueValueSupplier {

    private static final int DEFAULT_LENGTH = 0;
    private static final int DEFAULT_COUNT = 0;

    private final int length;
    private final int count;


    /**
     * Mint a hierarchical identifier with args to control length and count of the pairtree. A length or count of ZERO
     * will return a non-hierarchical identifier.
     *
     * @param desiredLength the desired length of pairtree parts
     * @param desiredCount the desired number of pairtree parts
     */
    public ConfigurableHierarchicalSupplier(final int desiredLength, final int desiredCount) {
        length = desiredLength;
        count = desiredCount;
    }

    /**
     * Mint a unique identifier by default using defaults
     *
     */
    public ConfigurableHierarchicalSupplier() {
        length = DEFAULT_LENGTH;
        count = DEFAULT_COUNT;
    }

    /**
     * Mint a unique identifier as a UUID
     *
     * @return uuid
     */
    @Override
    public String get() {

        final String s = randomUUID().toString();
        final String id;

        if (count > 0 && length > 0) {
            final StringJoiner joiner = new StringJoiner("/", "", "/" + s);

            IntStream.rangeClosed(0, count - 1)
            .forEach(x -> joiner.add(s.substring(x * length, (x + 1) * length)));
            id = joiner.toString();
        } else {
            id = s;
        }
        return id;
    }
}
