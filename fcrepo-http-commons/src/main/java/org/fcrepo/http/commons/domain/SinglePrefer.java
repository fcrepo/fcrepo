/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons.domain;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.fcrepo.http.commons.domain.PreferTag.emptyTag;

import java.util.Set;
import java.util.TreeSet;

/**
 * JAX-RS HTTP parameter parser for the Prefer header
 *
 * @author cabeer
 * @author ajs6f
 * @author acoburn
 */
public class SinglePrefer {

    private final Set<PreferTag> preferTags = new TreeSet<>();

    /**
     * Parse a Prefer: header
     *
     * @param header the header
     */
    public SinglePrefer(final String header) {
        preferTags.addAll(stream(header.split(","))
                .map(PreferTag::new)
                .collect(toSet()));
    }

    /**
     * Does the Prefer: header have a return tag
     *
     * @return true if the header has a return tag
     */
    public Boolean hasReturn() {
        return preferTags().stream().map(PreferTag::getTag).anyMatch("return"::equals);
    }

    /**
     * Does the Prefer: header have a return tag
     *
     * @return true if the header has a return tag
     */
    public Boolean hasHandling() {
        return preferTags().stream().map(PreferTag::getTag).anyMatch("handling"::equals);
    }

    /**
     * Get the return tag, or a blank default, if none exists.
     *
     * @return return tag, or a blank default, if none exists
     */
    public PreferTag getReturn() {
        return preferTags().stream()
                .filter(x -> x.getTag().equals("return"))
                .findFirst().orElse(emptyTag());
    }

    /**
     * Get the return tag, or a blank default, if none exists.
     *
     * @return return tag, or a blank default, if none exists
     */
    public PreferTag getHandling() {
        return preferTags().stream()
                .filter(x -> x.getTag().equals("handling"))
                .findFirst().orElse(emptyTag());
    }

    protected Set<PreferTag> preferTags() {
        return preferTags;
    }
}
