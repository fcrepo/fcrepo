/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.common.test.utils;

import org.mockito.Mockito;
import org.springframework.beans.factory.FactoryBean;

/**
 * Utility for creating Mockito mocks as Spring beans, used in context files.
 * @author bbpennel
 */
public class MockitoMockFactoryBean<T> implements FactoryBean<T> {

    private Class<T> type;

    public void setType(final Class<T> type) {
        this.type = type;
    }

    @Override
    public T getObject() {
        return Mockito.mock(type);
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
