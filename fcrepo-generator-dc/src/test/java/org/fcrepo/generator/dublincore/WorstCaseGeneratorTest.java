/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.generator.dublincore;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.InputStream;

import javax.jcr.Node;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.fcrepo.generator.util.OaiDublinCore;
import org.junit.Before;
import org.junit.Test;

public class WorstCaseGeneratorTest {

    private WorstCaseGenerator testObj;

    private JAXBContext context;

    @Before
    public void setUp() throws JAXBException {
        testObj = new WorstCaseGenerator();
        context = JAXBContext.newInstance(OaiDublinCore.class);

    }

    @Test
    public void testGetStream() throws Exception {
        final Node mockNode = mock(Node.class);
        final InputStream out = testObj.getStream(mockNode);
        final OaiDublinCore actual =
                (OaiDublinCore) context.createUnmarshaller().unmarshal(out);
        assertNotNull(actual);
    }
}
