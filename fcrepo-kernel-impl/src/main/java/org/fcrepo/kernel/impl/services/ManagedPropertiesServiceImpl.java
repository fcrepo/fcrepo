/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static org.apache.jena.datatypes.xsd.impl.XSDDateTimeType.XSDdateTime;
import static org.apache.jena.graph.NodeFactory.createLiteral;
import static org.apache.jena.graph.NodeFactory.createURI;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.CREATED_DATE;
import static org.fcrepo.kernel.api.RdfLexicon.FEDORA_OCFL_PATH;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MIME_TYPE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_ORIGINAL_NAME;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_BY;
import static org.fcrepo.kernel.api.RdfLexicon.LAST_MODIFIED_DATE;
import static org.slf4j.LoggerFactory.getLogger;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.inject.Inject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.fcrepo.config.DisplayOcflPath;
import org.fcrepo.config.OcflPropsConfig;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.identifiers.FedoraId;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.TimeMap;
import org.fcrepo.kernel.api.models.Tombstone;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.ManagedPropertiesService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Retrieve the managed properties as triples
 *
 * @author dbernstein
 * @since 2020-01-07
 */
@Component
public class ManagedPropertiesServiceImpl implements ManagedPropertiesService {

    private static final Logger LOGGER = getLogger(ManagedPropertiesServiceImpl.class);

    @Inject
    OcflPropsConfig ocflPropsConfig;

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
                    createLiteral(String.valueOf(binary.getContentSize()), XSDlong)));
            if (binary.getFilename() != null) {
                triples.add(Triple.create(subject, HAS_ORIGINAL_NAME.asNode(), createLiteral(binary.getFilename())));
            }
            if (binary.getMimeType() != null) {
                triples.add(Triple.create(subject, HAS_MIME_TYPE.asNode(), createLiteral(binary.getMimeType())));
            }
            if (binary.getContentDigests() != null) {
                for (var digest : binary.getContentDigests()) {
                    triples.add(Triple.create(subject, HAS_MESSAGE_DIGEST.asNode(),
                            createURI(digest.toString())));
                }

            }
        }

        triples.add(Triple.create(subject, CREATED_DATE.asNode(),
                                  createLiteral(createdDate.toString(), XSDdateTime)));
        triples.add(Triple.create(subject, LAST_MODIFIED_DATE.asNode(),
                                  createLiteral(lastModifiedDate.toString(), XSDdateTime)));
        if (createdBy != null) {
            triples.add(Triple.create(subject, CREATED_BY.asNode(), createLiteral(createdBy)));
        }
        if (lastModifiedBy != null) {
            triples.add(Triple.create(subject, LAST_MODIFIED_BY.asNode(), createLiteral(lastModifiedBy)));
        }

        describedResource.getSystemTypes(true).forEach(triple -> {
            triples.add(Triple.create(subject, type.asNode(), createURI(triple.toString())));
        });

        if (ocflPropsConfig.getDisplayOcflPath() != DisplayOcflPath.NONE) {
            final var ocflPath = resolveOcflPath(describedResource, ocflPropsConfig.getDisplayOcflPath());
            triples.add(Triple.create(subject, createURI(FEDORA_OCFL_PATH.getURI()),
                    createLiteral(ocflPath)));
        }

        return new DefaultRdfStream(subject, triples.stream());
    }

    private String resolveId(final FedoraResource resource) {
        if (resource instanceof TimeMap) {
            return resource.getFedoraId().getFullId();
        }
        return resource.getId();
    }

    private String resolveOcflPath(final FedoraResource resource, final DisplayOcflPath displayOcflPath) {
        try {
            final FedoraId id;
            if (resource.getArchivalGroupId().isPresent()) {
                id = resource.getArchivalGroupId().get();
            } else {
                id = resource.getFedoraId();
            }
            final var algo = MessageDigest.getInstance("sha-256");
            final var ocflId = algo.digest(id.getBaseId().getBytes(UTF_8));
            final var ocflIdHash = encodeHexString(ocflId);
            final List<String> hashSlices = new ArrayList<>();
            for (int i = 0; i < ocflIdHash.length(); i += 3) {
                hashSlices.add(ocflIdHash.substring(i, Math.min(i + 3,ocflIdHash.length())));
            }
            final var ocflPath = hashSlices.get(0) + "/" + hashSlices.get(1) + "/" + hashSlices.get(2) + "/" +
                    ocflIdHash;
            return (displayOcflPath == DisplayOcflPath.ABSOLUTE ? ocflPropsConfig.getOcflRepoRoot() + "/" : "") +
                    ocflPath;
        } catch (NoSuchAlgorithmException e) {
            final var message = "Unable to resolve OCFL path for resource " + resource.getId();
            LOGGER.error(message, e);
            throw new RepositoryRuntimeException(message, e);
        }
    }

}
