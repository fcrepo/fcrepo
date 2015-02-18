/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.rdf.impl;

import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;

import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;

import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.impl.identifiers.HashConverter;
import org.fcrepo.kernel.impl.identifiers.NamespaceConverter;
import org.fcrepo.kernel.impl.identifiers.NodeResourceConverter;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.List;

/**
 * A very simple {@link IdentifierConverter} which translates JCR paths into
 * un-dereference-able Fedora subjects (by replacing JCR-specific names with
 * Fedora names). Should not be used except in "embedded" deployments in which
 * no publication of translated identifiers is expected!
 *
 * @author barmintor
 * @author ajs6f
 * @since May 15, 2013
 */
public class DefaultIdentifierTranslator extends IdentifierConverter<Resource, FedoraResource> {


    private static final NodeResourceConverter nodeResourceConverter = new NodeResourceConverter();

    /**
     * Default namespace to use for node URIs
     */
    public static final String RESOURCE_NAMESPACE = "info:fedora/";
    private final Session session;

    /**
     * Construct the graph with a placeholder context resource
     */
    public DefaultIdentifierTranslator(final Session session) {
        this.session = session;
        setTranslationChain();
    }


    protected Converter<String, String> forward = identity();
    protected Converter<String, String> reverse = identity();

    private void setTranslationChain() {

        for (final Converter<String, String> t : minimalTranslationChain) {
            forward = forward.andThen(t);
        }
        for (final Converter<String, String> t : Lists.reverse(minimalTranslationChain)) {
            reverse = reverse.andThen(t.reverse());
        }
    }


    @SuppressWarnings("unchecked")
    private static final List<Converter<String, String>> minimalTranslationChain =
            newArrayList((Converter<String, String>) new NamespaceConverter(),
                    (Converter<String, String>) new HashConverter()
            );

    @Override
    protected FedoraResource doForward(final Resource subject) {
        try {
            if (!inDomain(subject)) {
                throw new RepositoryRuntimeException("Subject " + subject + " is not in this repository");
            }

            return nodeResourceConverter.convert(session.getNode(asString(subject)));
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @Override
    protected Resource doBackward(final FedoraResource resource) {
        final String absPath = resource.getPath();

        return toDomain(absPath);
    }

    @Override
    public boolean inDomain(final Resource subject) {
        return subject.isURIResource() && subject.getURI().startsWith(RESOURCE_NAMESPACE);
    }

    @Override
    public Resource toDomain(final String absPath) {
        final String relativePath;

        if (absPath.startsWith("/")) {
            relativePath = absPath.substring(1);
        } else {
            relativePath = absPath;
        }
        return createResource(RESOURCE_NAMESPACE + reverse.convert(relativePath));
    }

    @Override
    public String asString(final Resource subject) {
        if (!inDomain(subject)) {
            return null;
        }

        final String path = subject.getURI().substring(RESOURCE_NAMESPACE.length() - 1);

        final String absPath = forward.convert(path);

        if (absPath.isEmpty()) {
            return "/";
        }
        return absPath;
    }

}
