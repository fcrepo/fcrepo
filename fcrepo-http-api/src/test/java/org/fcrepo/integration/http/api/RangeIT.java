/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.integration.http.api;

import static org.fcrepo.kernel.api.RdfLexicon.NON_RDF_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Test of Range related requests.
 * @author whikloj
 */
@TestExecutionListeners(
        listeners = { TestIsolationExecutionListener.class },
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class RangeIT extends AbstractResourceIT{

    @Test
    public void testPartialRange() throws IOException {
        final var pid = getRandomUniqueId();

        final String contentLength = createDatastreamAndGetLength(pid);
        final var getRange = getObjMethod(pid);
        final var halfContentLength = Math.round((float) Integer.parseInt(contentLength) / 2);
        getRange.addHeader("Range", "bytes=0-" + halfContentLength);
        try (final var res = execute(getRange)) {
            assertEquals(206, res.getStatusLine().getStatusCode());
            checkForLinkHeader(res, NON_RDF_SOURCE.getURI(), "type");
            assertEquals(String.valueOf(halfContentLength + 1), res.getFirstHeader("Content-Length").getValue());
            assertEquals(
                    "bytes 0-" + halfContentLength + "/" + contentLength,
                    res.getFirstHeader("Content-Range").getValue()
            );
        }
    }

    @Test
    public void testRangeAsContentLength() throws IOException {
        final var pid = getRandomUniqueId();
        final String contentLength = createDatastreamAndGetLength(pid);
        final var returnRange = "0-" + (Integer.parseInt(contentLength) - 1);
        final var requestRange = "0-" + contentLength;
        final var getRange = getObjMethod(pid);
        getRange.addHeader("Range", "bytes=" + requestRange);
        try (final var res = execute(getRange)) {
            assertEquals(206, res.getStatusLine().getStatusCode());
            checkForLinkHeader(res, NON_RDF_SOURCE.getURI(), "type");
            assertEquals("bytes " + returnRange + "/" + contentLength, res.getFirstHeader("Content-Range").getValue());
            assertEquals(contentLength, res.getFirstHeader("Content-Length").getValue());
        }
    }

    @Test
    public void testRangeTooLong() throws IOException {
        final var pid = getRandomUniqueId();
        final String contentLength = createDatastreamAndGetLength(pid);
        final var getRangeVal = "0-" + (Integer.parseInt(contentLength) + 1);
        final var returnRangeVal = "0-" + (Integer.parseInt(contentLength) - 1);
        final var getRange = getObjMethod(pid);
        getRange.addHeader("Range", "bytes=" + getRangeVal);
        try (final var res = execute(getRange)) {
            assertEquals(206, res.getStatusLine().getStatusCode());
            checkForLinkHeader(res, NON_RDF_SOURCE.getURI(), "type");
            assertEquals(
                    "bytes " + returnRangeVal + "/" + contentLength,
                    res.getFirstHeader("Content-Range").getValue()
            );
            assertEquals(contentLength, res.getFirstHeader("Content-Length").getValue());
        }
    }

    @Test
    public void testLastHalf() throws IOException {
        final var pid = getRandomUniqueId();
        final String contentLength = createDatastreamAndGetLength(pid);
        final int contentLengthInt = Integer.parseInt(contentLength);
        final var halfContentLength = contentLengthInt / 2;
        final var rangeEnd = contentLengthInt - 1;
        final var rangeStart = rangeEnd - halfContentLength + 1;
        final var range = "-" + halfContentLength;
        final var getRange = getObjMethod(pid);
        getRange.addHeader("Range", "bytes=" + range);
        try (final var res = execute(getRange)) {
            assertEquals(206, res.getStatusLine().getStatusCode());
            checkForLinkHeader(res, NON_RDF_SOURCE.getURI(), "type");
            assertEquals(String.valueOf(halfContentLength), res.getFirstHeader("Content-Length").getValue());
            assertEquals(
                    "bytes " + rangeStart + "-" + rangeEnd + "/" + contentLength,
                    res.getFirstHeader("Content-Range").getValue()
            );
        }
    }

    private String createDatastreamAndGetLength(final String pid) throws IOException {
        final var randomLength = (int)((Math.random() + 0.1) * 100);
        final RandomStringGenerator generator = RandomStringGenerator.builder()
                .withinRange('a', 'z')
                .get();
        final String content = generator.generate(randomLength);
        createDatastream(pid, content);
        try (final var res = execute(getObjMethod(pid))) {
            assertEquals(200, res.getStatusLine().getStatusCode());
            checkForLinkHeader(res, NON_RDF_SOURCE.getURI(), "type");
            return res.getFirstHeader("Content-Length").getValue();
        }
    }
}
