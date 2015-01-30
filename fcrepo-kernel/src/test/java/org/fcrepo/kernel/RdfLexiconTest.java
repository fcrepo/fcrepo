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
package org.fcrepo.kernel;

import static org.fcrepo.kernel.RdfLexicon.PREMIS_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.fcrepo.kernel.RdfLexicon.isManagedPredicateURI;
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
        assertTrue( isManagedPredicateURI.apply( PREMIS_NAMESPACE + "hasSize") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "primaryType") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryCustomRepName") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryIdentifierStability") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryJcrRepositoryName") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryJcrRepositoryVendor") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryJcrRepositoryVendorUrl") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryJcrRepositoryVersion") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryJcrSpecificationName") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryJcrSpecificationVersion") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryLevel1Supported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE + "repositoryLevel2Supported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementAutocreatedDefinitionsSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementInheritance") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementMultipleBinaryPropertiesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementMultivaluedPropertiesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementOrderableChildNodesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementOverridesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementPrimaryItemNameSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementPropertyTypes") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementResidualDefinitionsSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementSameNameSiblingsSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementUpdateInUseSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryNodeTypeManagementValueConstraintsSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionAccessControlSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionActivitiesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionBaselinesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionJournaledObservationSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionLifecycleSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionLockingSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionNodeAndPropertyWithSameNameSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionNodeTypeManagementSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionObservationSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionQuerySqlSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionRetentionSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionShareableNodesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionSimpleVersioningSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionTransactionsSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionUnfiledContentSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionUpdateMixinNodeTypesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionUpdatePrimaryNodeTypeSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionVersioningSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionWorkspaceManagementSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionXmlExportSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryOptionXmlImportSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryQueryFullTextSearchSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryQueryJoins") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryQueryStoredQueriesSupported") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryQueryXpathDocOrder") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryQueryXpathPosIndex") );
        assertTrue( isManagedPredicateURI.apply( REPOSITORY_NAMESPACE +
                    "repositoryWriteSupported") );
    }
    @Test
    public void otherPredicatesAreNotManaged() {
        assertTrue( !isManagedPredicateURI.apply( "http://purl.org/dc/elements/1.1/title") );
    }
}
