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
package org.fcrepo.kernel.impl.services;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_RESULT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_STATE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_MESSAGE_DIGEST;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SIZE;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_EVENT_OUTCOME_DETAIL;
import static org.fcrepo.kernel.api.RdfLexicon.PREMIS_FIXITY;

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
import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;
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
                .map(DIGEST_ALGORITHM::fromAlgorithm)
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

        // Built for more than one digest in anticipation of FCREPO-3419
        final List<URI> existingDigestList = new ArrayList<>();
        existingDigestList.add(binary.getContentDigest());

        final var digestAlgs = existingDigestList.stream()
                .map(URI::toString)
                .map(a -> a.replace("urn:", "").split(":")[0])
                .map(DIGEST_ALGORITHM::fromAlgorithm)
                .collect(Collectors.toList());

        try (final var content = binary.getContent()) {
            final MultiDigestInputStreamWrapper digestWrapper = new MultiDigestInputStreamWrapper(content,
                    existingDigestList, digestAlgs);
            digestWrapper.getDigests().forEach(d ->
                    model.add(fixityResult, HAS_MESSAGE_DIGEST, model.createResource(d.toString())));
            digestWrapper.checkFixity();
            model.add(fixityResult, HAS_FIXITY_STATE, successResource);
        } catch (final IOException e) {
            // input stream closed prematurely.
            throw new RepositoryRuntimeException("Problem reading content stream from " + binary.getId(), e);
        } catch (final InvalidChecksumException e) {
            model.add(fixityResult, HAS_FIXITY_STATE, badChecksumResource);
        }
        return DefaultRdfStream.fromModel(subject.asNode(), model);
    }
}
