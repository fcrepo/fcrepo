/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.kernel.rdf;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.services.LowLevelStorageService;
import org.fcrepo.kernel.utils.iterators.RdfStream;

/**
 * {@link RdfStream} that holds contexts related to a specific {@link Node}.
 *
 * @author ajs6f
 * @date Oct 10, 2013
 */
public abstract class NodeRdfContext extends RdfStream {

    private final Node node;

    private final GraphSubjects graphSubjects;

    private final com.hp.hpl.jena.graph.Node subject;

    private final LowLevelStorageService lowLevelStorageService;

    /**
     * Default constructor.
     *
     * @param node
     * @param graphSubjects
     * @throws RepositoryException
     */
    public NodeRdfContext(final Node node, final GraphSubjects graphSubjects, final LowLevelStorageService lowLevelStorageService) throws RepositoryException {
        super();
        this.node = node;
        this.graphSubjects = graphSubjects;
        subject = graphSubjects.getGraphSubject(node).asNode();

        // TODO fix GraphProperties to allow for LowLevelStorageServices to pass through it
        // this is horribly ugly. LowLevelStorageServices are supposed to be managed beans.
        // but the contract of GraphProperties isn't wide enough to pass one in, so rather than
        // alter GraphProperties right now, I'm just spinning one on the fly.
        if (lowLevelStorageService == null) {
            this.lowLevelStorageService = new LowLevelStorageService();
            this.lowLevelStorageService.setRepository(node.getSession()
                    .getRepository());
        } else {
            this.lowLevelStorageService = lowLevelStorageService;
        }
    }

    /**
     * @return The {@link Node} in question
     */
    public Node node() {
        return node;
    }

    /**
     * @return local {@link GraphSubjects}
     */
    public GraphSubjects graphSubjects() {
        return graphSubjects;
    }

    /**
     * @return the RDF subject at the center of this context
     */
    public com.hp.hpl.jena.graph.Node subject() {
        return subject;
    }

    /**
     * @return the {@link LowLevelStorageService} in scope
     */
    public LowLevelStorageService lowLevelStorageService() {
        return lowLevelStorageService;
    }

}
