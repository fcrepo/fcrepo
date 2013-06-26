/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.services.functions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * @todo Add Documentation.
 * @author Benjamin Armintor
 * @date Apr 3, 2013
 */
public class GetBinaryKeyTest {

    /**
     * @todo Add Documentation.
     */
    @Test
    public void testApply() throws LoginException, RepositoryException {
        final Node mockNode = mock(Node.class);
        final Node mockContent = mock(Node.class);
        final Property mockProp = mock(Property.class);
        final BinaryValue mockBin = mock(BinaryValue.class);
        final BinaryKey binaryKey = new BinaryKey("abc");
        when(mockBin.getKey()).thenReturn(binaryKey);
        when(mockProp.getBinary()).thenReturn(mockBin);
        when(mockContent.getProperty(JCR_DATA)).thenReturn(mockProp);
        when(mockNode.getNode(JCR_CONTENT)).thenReturn(mockContent);
        final GetBinaryKey testObj = new GetBinaryKey();
        assertEquals(binaryKey, testObj.apply(mockProp));
    }

}
