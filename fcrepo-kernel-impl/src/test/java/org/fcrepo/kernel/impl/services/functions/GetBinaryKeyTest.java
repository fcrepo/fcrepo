/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel.impl.services.functions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import javax.jcr.Node;
import javax.jcr.Property;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * <p>GetBinaryKeyTest class.</p>
 *
 * @author awoods
 */
public class GetBinaryKeyTest {

    @Mock
    private Node mockNode;

    @Mock
    private Node mockContent;

    @Mock
    private Property mockProp;

    @Mock
    private BinaryValue mockBin;

    private BinaryKey binaryKey;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        binaryKey = new BinaryKey("abc");
        when(mockBin.getKey()).thenReturn(binaryKey);
        when(mockProp.getBinary()).thenReturn(mockBin);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockProp);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
    }

    @Test
    public void testApply() {
        final GetBinaryKey testObj = new GetBinaryKey();
        assertEquals(binaryKey, testObj.apply(mockProp));
    }

}
