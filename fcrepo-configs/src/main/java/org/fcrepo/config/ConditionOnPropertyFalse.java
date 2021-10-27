/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

/**
 * This condition enables a bean/configuration when the specified property is false
 *
 * Implementations must provide a no-arg constructor.
 *
 * @author pwinckles
 */
public abstract class ConditionOnPropertyFalse extends ConditionOnProperty<Boolean> {

    public ConditionOnPropertyFalse(final String name, final boolean defaultValue) {
        super(name, false, defaultValue, Boolean.class);
    }

}
