/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.persistence.ocfl.impl;

import java.lang.reflect.ParameterizedType;


import org.fcrepo.kernel.api.operations.ResourceOperation;
import org.fcrepo.kernel.api.operations.ResourceOperationType;
import org.fcrepo.persistence.ocfl.api.Persister;

/**
 * A base abstract persister class
 *
 * @author dbernstein
 * @since 6.0.0
 */
public abstract class AbstractPersister<T extends ResourceOperation> implements Persister<T> {

    private ResourceOperationType resourceOperationType;
    private Class clazz;

    protected AbstractPersister(final ResourceOperationType resourceOperationType) {
        this.resourceOperationType = resourceOperationType;
    }

    @Override
    public boolean handle(final ResourceOperation operation) {
        //get the class of T
        final Class clazz = ((Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);

        //retrieve the interfaces of the operation class
        final Class[] interfaces = operation.getClass().getInterfaces();

        //ensure that at least one of them match.
        for (Class i : interfaces) {
            if (clazz.equals(i)) {
                //return true if the operation types match.
                return this.resourceOperationType.equals(operation.getType());
            }
        }
        return false;
    }
}
