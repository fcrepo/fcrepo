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
package org.fcrepo.http.commons.api.rdf;

import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_ID_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.UriBuilder;

import java.util.UUID;

/**
 * @author whikloj
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class HttpIdentifierConverterTest {

    private HttpIdentifierConverter converter;

    private static final String uriBase = "http://localhost:8080/some";

    private static final String uriTemplate = uriBase + "/{path: .*}";

    private UriBuilder uriBuilder;

    @Before
    public void setUp() {
        uriBuilder = UriBuilder.fromUri(uriTemplate);
        converter = new HttpIdentifierConverter(uriBuilder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankUri() {
        final String testUri = "";
        converter.toInternalId(testUri);
    }

    /**
     * Test that a blank string toDomain becomes a /
     */
    @Test
    public void testBlankToDomain() {
        final String testUri = "";
        final String fedoraUri = converter.toDomain(testUri);
        assertEquals(uriBase + "/", fedoraUri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankId() {
        final String testId = "";
        final String fedoraId = converter.toExternalId(testId);
    }

    @Test
    public void testinExternalDomainSuccess() {
        final String testURI = uriBase + "/someurl/thatWeWant";
        assertTrue(converter.inExternalDomain(testURI));
    }

    @Test
    public void testinExternalDomainFailure() {
        final String testURI = "http://someplace.com/whatHappened";
        assertFalse(converter.inExternalDomain(testURI));
    }

    @Test
    public void testinInternalDomainSuccess() {
        final String testID = FEDORA_ID_PREFIX + "/myLittleResource";
        assertTrue(converter.inInternalDomain(testID));
    }

    @Test
    public void testinInternalDomainFailure() {
        final String testID = "info:test/myLittleResource";
        assertFalse(converter.inInternalDomain(testID));
    }

    @Test
    public void testRootUriWithTrailingSlash() {
        final String testUri = uriBase + "/";
        final String fedoraId = converter.toInternalId(testUri);
        assertEquals(FEDORA_ID_PREFIX, fedoraId);
        final String httpUri = converter.toExternalId(fedoraId);
        assertEquals(testUri, httpUri);
    }

    @Test
    public void testRootUriWithoutTrailingSlash() {
        final String testUri = uriBase;
        final String fedoraId = converter.toInternalId(testUri);
        assertEquals(FEDORA_ID_PREFIX, fedoraId);
        final String httpUri = converter.toExternalId(fedoraId);
        // We also return the trailing slash.
        assertEquals(testUri + "/", httpUri);
    }

    @Test
    public void testFirstLevel() {
        final String baseUid = getUniqueId();
        final String testUri = uriBase + "/" + baseUid;
        final String fedoraId = converter.toInternalId(testUri);
        assertEquals(FEDORA_ID_PREFIX + "/" + baseUid, fedoraId);
        final String httpUri = converter.toExternalId(fedoraId);
        assertEquals(testUri, httpUri);
    }

    @Test
    public void testFirstLevelExternalPath() {
        final String baseUid = getUniqueId();
        final String testUri = "/" + baseUid;
        final String fedoraId = converter.toInternalId(converter.toDomain(testUri));
        assertEquals(FEDORA_ID_PREFIX + "/" + baseUid, fedoraId);
        final String httpUri = converter.toExternalId(fedoraId);
        assertEquals(uriBase + testUri, httpUri);
    }

    @Test
    public void testFirstLevelWithAcl() {
        final String testUri = "/" + getUniqueId() + "/" + FCR_ACL;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testFirstLevelWithMetadata() {
        final String baseUid = getUniqueId();
        final String testUri = "/" + baseUid + "/" + FCR_METADATA;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testFirstLevelWithVersions() {
        final String baseUid = getUniqueId();
        final String testUri = "/" + baseUid + "/" + FCR_VERSIONS;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testFirstLevelWithMemento() {
        final String memento = "20190926133245";
        final String baseUid = getUniqueId();
        final String testUri = "/" + baseUid + "/" + FCR_VERSIONS + "/" + memento;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testSecondLevel() {
        final String testUri = "/" + getUniqueId() + "/" + getUniqueId();
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testSecondLevelWithAcl() {
        final String baseUid = "/" + getUniqueId() + "/" + getUniqueId();
        final String testUri = baseUid + "/" + FCR_ACL;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testSecondLevelWithMetadata() {
        final String baseUid = "/" + getUniqueId() + "/" + getUniqueId();
        final String testUri = baseUid + "/" + FCR_METADATA;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testSecondLevelWithVersions() {
        final String baseUid = "/" + getUniqueId() + "/" + getUniqueId();
        final String testUri = baseUid + "/" + FCR_VERSIONS;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testSecondLevelWithMemento() {
        final String memento = "20190926133245";
        final String baseUid = "/" + getUniqueId() + "/" + getUniqueId();
        final String testUri = baseUid + "/" + FCR_VERSIONS + "/" + memento;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    @Test
    public void testItemWithDoubleAcl() {
        final String baseUid = getUniqueId();
        final String testUri = "/" + baseUid + "/" + FCR_ACL + "/" + FCR_ACL;
        final String external = uriBase + testUri;
        final String internal = FEDORA_ID_PREFIX + testUri;
        final String fedoraId = converter.toInternalId(external);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(external, httpUri);
    }

    /**
     * We decode on the way in, but don't re-encode on the way back out. This is the same as Fedora 5.1.0
     */
    @Test
    public void testWithEncodedColon() {
        final String externalOriginal = uriBase + "/some%3Atest";
        final String internal = FEDORA_ID_PREFIX + "/some:test";
        final String externalNew = uriBase + "/some:test";
        final String fedoraId = converter.toInternalId(externalOriginal);
        assertEquals(internal, fedoraId);
        final String httpUri = converter.toExternalId(internal);
        assertEquals(externalNew, httpUri);
    }


    @Test
    public void testBlankPathToId() {
        final String testUri = "";
        final FedoraId fedoraUri = converter.pathToInternalId(testUri);
        assertEquals(FedoraId.getRepositoryRootId(), fedoraUri);
    }

    @Test
    public void testSinglePathToId() {
        final String externalOriginal = "/object";
        final FedoraId internal = FedoraId.create("object");
        final FedoraId id = converter.pathToInternalId(externalOriginal);
        assertEquals(internal, id);
        // Without leading slash
        final String externalOriginal2 = "object";
        final FedoraId id2 = converter.pathToInternalId(externalOriginal2);
        assertEquals(internal, id2);
    }

    @Test
    public void testDoublePathToId() {
        final String externalOriginal = "/object/child";
        final FedoraId internal = FedoraId.create("object", "child");
        final FedoraId id = converter.pathToInternalId(externalOriginal);
        assertEquals(internal, id);
        // Without leading slash
        final String externalOriginal2 = "object/child";
        final FedoraId id2 = converter.pathToInternalId(externalOriginal2);
        assertEquals(internal, id2);
    }

    /**
     * Utility function to get a UUID.
     * @return a UUID.
     */
    private static String getUniqueId() {
        return UUID.randomUUID().toString();
    }
}
