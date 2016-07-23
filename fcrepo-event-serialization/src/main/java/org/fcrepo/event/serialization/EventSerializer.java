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
package org.fcrepo.event.serialization;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static org.apache.jena.vocabulary.RDF.type;
import static org.apache.jena.vocabulary.DCTerms.identifier;
import static org.apache.jena.vocabulary.DCTerms.isPartOf;
import static org.fcrepo.kernel.api.RdfLexicon.PROV_NAMESPACE;
import static org.fcrepo.kernel.api.observer.OptionalValues.BASE_URL;
import static org.fcrepo.kernel.api.observer.OptionalValues.USER_AGENT;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.kernel.api.observer.FedoraEvent;

/**
 * A basic serialization API for Fedora events
 * @author acoburn
 */
public interface EventSerializer {

    /**
     * Convert an event to an Rdf Model
     * @param evt the Fedora event
     * @return an RDF model representing the event
     */
    public static Model toModel(final FedoraEvent evt) {
        final String FOAF = "http://xmlns.com/foaf/0.1/";

        final Model model = createDefaultModel();
        final String baseUrl = evt.getInfo().get(BASE_URL);
        final String userAgent = evt.getInfo().get(USER_AGENT);
        final Resource root = model.createResource((baseUrl != null ? baseUrl : "info:fedora" ) + evt.getPath());

        evt.getResourceTypes().forEach(rdfType -> {
            root.addProperty(type, createResource(rdfType));
        });

        if (baseUrl != null) {
            root.addProperty(isPartOf, createResource(baseUrl));
        }

        final Resource activity = model.createResource()
                .addProperty(type, createResource(PROV_NAMESPACE + "Activity"))
                .addProperty(identifier, createResource(evt.getEventID()))
                .addLiteral(createProperty(PROV_NAMESPACE, "atTime"),
                        createTypedLiteral(evt.getDate().toString(), XSDdateTime));

        evt.getTypes().stream().map(rdfType -> rdfType.getType()).forEach(rdfType -> {
            activity.addProperty(type, createResource(rdfType));
        });

        root.addProperty(createProperty(PROV_NAMESPACE, "wasGeneratedBy"), activity);

        root.addProperty(createProperty(PROV_NAMESPACE, "wasAttributedTo"),
                model.createResource()
                    .addProperty(type, createResource(PROV_NAMESPACE + "Person"))
                    .addLiteral(createProperty(FOAF, "name"), evt.getUserID()));

        if (userAgent != null) {
            root.addProperty(createProperty(PROV_NAMESPACE, "wasAttributedTo"),
                    model.createResource()
                        .addProperty(type, createResource(PROV_NAMESPACE + "SoftwareAgent"))
                        .addLiteral(createProperty(FOAF, "name"), userAgent));
        }

        return model;
    }

    /**
     * Serialize a FedoraEvent into a JSON String
     * @param evt the Fedora event
     * @return a JSON string
     */
    String serialize(final FedoraEvent evt);
}
