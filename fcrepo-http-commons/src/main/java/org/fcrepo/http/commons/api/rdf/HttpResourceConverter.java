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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.replaceOnce;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_ACL;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_METADATA;
import static org.fcrepo.kernel.api.FedoraTypes.FCR_VERSIONS;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.ws.rs.core.UriBuilder;

import com.google.common.base.Converter;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.http.commons.session.HttpSession;
import org.fcrepo.kernel.api.FedoraTransaction;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.glassfish.jersey.uri.UriTemplate;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

/**
 * Convert between Jena Resources and JCR Nodes using a JAX-RS UriBuilder to mediate the
 * URI translation.
 *
 * @author cabeer
 * @since 10/5/14
 */
public class HttpResourceConverter extends IdentifierConverter<Resource,FedoraResource> {

    private static final Logger LOGGER = getLogger(HttpResourceConverter.class);

    // Regex pattern which decomposes a http resource uri into components
    // The first group determines if it is an fcr:metadata non-rdf source.
    // The second group determines if the path is for a memento or timemap.
    // The third group allows for a memento identifier.
    // The fourth group for allows ACL.
    // The fifth group allows for any hashed suffixes.
    private final static Pattern FORWARD_COMPONENT_PATTERN = Pattern.compile(
            ".*?(/" + FCR_METADATA + ")?(/" + FCR_VERSIONS + "(/\\d{14})?)?(/" + FCR_ACL + ")?(\\#\\S+)?$");

    protected List<Converter<String, String>> translationChain;

    private final FedoraTransaction transaction;
    private final UriBuilder uriBuilder;

    protected Converter<String, String> forward = identity();
    protected Converter<String, String> reverse = identity();

    private final UriTemplate uriTemplate;
    private final boolean batch;

    /**
     * Create a new identifier converter within the given session with the given URI template
     * @param session the session
     * @param uriBuilder the uri builder
     */
    public HttpResourceConverter(final HttpSession session,
                                 final UriBuilder uriBuilder) {

        this.transaction = session.getFedoraTransaction();
        this.uriBuilder = uriBuilder;
        this.batch = session.isBatchSession();
        this.uriTemplate = new UriTemplate(uriBuilder.toTemplate());
    }

    private UriBuilder uriBuilder() {
        return UriBuilder.fromUri(uriBuilder.toTemplate());
    }

    @Override
    protected FedoraResource doForward(final Resource resource) {
        //@TODO Implement this
        return null;
    }

    @Override
    protected Resource doBackward(final FedoraResource resource) {
            //@TODO Implement this
            return null;
    }

    @Override
    public boolean inDomain(final Resource resource) {
        //@TODO Implement this
        return false;
    }

    @Override
    public Resource toDomain(final String path) {
        //@TODO Implement this
        return null;
    }

    @Override
    public String asString(final Resource resource) {
        //@TODO Implement this
        return null;
    }

    protected ApplicationContext getApplicationContext() {
        return getCurrentWebApplicationContext();
    }

    /**
     * Translate the current transaction into the identifier
     */
    static class TransactionIdentifierConverter extends Converter<String, String> {
        public static final String TX_PREFIX = "tx:";

        private final FedoraTransaction transaction;
        private final boolean batch;

        public TransactionIdentifierConverter(final FedoraTransaction transaction, final boolean batch) {
            this.transaction = transaction;
            this.batch = batch;
        }

        @Override
        protected String doForward(final String path) {

            if (path.contains(TX_PREFIX) && !path.contains(txSegment())) {
                throw new RepositoryRuntimeException("Path " + path
                        + " is not in current transaction " +  transaction.getId());
            }

            return replaceOnce(path, txSegment(), EMPTY);
        }

        @Override
        protected String doBackward(final String path) {
            return txSegment() + path;
        }

        private String txSegment() {
            return batch ? "/" + TX_PREFIX + transaction.getId() : EMPTY;
        }
    }

    private boolean isRootWithoutTrailingSlash(final Resource resource) {
        final Map<String, String> values = new HashMap<>();

        return uriTemplate.match(resource.getURI() + "/", values) && values.containsKey("path") &&
            values.get("path").isEmpty();
    }
}
