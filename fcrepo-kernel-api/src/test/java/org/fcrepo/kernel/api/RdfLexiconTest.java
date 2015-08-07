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
package org.fcrepo.kernel.api;

import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.api.RdfLexicon.isManagedPredicateURI;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * <p>RdfLexiconTest class.</p>
 *
 * @author ajs6f
 */
public class RdfLexiconTest {

    @Test
    public void repoPredicatesAreManaged() {
        assertTrue( isManagedPredicateURI.test( PREMIS_NAMESPACE + "hasSize") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "primaryType") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryCustomRepName") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryIdentifierStability") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryJcrRepositoryName") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryJcrRepositoryVendor") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryJcrRepositoryVendorUrl") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryJcrRepositoryVersion") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryJcrSpecificationName") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryJcrSpecificationVersion") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryLevel1Supported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE + "repositoryLevel2Supported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementAutocreatedDefinitionsSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementInheritance") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementMultipleBinaryPropertiesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementMultivaluedPropertiesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementOrderableChildNodesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementOverridesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementPrimaryItemNameSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementPropertyTypes") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementResidualDefinitionsSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementSameNameSiblingsSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementUpdateInUseSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementValueConstraintsSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionAccessControlSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionActivitiesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionBaselinesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionJournaledObservationSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionLifecycleSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionLockingSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionNodeAndPropertyWithSameNameSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionNodeTypeManagementSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionObservationSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionQuerySqlSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionRetentionSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionShareableNodesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionSimpleVersioningSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionTransactionsSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionUnfiledContentSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionUpdateMixinNodeTypesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionUpdatePrimaryNodeTypeSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionVersioningSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionWorkspaceManagementSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionXmlExportSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryOptionXmlImportSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryQueryFullTextSearchSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryQueryJoins") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryQueryStoredQueriesSupported") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryQueryXpathDocOrder") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryQueryXpathPosIndex") );
        assertTrue( isManagedPredicateURI.test( REPOSITORY_NAMESPACE +
                    "repositoryWriteSupported") );
    }
    @Test
    public void otherPredicatesAreNotManaged() {
        assertTrue( !isManagedPredicateURI.test( "http://purl.org/dc/elements/1.1/title") );
    }
}
