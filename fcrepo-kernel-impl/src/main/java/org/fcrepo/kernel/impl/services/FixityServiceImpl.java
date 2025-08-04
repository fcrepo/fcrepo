/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDnonNegativeInteger;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.vocabulary.RDF.type;
import static org.apache.jena.vocabulary.RDF.value;
import static org.fcrepo.kernel.api.RdfLexicon.EVENT;
import static org.fcrepo.kernel.api.RdfLexicon.EVENT_OUTCOME_FAIL;
import static org.fcrepo.kernel.api.RdfLexicon.EVENT_OUTCOME_SUCCESS;
import static org.fcrepo.kernel.api.RdfLexicon.EVENT_TYPE_FIXITY_CHECK;
import static org.fcrepo.kernel.api.RdfLexicon.FIXITY;
import static org.fcrepo.kernel.api.RdfLexicon.GENERATED;
import static org.fcrepo.kernel.api.RdfLexicon.HASHFUNC_SHA512;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.OUTCOME;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_EVENT_OUTCOME_DETAIL;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_FIXITY;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS3_FILE;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS3_FIXITY;
import static org.fcrepo.kernel.api.RdfLexicon.SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.WAS_GENERATED_BY;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.FixityService;
import org.fcrepo.config.DigestAlgorithm;
import org.fcrepo.persistence.common.MultiDigestInputStreamWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link org.fcrepo.kernel.api.services.FixityService}
 *
 * @author dbernstein
 * @author whikloj
 */
@Component
public class FixityServiceImpl extends AbstractService implements FixityService {

    private static final Literal successResource = createTypedLiteral("SUCCESS");
    private static final Literal badChecksumResource = createTypedLiteral("BAD_CHECKSUM");

    @Override
    public Collection<URI> getFixity(final Binary binary, final Collection<String> algorithms)
            throws UnsupportedAlgorithmException {
        final var digestAlgs = algorithms.stream()
                .map(DigestAlgorithm::fromAlgorithm)
                .collect(Collectors.toList());

        try (final var content = binary.getContent()) {
            final MultiDigestInputStreamWrapper digestWrapper = new MultiDigestInputStreamWrapper(content, null,
                    digestAlgs);
            return digestWrapper.getDigests();
        } catch (final IOException e) {
            // input stream closed prematurely.
            throw new RepositoryRuntimeException("Problem reading content stream from " + binary.getId(), e);
        }
    }

    @Override
    public RdfStream checkFixity(final Binary binary)
            throws InvalidChecksumException {
        final Model model = createDefaultModel();
        final Resource subject = model.createResource(binary.getId());
        final Resource fixityResult = model.createResource(
                binary.getFedoraId().resolve("#" + UUID.randomUUID().toString()).getFullId());
        model.add(subject, HAS_FIXITY_RESULT, fixityResult);
        model.add(fixityResult, type, PREMIS_FIXITY);
        model.add(fixityResult, type, PREMIS_EVENT_OUTCOME_DETAIL);
        model.add(fixityResult, HAS_SIZE, createTypedLiteral(binary.getContentSize()));

        final Resource fixity3Result = model.createResource(
                binary.getFedoraId().resolve("#" + UUID.randomUUID()).getFullId());
        model.add(subject, type, PREMIS3_FILE);
        model.add(subject, SIZE, createTypedLiteral(
                String.valueOf(binary.getContentSize()), XSDnonNegativeInteger)
        );

        final Resource event = model.createResource(
                binary.getFedoraId().resolve("#" + UUID.randomUUID()).getFullId());
        model.add(event, type, EVENT);
        model.add(event, type, EVENT_TYPE_FIXITY_CHECK);
        model.add(fixity3Result, WAS_GENERATED_BY, event);
        model.add(event, GENERATED, fixity3Result);

        // Built for more than one digest in anticipation of FCREPO-3419
        final List<URI> existingDigestList = new ArrayList<>();
        existingDigestList.addAll(binary.getContentDigests());

        final var digestAlgs = existingDigestList.stream()
                .map(URI::toString)
                .map(a -> a.replace("urn:", "").split(":")[0])
                .map(DigestAlgorithm::fromAlgorithm)
                .collect(Collectors.toList());

        try (final var content = binary.getContent()) {
            final MultiDigestInputStreamWrapper digestWrapper = new MultiDigestInputStreamWrapper(content,
                    existingDigestList, digestAlgs);

            final var computedSha512Digest = digestWrapper.getDigest(DigestAlgorithm.SHA512);
            if (computedSha512Digest != null) {
                model.add(subject, FIXITY, fixity3Result);
                model.add(fixity3Result, type, PREMIS3_FIXITY);
                model.add(fixity3Result, type, HASHFUNC_SHA512);
                model.add(fixity3Result, value, model.createTypedLiteral(computedSha512Digest));
            }

            digestWrapper.getDigests().forEach(d ->
                    model.add(fixityResult, HAS_MESSAGE_DIGEST, model.createResource(d.toString())));
            digestWrapper.checkFixity();
            model.add(fixityResult, HAS_FIXITY_STATE, successResource);
            model.add(event, OUTCOME, EVENT_OUTCOME_SUCCESS);
        } catch (final IOException e) {
            // input stream closed prematurely.
            throw new RepositoryRuntimeException("Problem reading content stream from " + binary.getId(), e);
        } catch (final InvalidChecksumException e) {
            model.add(fixityResult, HAS_FIXITY_STATE, badChecksumResource);
            model.add(event, OUTCOME, EVENT_OUTCOME_FAIL);
        }
        return DefaultRdfStream.fromModel(subject.asNode(), model);
    }
}
