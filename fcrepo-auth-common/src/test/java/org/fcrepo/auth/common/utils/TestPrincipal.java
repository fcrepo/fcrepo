/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.common.utils;

import java.security.Principal;

/**
 * Simple implementation of Principal for testing.
 *
 * @author whikloj
 */
public class TestPrincipal implements Principal {

    private final String name;

    public TestPrincipal(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
