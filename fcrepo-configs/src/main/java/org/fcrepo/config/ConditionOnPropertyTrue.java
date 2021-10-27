/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

/**
 * This condition enables a bean/configuration when the specified property is true
 *
 * Implementations must provide a no-arg constructor.
 *
 * @author pwinckles
 */
public abstract class ConditionOnPropertyTrue extends ConditionOnProperty<Boolean> {

    public ConditionOnPropertyTrue(final String name, final boolean defaultValue) {
        super(name, true, defaultValue, Boolean.class);
    }

}
