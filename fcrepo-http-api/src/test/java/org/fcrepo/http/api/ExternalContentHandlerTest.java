/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import static org.fcrepo.kernel.api.RdfLexicon.EXTERNAL_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.core.Link;
import org.fcrepo.kernel.api.exception.ExternalMessageBodyException;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author bbpennel
 */
public class ExternalContentHandlerTest {

    @Test
    public void testExplicitSizeDoesNotReadMissingFile() {
        final var missingFile = Path.of("/this/path/should/not/exist").toUri();
        final var link = Link.fromUri(missingFile)
                .rel(EXTERNAL_CONTENT.toString())
                .param("handling", "proxy")
                .param("type", "text/plain")
                .param("size", "123")
                .build()
                .toString();

        final var handler = new ExternalContentHandler(link);

        assertEquals(123L, handler.getContentSize());
        assertEquals("text/plain", handler.getContentType());
        assertEquals("proxy", handler.getHandling());
        assertEquals(handler.getURI().toString(), handler.getURL());
        assertEquals(missingFile, handler.getURI());
        assertTrue(handler.isProxy());
        assertFalse(handler.isCopy());
        assertFalse(handler.isRedirect());
    }

    @Test
    public void testExplicitSizeOverridesHttpContentLength() throws Exception {
        final var requests = new AtomicInteger();
        final var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/binary", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.getResponseHeaders().add("Content-Length", "999");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            final var uri = "http://localhost:" + server.getAddress().getPort() + "/binary";
            final var link = Link.fromUri(uri)
                    .rel(EXTERNAL_CONTENT.toString())
                    .param("handling", "proxy")
                    .param("size", "123")
                    .build()
                    .toString();

            final var handler = new ExternalContentHandler(link);

            assertEquals(123L, handler.getContentSize());
            assertEquals("text/plain", handler.getContentType());
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testHttpContentDetailsAndFetchContentWhenSizeParamNotProvided() throws Exception {
        final var requests = new AtomicInteger();
        final var content = "hello over http";
        final var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/binary", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.getResponseHeaders().add("Content-Length", String.valueOf(content.length()));
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                final var body = content.getBytes();
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        try {
            final var uri = URI.create("http://localhost:" + server.getAddress().getPort() + "/binary");
            final var link = Link.fromUri(uri)
                    .rel(EXTERNAL_CONTENT.toString())
                    .param("handling", "proxy")
                    .build()
                    .toString();

            final var handler = new ExternalContentHandler(link);

            assertEquals(content.length(), handler.getContentSize());
            assertEquals("text/plain", handler.getContentType());
            assertEquals("proxy", handler.getHandling());
            assertEquals(uri.toString(), handler.getURL());
            assertEquals(uri, handler.getURI());
            assertTrue(handler.isProxy());
            assertFalse(handler.isCopy());
            assertFalse(handler.isRedirect());
            try (final var stream = handler.fetchExternalContent()) {
                assertEquals(content, new String(stream.readAllBytes()));
            }
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testMissingSizeDefaultsToNegativeOneWhenNotRetrieved() throws Exception {
        final var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/binary", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            final var uri = "http://localhost:" + server.getAddress().getPort() + "/binary";
            final var link = Link.fromUri(uri)
                    .rel(EXTERNAL_CONTENT.toString())
                    .param("handling", "proxy")
                    .build()
                    .toString();

            final var handler = new ExternalContentHandler(link);

            assertEquals(-1L, handler.getContentSize());
            assertEquals("text/plain", handler.getContentType());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testRealSizeRetrievedWhenSizeParamNotProvided() throws Exception {
        final var file = Files.createTempFile("external-content-handler", ".txt");
        final var content = "hello world";
        Files.writeString(file, content);

        try {
            final var link = Link.fromUri(file.toUri())
                    .rel(EXTERNAL_CONTENT.toString())
                    .param("handling", "copy")
                    .param("type", "text/plain")
                    .build()
                    .toString();

            final var handler = new ExternalContentHandler(link);

            assertEquals(Files.size(file), handler.getContentSize());
            assertEquals("text/plain", handler.getContentType());
            assertEquals("copy", handler.getHandling());
            assertTrue(handler.isCopy());
            assertFalse(handler.isProxy());
            assertFalse(handler.isRedirect());
            try (final var stream = handler.fetchExternalContent()) {
                assertEquals(content, new String(stream.readAllBytes()));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testRedirectHandlingMethods() {
        final var fileUri = Path.of("/tmp/redirect-test").toUri();
        final var link = Link.fromUri(fileUri)
                .rel(EXTERNAL_CONTENT.toString())
                .param("handling", "redirect")
                .param("type", "text/plain")
                .param("size", "12")
                .build()
                .toString();

        final var handler = new ExternalContentHandler(link);

        assertEquals("redirect", handler.getHandling());
        assertTrue(handler.isRedirect());
        assertFalse(handler.isProxy());
        assertFalse(handler.isCopy());
    }

    @Test
    public void testInvalidExplicitSizeRejected() {
        final var link = Link.fromUri("https://example.com/file")
                .rel(EXTERNAL_CONTENT.toString())
                .param("handling", "proxy")
                .param("size", "abc")
                .build()
                .toString();

        final var ex = assertThrows(ExternalMessageBodyException.class, () -> new ExternalContentHandler(link));

        assertTrue(ex.getMessage().contains("'size' parameter must be a non-negative long"));
    }

    @Test
    public void testInvalidNegativeSizeRejected() {
        final var link = Link.fromUri("https://example.com/file")
                .rel(EXTERNAL_CONTENT.toString())
                .param("handling", "proxy")
                .param("size", "-495")
                .build()
                .toString();

        final var ex = assertThrows(ExternalMessageBodyException.class, () -> new ExternalContentHandler(link));

        assertTrue(ex.getMessage().contains("'size' parameter must be a non-negative long"));
    }
}
