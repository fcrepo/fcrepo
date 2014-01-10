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

package org.fcrepo.kernel;

import static org.fcrepo.kernel.RdfLexicon.PREMIS_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.isManagedPredicateURI;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RdfLexiconTest {

    @Test
    public void repoPredicatesAreManaged() {
        assertTrue( isManagedPredicateURI.apply( PREMIS_NAMESPACE + "hasSize") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "primaryType") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repository/custom.rep.name") );
    }
    @Test
    public void otherPredicatesAreNotManaged() {
        assertTrue( !isManagedPredicateURI.apply( "http://purl.org/dc/elements/1.1/title") );
    }
}
