/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services.functions;

import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;
import javax.inject.Inject;
import java.util.StringJoiner;
import java.util.stream.IntStream;
import org.fcrepo.config.FedoraPropsConfig;

/**
 * Unique value minter that creates hierarchical IDs from a UUID
 *
 * @author rdfloyd
 * @author whikloj
 */
@Component
public class ConfigurableHierarchicalSupplier implements UniqueValueSupplier {

    private final int length;
    private final int count;

    @Inject
    protected FedoraPropsConfig fedoraPropsConfig;

    /**
     * Mint a unique identifier by default using defaults or
     * if set, use the length and count from fedora properties
     */
    @Inject
    public ConfigurableHierarchicalSupplier() {
        length = fedoraPropsConfig.getFcrepoPidMinterLength();
        count = fedoraPropsConfig.getFcrepoPidMinterCount();
    }

    /**
     * Mint a unique identifier as a UUID
     *
     * @return uuid
     */
    @Override
    public String get() {
        //final int length = fedoraPropsConfig.getFcrepoPidMinterLength();
        //final int count = fedoraPropsConfig.getFcrepoPidMinterCount();

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
