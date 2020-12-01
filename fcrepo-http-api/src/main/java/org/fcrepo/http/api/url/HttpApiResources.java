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
package org.fcrepo.http.api.url;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_FIXITY_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.HAS_TRANSACTION_SERVICE;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_ROOT;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import org.fcrepo.http.api.Transactions;
import org.fcrepo.http.commons.api.rdf.UriAwareResourceModelFactory;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.models.NonRdfSourceDescription;

import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;

/**
 * Inject our HTTP API methods into the object graphs
 *
 * @author awoods
 */
@Component
public class HttpApiResources implements UriAwareResourceModelFactory {

    @Override
    public Model createModelForResource(final FedoraResource resource,
        final UriInfo uriInfo) {

        final Model model = createDefaultModel();

        final Resource s = createResource(resource.getFedoraId().getFullId());

        if (resource.hasType(REPOSITORY_ROOT.getURI())) {
            addRepositoryStatements(uriInfo, model, s);
        }

        if (resource instanceof NonRdfSourceDescription && !resource.isMemento()) {
            addContentStatements((Binary)resource.getDescribedResource(), model);
        }
        return model;
    }

    private static void addContentStatements(final Binary resource,
                                             final Model model) {
        // fcr:fixity
        final Resource subject = createResource(resource.getFedoraId().getFullId());
        model.add(subject, HAS_FIXITY_SERVICE, createResource(subject.getURI() +
                "/fcr:fixity"));
    }


    private void addRepositoryStatements(final UriInfo uriInfo, final Model model,
        final Resource s) {
        // fcr:tx
        model.add(s, HAS_TRANSACTION_SERVICE, createResource(uriInfo
                .getBaseUriBuilder().path(Transactions.class)
                .build().toASCIIString()));
    }

}
