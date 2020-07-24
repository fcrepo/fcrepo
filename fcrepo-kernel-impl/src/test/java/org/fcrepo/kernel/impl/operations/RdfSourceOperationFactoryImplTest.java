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
package org.fcrepo.kernel.impl.operations;

import static org.junit.Assert.assertEquals;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.RdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.RdfSourceOperationFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

/**
 * @author bseeger
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RdfSourceOperationFactoryImplTest {

    private RdfSourceOperationFactory factory;
    private FedoraId randomId;


    @Before
    public void setUp() {
        factory = new RdfSourceOperationFactoryImpl();
        randomId = FedoraId.create(UUID.randomUUID().toString());
    }

    @Test
    public void testCreateBuilder() {
        final String model = "some-interaction-model";
        final RdfSourceOperationBuilder builder = factory.createBuilder(randomId, model);
        assertEquals(CreateRdfSourceOperationBuilderImpl.class, builder.getClass());
    }
}
