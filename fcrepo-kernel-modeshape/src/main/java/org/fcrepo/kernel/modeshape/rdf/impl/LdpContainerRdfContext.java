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
package org.fcrepo.kernel.modeshape.rdf.impl;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.FedoraTypes;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.functions.Converter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyValueIterator;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.RowIterator;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.RowToResourceUri;
import org.fcrepo.kernel.modeshape.utils.UncheckedFunction;
import org.fcrepo.kernel.modeshape.utils.UncheckedPredicate;

import org.modeshape.jcr.query.JcrQueryResult;
import org.slf4j.Logger;

import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.api.RdfLexicon.LDP_MEMBER;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.converters.PropertyConverter.getPropertyNameFromPredicate;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @author ajs6f
 * @since 9/25/14
 */
public class LdpContainerRdfContext extends NodeRdfContext {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = getLogger(LdpContainerRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws javax.jcr.RepositoryException if repository exception occurred
     */
    public LdpContainerRdfContext(final FedoraResource resource,
                                  final Converter<Resource, String> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        concat(getMembershipContext(resource)
                .flatMap(uncheck(p -> memberRelations(nodeConverter.apply(p.getParent())))));
    }

    /**
     * 
     * @param resource
     * @param session
     * @param idTranslator
     * @throws RepositoryException
     */
    public LdpContainerRdfContext(final Resource resource, final Session session,
            final Converter<Resource, String> idTranslator) throws RepositoryException {
        this(toResource(resource, session, idTranslator), idTranslator);
    }

    @SuppressWarnings("unchecked")
    private static Stream<Property> getMembershipContext(final FedoraResource resource) throws RepositoryException {
        return iteratorToStream(getJcrNode(resource).getReferences(LDP_MEMBER_RESOURCE))
                    .filter(UncheckedPredicate.uncheck((final Property p) -> {
                        final Node container = p.getParent();
                        return container.isNodeType(LDP_DIRECT_CONTAINER)
                            || container.isNodeType(LDP_INDIRECT_CONTAINER);
                    }));
    }

    /**
     * Get the member relations assert on the subject by the given node
     * @param container
     * @return
     * @throws RepositoryException
     */
    private Stream<Triple> memberRelations(final FedoraResource container) throws RepositoryException {
        final com.hp.hpl.jena.graph.Node memberRelation;

        if (container.hasProperty(LDP_HAS_MEMBER_RELATION)) {
            final Property property = getJcrNode(container).getProperty(LDP_HAS_MEMBER_RELATION);
            memberRelation = createURI(property.getString());
        } else if (container.hasType(LDP_BASIC_CONTAINER)) {
            memberRelation = LDP_MEMBER.asNode();
        } else {
            return empty();
        }

        if (!container.hasType(LDP_INDIRECT_CONTAINER)) {
            // Basic or Direct, just return the mapped children
            return childMembersQuery(container, memberRelation);
        }
        // Indirect
        if (!container.hasProperty(LDP_INSERTED_CONTENT_RELATION)) {
            return empty();
        }

        final String insertedContainerProperty =
                getJcrNode(container).getProperty(LDP_INSERTED_CONTENT_RELATION).getString();

        return childMembersDataQuery(container, memberRelation, createResource(insertedContainerProperty));
    }

    @SuppressWarnings("unused")
    private Stream<Triple> childIteratingMembers(final FedoraResource container,
            final com.hp.hpl.jena.graph.Node memberRelation) {
        return container.getChildren().flatMap(
                UncheckedFunction.<FedoraResource, Stream<Triple>>uncheck(child -> {
                    final com.hp.hpl.jena.graph.Node childSubject = uriFor(child.getDescribedResource());
                    return of(create(subject(), memberRelation, childSubject));
                }
                ));
    }
    @SuppressWarnings("unused")
    private Stream<Triple> childMembersQuery(final FedoraResource container,
            final com.hp.hpl.jena.graph.Node memberRelation) {
        try {
            final com.hp.hpl.jena.graph.Node subject = subject();
            final Session session = getJcrNode(resource()).getSession();
            final String path = container.getPath();
            final String jcrSql =
                    "SELECT [jcr:path] AS path, [jcr:mixinTypes] AS type\n" +
                    "FROM [nt:base] as child\n" +
                    "WHERE ISDESCENDANTNODE(child,'" + path + "')" +
                    "AND child.[jcr:mixinTypes] != '" + FedoraTypes.FEDORA_PAIRTREE + "'";
            final javax.jcr.query.QueryManager queryManager =
                    session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery(jcrSql, "JCR-SQL2");
            final JcrQueryResult result = (JcrQueryResult) query.execute();
            final RowIterator rows = new RowIterator(result.getRows());
            final RowToResourceUri toUris = new RowToResourceUri(translator());
            return iteratorToStream(rows).map(UncheckedFunction.<Row, com.hp.hpl.jena.graph.Node>uncheck(
                    (Row r) -> createResource(toUris.apply(r.getValue("path").getString(), r.getValues())
                    .getString()).asNode()))
                    .map((com.hp.hpl.jena.graph.Node n) -> create(subject, memberRelation, n));
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }
    @SuppressWarnings("unused")
    private Stream<Triple> originalIndirectBehavior(final FedoraResource container,
            final com.hp.hpl.jena.graph.Node memberRelation, final Resource contentRelation)
                    throws RepositoryException {
        final Session session = getJcrNode(resource()).getSession();
        return container.getChildren().flatMap(
            UncheckedFunction.<FedoraResource, Stream<Triple>>uncheck(child -> {
                String insertedContentProperty = getPropertyNameFromPredicate(session, contentRelation, null);

                if (child.hasProperty(insertedContentProperty)) {
                    // do nothing, insertedContentProperty is good

                } else if (child.hasProperty(getReferencePropertyName(insertedContentProperty))) {
                    // The insertedContentProperty is a pseudo reference property
                    insertedContentProperty = getReferencePropertyName(insertedContentProperty);

                } else {
                    // No property found!
                    return empty();
                }

                return iteratorToStream(new PropertyValueIterator(
                        getJcrNode(child).getProperty(insertedContentProperty), new RowToResourceUri(translator())))
                    .map(uncheck(v -> create(subject(), memberRelation,
                        new ValueConverter(session, translator()).apply(v).asNode())));
            }));
    }
    @SuppressWarnings("unused")
    private Stream<Triple> childMembersDataQuery(final FedoraResource container,
            final com.hp.hpl.jena.graph.Node memberRelation, final Resource contentRelation) {
        try {
            final com.hp.hpl.jena.graph.Node subject = subject();
            final Session session = getJcrNode(resource()).getSession();
            final String insertedContentProperty = getPropertyNameFromPredicate(session, contentRelation, null);
            final String insertedContentRefProperty =
                    getReferencePropertyName(insertedContentProperty);
            final String path = container.getPath();
            //TODO will pairtree nodes have the content property?
            String jcrSql2 =
                   "SELECT [jcr:mixinTypes] AS type\n" +
                    "FROM [nt:base] as ref\n" +
                    "WHERE [jcr:uuid] IN (\n" +
                    "  SELECT [" + insertedContentRefProperty + "] FROM [nt:base] AS child" +
                    "  WHERE ISDESCENDANTNODE(child, '" + path + "')" +
                    "  AND child.[" + insertedContentRefProperty + "] IS NOT NULL" +
                    ")";
            RowIterator rows = query(jcrSql2, session);
            if (rows.hasNext()) {
                final RowToResourceUri toUris = new RowToResourceUri(translator());
                return iteratorToStream(rows).map(UncheckedFunction.<Row, com.hp.hpl.jena.graph.Node>uncheck(
                        (Row r) -> createResource(
                                toUris.apply(r.getPath(), r.getValues()).getString()).asNode()))
                        .map((com.hp.hpl.jena.graph.Node n) -> create(subject, memberRelation, n));
            } else {
                //TODO will pairtree nodes have the content property?
                jcrSql2 =
                        "SELECT [" + insertedContentProperty + "] AS uri\n" +
                        "FROM [nt:base] as child\n" +
                        "WHERE ISDESCENDANTNODE(child, '" + path + "')\n" +
                        "AND child.[" + insertedContentProperty + "] IS NOT NULL";
                rows = query(jcrSql2, session);
                if (!rows.hasNext()) {
                    return empty();
                }
                return iteratorToStream(rows).map(UncheckedFunction.<Row, Resource>uncheck(
                        (Row r) -> createResource(r.getValue("uri").getString())))
                        .map((Resource n) -> create(subject, memberRelation, n.asNode()));
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    private static RowIterator query(final String jcrSql2, final Session session) throws RepositoryException {
        final javax.jcr.query.QueryManager queryManager =
                session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(jcrSql2, "JCR-SQL2");
        final JcrQueryResult result = (JcrQueryResult) query.execute();
        //LOGGER.info(result.getPlan());
        return new RowIterator(result.getRows());
    }
}
