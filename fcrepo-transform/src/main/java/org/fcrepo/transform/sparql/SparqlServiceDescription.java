/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.transform.sparql;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.RdfLexicon.HAS_SPARQL_ENDPOINT;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.RdfLexicon;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.transform.http.FedoraSparql;

import javax.jcr.Session;
import javax.ws.rs.core.UriInfo;

/**
 * Class to create service description for sparql endpoint
 *
 * @author lsitu
 */
public class SparqlServiceDescription {
    private static final String SPARQL_FORMATS_NAMESPACE = "http://www.w3.org/ns/formats/";

    private Session session;

    private UriInfo uriInfo;

    /**
     * Constructor
     * @param session
     * @param uriInfo
     */
    public SparqlServiceDescription(final Session session, final UriInfo uriInfo) {
        this.session = session;
        this.uriInfo = uriInfo;
    }

    /**
     * Create sparql service description
     * @return rdf model
     */
    public RdfStream createServiceDescriptoion() {
        final Model model = ModelFactory.createDefaultModel();
        // namespace prefixes use
        model.setNsPrefix("rdf", RdfLexicon.RDF_NAMESPACE);
        model.setNsPrefix("sd", RdfLexicon.SPARQL_SD_NAMESPACE);

        final Resource s = model.createResource();
        model.add(s, createProperty(RdfLexicon.RDF_NAMESPACE + "type"),
                model.createResource(RdfLexicon.SPARQL_SD_NAMESPACE + "Service"));
        model.add(s, HAS_SPARQL_ENDPOINT, model.createResource(
                uriInfo.getBaseUriBuilder().path(FedoraSparql.class).build().toASCIIString()));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "supportedLanguage"),
                model.createResource(RdfLexicon.SPARQL_SD_NAMESPACE + "SPARQL11Query"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat"),
                model.createResource(SPARQL_FORMATS_NAMESPACE + "SPARQL_Results_TSV"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat"),
                model.createResource(SPARQL_FORMATS_NAMESPACE + "SPARQL_Results_CSV"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat"),
                model.createResource(SPARQL_FORMATS_NAMESPACE + "SPARQL_Results_JSON"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat"),
                model.createResource(SPARQL_FORMATS_NAMESPACE + "SPARQL_Results_XML"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat"),
                model.createResource(SPARQL_FORMATS_NAMESPACE + "Turtle"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat"),
                model.createResource(SPARQL_FORMATS_NAMESPACE + "N3"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat"),
                model.createResource(SPARQL_FORMATS_NAMESPACE + "N-Triples"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "resultFormat"),
                model.createResource(SPARQL_FORMATS_NAMESPACE + "RDF_XML"));
        model.add(s, createProperty(RdfLexicon.SPARQL_SD_NAMESPACE + "feature"),
                model.createResource(RdfLexicon.SPARQL_SD_NAMESPACE + "DereferencesURIs"));
        final RdfStream rdfStream = RdfStream.fromModel(model);
        rdfStream.session(session);
        return rdfStream;
    }
}
