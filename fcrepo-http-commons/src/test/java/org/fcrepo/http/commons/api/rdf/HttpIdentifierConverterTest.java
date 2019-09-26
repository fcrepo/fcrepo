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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

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

    @Test
    public void testBlankUri() {
        final String testUri = "";
        final String fedoraId = converter.convert(testUri);
        assertTrue(fedoraId.length() == 0);
    }

    @Test
    public void testRootUriWithTrailingSlash() {
        final String testUri = uriBase + "/";
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/", fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(testUri, httpUri);
    }

    @Test
    public void testRootUriWithoutTrailingSlash() {
        final String testUri = uriBase;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/", fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        // We also return the trailing slash.
        assertEquals(testUri + "/", httpUri);
    }

    @Test
    public void testFirstLevel() {
        final String baseUid = getUniqueId();
        final String testUri = uriBase + "/" + baseUid;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(testUri, httpUri);
    }

    @Test
    public void testFirstLevelWithAcl() {
        final String baseUid = getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_ACL;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    @Test
    public void testFirstLevelWithMetadata() {
        final String baseUid = getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_METADATA;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    @Test
    public void testFirstLevelWithVersions() {
        final String baseUid = getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_VERSIONS;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    @Test
    public void testFirstLevelWithMemento() {
        final String memento = "20190926133245";
        final String baseUid = getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_VERSIONS + memento;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    @Test
    public void testSecondLevel() {
        final String baseUid = getUniqueId() + "/" + getUniqueId();
        final String testUri = uriBase + "/" + baseUid;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(testUri, httpUri);
    }

    @Test
    public void testSecondLevelWithAcl() {
        final String baseUid = getUniqueId() + "/" + getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_ACL;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    @Test
    public void testSecondLevelWithMetadata() {
        final String baseUid = getUniqueId() + "/" + getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_METADATA;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    @Test
    public void testSecondLevelWithVersions() {
        final String baseUid = getUniqueId() + "/" + getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_VERSIONS;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    @Test
    public void testSecondLevelWithMemento() {
        final String memento = "20190926133245";
        final String baseUid = getUniqueId() + "/" + getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_VERSIONS + memento;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    @Test
    public void testItemWithDoubleAcl() {
        final String baseUid = getUniqueId();
        final String baseUrl = uriBase + "/" + baseUid;
        final String testUri = baseUrl + "/" + FCR_ACL + "/" + FCR_ACL;
        final String fedoraId = converter.convert(testUri);
        assertEquals("info:fedora/" + baseUid, fedoraId);
        final String httpUri = converter.reverse().convert(fedoraId);
        assertEquals(baseUrl, httpUri);
    }

    private static String getUniqueId() {
        return UUID.randomUUID().toString();
    }
}
