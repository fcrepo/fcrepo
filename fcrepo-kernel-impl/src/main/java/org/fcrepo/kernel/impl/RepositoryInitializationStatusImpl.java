/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.kernel.impl;

import org.fcrepo.kernel.api.RepositoryInitializationStatus;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of RepositoryInitializationStatus
 *
 * @author bbpennel
 */
@Component("repositoryInitializationStatus")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class RepositoryInitializationStatusImpl implements RepositoryInitializationStatus {
    private final AtomicBoolean initializationComplete = new AtomicBoolean(false);

    @Override
    public boolean isInitializationComplete() {
        return initializationComplete.get();
    }

    @Override
    public void setInitializationComplete(final boolean complete) {
        initializationComplete.set(complete);
    }
}
