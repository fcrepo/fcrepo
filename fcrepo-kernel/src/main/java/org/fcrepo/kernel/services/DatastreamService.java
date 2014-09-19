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
package org.fcrepo.kernel.services;

import javax.jcr.Node;
import javax.jcr.Session;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraBinary;
import org.fcrepo.kernel.exception.ResourceTypeException;
import org.fcrepo.kernel.rdf.IdentifierTranslator;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.kernel.utils.iterators.RdfStream;

/**
 * @author bbpennel
 * @since Feb 20, 2014
 */
public interface DatastreamService extends Service {

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param path jcr path to the datastream
     * @return retrieved Datastream
     */
    Datastream findOrCreateDatastream(Session session, String path);

    /**
     * Retrieve a Binary instance by path
     * @param session
     * @param path
     * @return
     */
    FedoraBinary getBinary(Session session, String path);

    /**
     * Retrieve a Datastream instance by pid and dsid
     *
     * @param node datastream node
     * @return node as a Datastream
     */
    Datastream asDatastream(Node node) throws ResourceTypeException;

    /**
     * Retrieve a Binary instance from a node
     *
     * @param node datastream node
     * @return node as a Datastream
     */
    FedoraBinary asBinary(Node node);


    /**
     * Get the fixity results for the datastream as a RDF Dataset
     *
     * @param subjects
     * @param datastream
     * @return fixity results for datastream
     */
    RdfStream getFixityResultsModel(IdentifierTranslator subjects, FedoraBinary datastream);

    /**
     * Get the active storage policy decision point
     * TODO: this should move to a different service?
     * @return
     */
    StoragePolicyDecisionPoint getStoragePolicyDecisionPoint();

}