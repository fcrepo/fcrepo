/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.apache.commons.lang3.NotImplementedException;
import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for UpdateNonRdfSourceHeadersOperationImpl
 *
 * @author bbpennel
 */
public class UpdateNonRdfSourceHeadersOperationImplTest {

    private Transaction tx;
    private FedoraId resourceId;
    private UpdateNonRdfSourceHeadersOperationImpl operation;

    @BeforeEach
    public void setup() {
        tx = mock(Transaction.class);
        resourceId = FedoraId.create("info:fedora/test");
        operation = new UpdateNonRdfSourceHeadersOperationImpl(tx, resourceId);
    }

    @Test
    public void testConstruction() {
        assertEquals(resourceId, operation.getResourceId());
        assertEquals(tx, operation.getTransaction());
        assertNull(operation.getMimeType());
        assertNull(operation.getFilename());
    }

    @Test
    public void testSetMimeType() throws Exception {
        final String mimeType = "text/plain";
        operation.setMimeType(mimeType);

        assertEquals(mimeType, operation.getMimeType());
    }

    @Test
    public void testSetFilename() throws Exception {
        final String filename = "test.txt";
        operation.setFilename(filename);
        assertEquals(filename, operation.getFilename());
    }

    @Test
    public void testGetContentStream() {
        assertThrows(UnsupportedOperationException.class, () -> {
            operation.getContentStream();
        });
    }

    @Test
    public void testGetExternalHandling() {
        assertThrows(UnsupportedOperationException.class, () -> {
            operation.getExternalHandling();
        });
    }

    @Test
    public void testGetContentUri() {
        assertThrows(UnsupportedOperationException.class, () -> {
            operation.getContentUri();
        });
    }

    @Test
    public void testGetContentDigests() {
        assertThrows(NotImplementedException.class, () -> {
            operation.getContentDigests();
        });
    }

    @Test
    public void testGetContentSize() {
        assertThrows(NotImplementedException.class, () -> {
            operation.getContentSize();
        });
    }
}