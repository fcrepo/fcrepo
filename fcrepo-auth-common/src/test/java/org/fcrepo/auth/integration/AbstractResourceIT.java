/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.auth.integration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.parseInt;

import org.fcrepo.kernel.api.auth.ACLHandle;

/**
 * <p>Abstract AbstractResourceIT class.</p>
 *
 * @author gregjan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-container.xml")
public abstract class AbstractResourceIT {

    private Logger logger;

    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Configuration
    static class TestConfig {
        @Bean
        public Cache<String, Optional<ACLHandle>> authHandleCache() {
            return Caffeine.newBuilder().weakValues().expireAfterAccess(10, TimeUnit.SECONDS)
                    .maximumSize(10).build();
        }
    }

    private static final int SERVER_PORT = parseInt(Objects.requireNonNullElse(
            Strings.emptyToNull(System.getProperty("fcrepo.dynamic.test.port")), "8080"));

    private static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME +
            ":" + SERVER_PORT + "/";

    private static HttpClient client;

    public AbstractResourceIT() {
        client =
            HttpClientBuilder.create().setMaxConnPerRoute(5).setMaxConnTotal(
                    Integer.MAX_VALUE).build();
    }

    protected static HttpPost postObjMethod(final String pid) {
        return new HttpPost(serverAddress + pid);
    }

    protected static HttpPost postObjMethod(final String pid,
            final String query) {
        if (query.equals("")) {
            return new HttpPost(serverAddress + pid);
        }
        return new HttpPost(serverAddress + pid + "?" + query);
    }

    protected HttpResponse execute(final HttpUriRequest method)
            throws IOException {
        logger.debug("Executing: " + method.getMethod() + " to " +
                method.getURI());
        return client.execute(method);
    }

    protected int getStatus(final HttpUriRequest method)
            throws IOException {
        final HttpResponse response = execute(method);
        final int result = response.getStatusLine().getStatusCode();
        if (!(result > 199) || !(result < 400)) {
            logger.warn(EntityUtils.toString(response.getEntity()));
        }
        return result;
    }
}
