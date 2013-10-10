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

package org.fcrepo.kernel.rdf;

import static com.google.common.collect.Iterators.any;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static org.fcrepo.kernel.utils.NamespaceTools.getNamespaceRegistry;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.utils.NamespaceTools;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.modeshape.jcr.api.NamespaceRegistry;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Predicate;
import com.hp.hpl.jena.graph.Triple;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NamespaceTools.class})
public class NamespaceContextTest {

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    @Mock
    private Session mockSession;

    private final static String testUri = "http://example.com";

    private final static String prefix = "jcr";

    @Test
    public void testConstructor() throws RepositoryException {
        initMocks(this);
        mockStatic(NamespaceTools.class);
        Mockito.when(mockNamespaceRegistry.getPrefixes()).thenReturn(
                new String[] {prefix});
        Mockito.when(mockNamespaceRegistry.getURI(prefix)).thenReturn(testUri);
        PowerMockito.when(getNamespaceRegistry(mockSession))
                .thenReturn(mockNamespaceRegistry);
        assertTrue(any(new NamespaceContext(mockSession).context(),
                hasTestUriAsObject));
    }

    private static Predicate<Triple> hasTestUriAsObject =
        new Predicate<Triple>() {

            @Override
            public boolean apply(final Triple t) {
                return t.objectMatches(createLiteral(testUri));
            }
        };
}
