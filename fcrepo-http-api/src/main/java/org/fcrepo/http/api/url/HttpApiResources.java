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
package org.fcrepo.http.api.url;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static java.util.Collections.singletonMap;
import static org.fcrepo.kernel.api.FedoraJcrTypes.ROOT;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_SERIALIZATION;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_VERSION_HISTORY;
import static org.fcrepo.kernel.api.RdfLexicon.RDFS_LABEL;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.fcrepo.kernel.api.RdfLexicon.DC_NAMESPACE;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.http.api.FedoraExport;
import org.fcrepo.http.api.FedoraVersioning;
import org.fcrepo.http.api.repository.FedoraRepositoryExport;
import org.fcrepo.http.api.repository.FedoraRepositoryTransactions;
import org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.kernel.api.models.NonRdfSource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.fcrepo.serialization.SerializerUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;

import java.util.Map;

/**
 * Inject our HTTP API methods into the object graphs
 *
 * @author awoods
 */
@Component
public class HttpApiResources implements UriAwareResourceModelFactory {

    @Autowired
    protected SerializerUtil serializers;

    @Override
    public Model createModelForResource(final FedoraResource resource,
        final UriInfo uriInfo, final IdentifierConverter<Resource,FedoraResource> idTranslator) {

        final Model model = createDefaultModel();

        final Resource s = idTranslator.reverse().convert(resource);

        if (resource.hasType(ROOT)) {
            addRepositoryStatements(uriInfo, model, s);
        } else {
            addNodeStatements(resource, uriInfo, model, s);
        }

        if (resource instanceof NonRdfSourceDescription) {
            final NonRdfSource describedResource = ((NonRdfSourceDescription) resource).getDescribedResource();

            if (describedResource instanceof FedoraBinary) {
                addContentStatements(idTranslator, (FedoraBinary)describedResource, model);
            }
        } else if (resource instanceof FedoraBinary) {
            addContentStatements(idTranslator, (FedoraBinary)resource, model);
        }

        return model;
    }

    private static void addContentStatements(final IdentifierConverter<Resource,FedoraResource> idTranslator,
                                             final FedoraBinary resource,
                                             final Model model) {
        // fcr:fixity
        final Resource subject = idTranslator.reverse().convert(resource);
        model.add(subject, HAS_FIXITY_SERVICE, createResource(subject.getURI() +
                "/fcr:fixity"));
    }

    private void addNodeStatements(final FedoraResource resource, final UriInfo uriInfo,
        final Model model, final Resource s) {

        String path = resource.getPath();
        path = path.endsWith(JCR_CONTENT) ? path.replace("/" + JCR_CONTENT, "") : path;
        final Map<String, String> pathMap = singletonMap("path", path.substring(1));

        // fcr:versions
        if (resource.isVersioned()) {
            model.add(s, HAS_VERSION_HISTORY, createResource(uriInfo
                    .getBaseUriBuilder().path(FedoraVersioning.class).buildFromMap(
                            pathMap, false).toASCIIString()));
        }

        final Property dcFormat = createProperty(DC_NAMESPACE + "format");
        // fcr:exports?format=xyz
        for (final String key : serializers.keySet()) {
            if (serializers.getSerializer(key).canSerialize(resource)) {
                final Resource format =
                        createResource(uriInfo.getBaseUriBuilder().path(
                                FedoraExport.class).queryParam("format", key)
                                .buildFromMap(pathMap, false).toASCIIString());
                model.add(s, HAS_SERIALIZATION, format);

                //RDF the serialization
                final Resource formatRDF = createResource(REPOSITORY_NAMESPACE + key);
                model.add(format, dcFormat, formatRDF);
            }
        }
    }

    private void addRepositoryStatements(final UriInfo uriInfo, final Model model,
        final Resource s) {
        // fcr:tx
        model.add(s, HAS_TRANSACTION_SERVICE, createResource(uriInfo
                .getBaseUriBuilder().path(FedoraRepositoryTransactions.class)
                .build().toASCIIString()));

        final Property dcFormat = createProperty(DC_NAMESPACE + "format");
        // fcr:export?format=xyz
        for (final String key : serializers.keySet()) {
            final Resource format = createResource(uriInfo
                .getBaseUriBuilder().path(FedoraRepositoryExport.class)
                .queryParam("format", key).build().toASCIIString());
            model.add(s, HAS_SERIALIZATION, format);
            final Resource formatRDF = createResource(REPOSITORY_NAMESPACE + key);

            model.add(formatRDF, RDFS_LABEL, key);
            model.add(format, dcFormat, formatRDF);
        }
    }

}
