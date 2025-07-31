/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static org.apache.jena.datatypes.xsd.impl.XSDDateTimeType.XSDdateTime;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createLiteralDT;
import static org.apache.jena.graph.NodeFactory.createLiteralString;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_PARENT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.fcrepo.kernel.api.exception.PathNotFoundException;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.ManagedPropertiesService;

import org.apache.jena.graph.Triple;
import org.springframework.stereotype.Component;

/**
 * Retrieve the managed properties as triples
 *
 * @author dbernstein
 * @since 2020-01-07
 */
@Component
public class ManagedPropertiesServiceImpl implements ManagedPropertiesService {

    @Override
    public Stream<Triple> get(final FedoraResource resource) {
        final List<Triple> triples = new ArrayList<>();

        final var describedResource = resource.getDescribedResource();
        final var subject = createURI(resolveId(describedResource));

        var createdBy = resource.getCreatedBy();
        var createdDate = resource.getCreatedDate();
        var lastModifiedBy = resource.getLastModifiedBy();
        var lastModifiedDate = resource.getLastModifiedDate();

        if (createdDate == null && resource.getOriginalResource() instanceof Tombstone) {
            final var deletedResource = ((Tombstone)resource.getOriginalResource()).getDeletedObject();
            createdBy = deletedResource.getCreatedBy();
            createdDate = deletedResource.getCreatedDate();
            lastModifiedBy = deletedResource.getLastModifiedBy();
            lastModifiedDate = deletedResource.getLastModifiedDate();
        }

        if (describedResource instanceof Binary) {
            final Binary binary = (Binary) describedResource;

            createdBy = binary.getCreatedBy();
            createdDate = binary.getCreatedDate();
            lastModifiedBy = binary.getLastModifiedBy();
            lastModifiedDate = binary.getLastModifiedDate();

            triples.add(Triple.create(subject, HAS_SIZE.asNode(),
                    createLiteralDT(String.valueOf(binary.getContentSize()), XSDlong)));
            if (binary.getFilename() != null) {
                triples.add(Triple.create(subject, HAS_ORIGINAL_NAME.asNode(),
                        createLiteralString(binary.getFilename())));
            }
            if (binary.getMimeType() != null) {
                triples.add(Triple.create(subject, HAS_MIME_TYPE.asNode(),
                        createLiteralString(binary.getMimeType())));
            }
            if (binary.getContentDigests() != null) {
                for (var digest : binary.getContentDigests()) {
                    triples.add(Triple.create(subject, HAS_MESSAGE_DIGEST.asNode(),
                            createURI(digest.toString())));
                }

            }
        }

        triples.add(Triple.create(subject, CREATED_DATE.asNode(),
                                  createLiteralDT(createdDate.toString(), XSDdateTime)));
        triples.add(Triple.create(subject, LAST_MODIFIED_DATE.asNode(),
                                  createLiteralDT(lastModifiedDate.toString(), XSDdateTime)));
        if (createdBy != null) {
            triples.add(Triple.create(subject, CREATED_BY.asNode(), createLiteralString(createdBy)));
        }
        if (lastModifiedBy != null) {
            triples.add(Triple.create(subject, LAST_MODIFIED_BY.asNode(), createLiteralString(lastModifiedBy)));
        }

        describedResource.getSystemTypes(true).forEach(triple -> {
            triples.add(Triple.create(subject, type.asNode(), createURI(triple.toString())));
        });

        if (!resource.getFedoraId().isRepositoryRoot() && !resource.isAcl() && !(resource instanceof TimeMap) &&
                !(resource.getOriginalResource() instanceof Tombstone)) {
            try {
                final FedoraResource parent = resource.getDescribedResource().getParent();
                triples.add(Triple.create(subject, HAS_PARENT.asNode(), createURI(resolveId(parent))));
            } catch (final PathNotFoundException e) {
                // no parent.
            }
        }
        return new DefaultRdfStream(subject, triples.stream());
    }

    private String resolveId(final FedoraResource resource) {
        if (resource instanceof TimeMap) {
            return resource.getFedoraId().getFullId();
        }
        return resource.getId();
    }

}
