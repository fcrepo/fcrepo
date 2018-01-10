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
package org.fcrepo.kernel.modeshape;

import static java.util.Arrays.asList;
import static org.fcrepo.kernel.api.utils.ContentDigest.asURI;
import static org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM.SHA1;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.fcrepo.integration.kernel.modeshape.AbstractIT;
import org.fcrepo.kernel.api.FedoraRepository;
import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.services.BinaryService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author bbpennel
 */
@ContextConfiguration({ "/spring-test/repo.xml" })
public class LocalFileBinaryIT extends AbstractIT {

    private static final String EXPECTED_CONTENT = "test content";

    private static final String CONTENT_SHA1 = "1eebdf4fdc9fc7bf283031b93f9aef3338de9052";

    @Inject
    private FedoraRepository repo;

    @Inject
    private BinaryService binaryService;

    private FedoraSession session;

    private File contentFile;

    private String mimeType;

    private String dsId;

    @Before
    public void setup() throws Exception {
        session = repo.login();

        contentFile = File.createTempFile("file", ".txt");
        IOUtils.write(EXPECTED_CONTENT, new FileOutputStream(contentFile), "UTF-8");
        mimeType = makeMimeType(contentFile);

        dsId = makeDsId();
    }

    @After
    public void after() throws Exception {
        session.expire();
    }

    @Test
    public void testDatastream() throws Exception {
        binaryService.findOrCreate(session, dsId)
                .setContent(null, mimeType, null, null, null);

        session.commit();

        final FedoraBinary ds = binaryService.findOrCreate(session, dsId);

        assertEquals(EXPECTED_CONTENT.length(), ds.getContentSize());
        assertEquals(EXPECTED_CONTENT, contentString(ds));

        assertEquals(mimeType, ds.getMimeType());
    }

    @Test
    public void testDatastreamWithMimeType() throws Exception {
        final String mimeTypeWithDsType = mimeType + ";  mime-type=\"text/plain\"";

        binaryService.findOrCreate(session, dsId)
                .setContent(null, mimeTypeWithDsType, null, null, null);

        session.commit();

        final FedoraBinary ds = binaryService.findOrCreate(session, dsId);

        assertEquals(EXPECTED_CONTENT.length(), ds.getContentSize());
        assertEquals(EXPECTED_CONTENT, contentString(ds));

        assertEquals("text/plain", ds.getMimeType());
    }

    @Test
    public void testWithValidChecksum() throws Exception {
        binaryService.findOrCreate(session, dsId)
                .setContent(null, mimeType, sha1Set(CONTENT_SHA1), null, null);

        session.commit();

        final FedoraBinary ds = binaryService.findOrCreate(session, dsId);

        assertEquals(EXPECTED_CONTENT, contentString(ds));
    }

    @Test(expected = InvalidChecksumException.class)
    public void testWithInvalidChecksum() throws Exception {
        binaryService.findOrCreate(session, dsId)
                .setContent(null, mimeType, sha1Set("badsum"), null, null);
    }

    private String makeMimeType(final File file) {
        return "message/external-body; access-type=LOCAL-FILE; LOCAL-FILE=\"" +
                file.toURI().toString() + "\"";
    }

    private String makeDsId() {
        return "/ds_" + UUID.randomUUID().toString();
    }

    private Set<URI> sha1Set(final String checksum) {
        return new HashSet<>(asList(asURI(SHA1.algorithm, checksum)));
    }

    private String contentString(final FedoraBinary ds) throws Exception {
        return IOUtils.toString(ds.getContent(), "UTF-8");
    }
}
