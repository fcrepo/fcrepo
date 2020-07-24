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

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

/**
 * @author bseeger
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class NonRdfSourceOperationFactoryImplTest {

    private NonRdfSourceOperationFactory factory;

    private FedoraId randomId;

    @Before
    public void setUp() throws Exception {
        factory = new NonRdfSourceOperationFactoryImpl();
        randomId = FedoraId.create(UUID.randomUUID().toString());
    }

    @Test
    public void testCreateInternalBuilder() throws Exception {
        final InputStream stream = IOUtils.toInputStream("This is some test data", "UTF-8");
        final NonRdfSourceOperationBuilder builder = factory.createInternalBinaryBuilder(randomId, stream);
        assertEquals(CreateNonRdfSourceOperationBuilderImpl.class, builder.getClass());
    }

    @Test
    public void testCreateExternalBuilder() {
        final URI externalURI = URI.create("http://example.com/some/location");
        final NonRdfSourceOperationBuilder builder = factory.createExternalBinaryBuilder(randomId, "PROXY",
                externalURI);
        assertEquals(CreateNonRdfSourceOperationBuilderImpl.class, builder.getClass());
    }
}
