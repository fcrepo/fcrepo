/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * @author mikejritter
 */
@TestExecutionListeners(
    listeners = { TestIsolationExecutionListener.class },
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class StreamingBaseHtmlProviderIT extends AbstractResourceIT {

    private static final String TEXT_HTML = "text/html";

    @Test
    public void testGetContainer() throws IOException {
        final String id = getRandomUniqueId();
        final HttpPut put = putObjMethod(id);
        executeAndClose(put);

        final HttpGet get = getObjMethod(id);
        get.addHeader("Accept", TEXT_HTML);

        try (final var response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testGetEncodedCharacters() throws IOException {
        final String id = "/some%3Atest";
        final HttpPut put = putObjMethod(id);
        executeAndClose(put);

        final HttpGet get = getObjMethod(id);
        get.addHeader("Accept", TEXT_HTML);

        try (final var response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testGetContainerUTFCharacters() throws IOException {
        final String id = "/ÅŤéșţ!";
        final HttpPut put = putObjMethod(id);
        executeAndClose(put);

        final HttpGet get = getObjMethod(id);
        get.addHeader("Accept", TEXT_HTML);

        try (final var response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
        }
    }

    @Test
    public void testGetContainerOtherCharacters() throws IOException {
        final String id = "/a+&test";
        final HttpPut put = putObjMethod(id);
        executeAndClose(put);

        final HttpGet get = getObjMethod(id);
        get.addHeader("Accept", TEXT_HTML);

        try (final var response = execute(get)) {
            assertEquals(OK.getStatusCode(), getStatus(response));
        }
    }

}
