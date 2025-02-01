/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.fcrepo.kernel.api.models.ExternalContent.PROXY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.apache.commons.io.IOUtils;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * @author bbpennel
 */
public class UpdateNonRdfSourceOperationBuilderTest {

    private final FedoraId RESOURCE_ID = FedoraId.create("info:fedora/test-subject");

    private final String MIME_TYPE = "text/plain";

    private final String FILENAME = "someFile.txt";

    private final long FILESIZE = 123L;

    private final Collection<URI> DIGESTS = asList(URI.create("urn:sha1:1234abcd"), URI.create("urn:md5:zyxw9876"));

    @Mock
    private Transaction tx;

    @Test
    public void buildExternalBinary() {
        final URI uri = URI.create("http://example.org/test/location");

        final NonRdfSourceOperationBuilder builder =
                new UpdateNonRdfSourceOperationBuilder(tx, RESOURCE_ID, PROXY, uri);
        builder.mimeType(MIME_TYPE)
                .contentDigests(DIGESTS)
                .contentSize(FILESIZE)
                .filename(FILENAME);

        final NonRdfSourceOperation op = builder.build();
        assertEquals(uri, op.getContentUri());
        assertEquals(PROXY, op.getExternalHandling());
        assertEquals(MIME_TYPE, op.getMimeType());
        assertEquals(FILENAME, op.getFilename());
        assertEquals(FILESIZE, op.getContentSize());
        assertEquals(DIGESTS, op.getContentDigests());
        assertNull(op.getContentStream());
    }

    @Test
    public void buildInternalBinary() throws Exception {
        final String contentString = "This is some test data";
        final InputStream stream = toInputStream(contentString, UTF_8);

        final NonRdfSourceOperationBuilder builder =
                new UpdateNonRdfSourceOperationBuilder(tx, RESOURCE_ID, stream);
        builder.mimeType(MIME_TYPE)
                .contentDigests(DIGESTS)
                .contentSize(FILESIZE)
                .filename(FILENAME);

        final NonRdfSourceOperation op = builder.build();
        assertEquals(contentString, IOUtils.toString(op.getContentStream(), UTF_8));
        assertEquals(MIME_TYPE, op.getMimeType());
        assertEquals(FILENAME, op.getFilename());
        assertEquals(FILESIZE, op.getContentSize());
        assertEquals(DIGESTS, op.getContentDigests());
        assertNull(op.getExternalHandling());
        assertNull(op.getContentUri());
    }
}
