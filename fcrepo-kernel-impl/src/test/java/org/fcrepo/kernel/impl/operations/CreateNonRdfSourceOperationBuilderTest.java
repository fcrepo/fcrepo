/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.ExternalContent;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperation;
import org.fcrepo.kernel.api.operations.NonRdfSourceOperationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author bseeger
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CreateNonRdfSourceOperationBuilderTest {

    private InputStream stream;

    private NonRdfSourceOperationBuilder internalBuilder;

    final FedoraId resourceId = FedoraId.create("info:fedora/test-subject");

    @Mock
    private Transaction tx;

    @BeforeEach
    public void setUp() throws Exception {
        stream = IOUtils.toInputStream("This is some test data", "UTF-8");
        internalBuilder = new CreateNonRdfSourceOperationBuilderImpl(tx, resourceId, stream);
    }

    @Test
    public void testContent() throws Exception {
        final NonRdfSourceOperation op = internalBuilder.build();
        assertEquals(stream, op.getContentStream());
    }

    @Test
    public void testExternal() {
        final URI uri = URI.create("http://example.org/test/location");
        final String handling = ExternalContent.PROXY;
        final NonRdfSourceOperationBuilder builder =
                new CreateNonRdfSourceOperationBuilderImpl(tx, resourceId, handling, uri);
        final NonRdfSourceOperation op = builder.build();
        assertEquals(uri, op.getContentUri());
        assertEquals(handling, op.getExternalHandling());
    }

    @Test
    public void testMimeType() {
        final String mimeType = "text/plain";
        final NonRdfSourceOperation op = internalBuilder.mimeType(mimeType).build();
        assertEquals(mimeType, op.getMimeType());
    }

    @Test
    public void testFilename() {
        final String filename = "someFile.txt";
        final NonRdfSourceOperation op = internalBuilder.filename(filename).build();
        assertEquals(filename, op.getFilename());
    }

    @Test
    public void testSize() {
        final long filesize = 123l;
        final NonRdfSourceOperation op = internalBuilder.contentSize(filesize).build();
        assertEquals(filesize, op.getContentSize());
    }

    @Test
    public void testDigests() {
        final Collection<URI> digests = Stream.of("urn:sha1:1234abcd", "urn:md5:zyxw9876").map(URI::create)
                .collect(Collectors.toCollection(HashSet::new));
        final NonRdfSourceOperation op = internalBuilder.contentDigests(digests).build();
        assertEquals(digests, op.getContentDigests());
    }
}
