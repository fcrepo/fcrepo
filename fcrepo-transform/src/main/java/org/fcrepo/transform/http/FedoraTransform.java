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

package org.fcrepo.transform.http;

import static org.apache.jena.riot.WebContent.contentTypeSPARQLQuery;
import static org.fcrepo.transform.transformations.LDPathTransform.APPLICATION_RDF_LDPATH;
import static org.fcrepo.transform.transformations.LDPathTransform.getNodeTypeTransform;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;

import org.apache.jena.riot.WebContent;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.fcrepo.http.api.FedoraNodes;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.http.commons.api.rdf.HttpGraphSubjects;
import org.fcrepo.http.commons.session.InjectedSession;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.transform.Transformation;
import org.fcrepo.transform.TransformationFactory;
import org.fcrepo.transform.transformations.LDPathTransform;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.codahale.metrics.annotation.Timed;
import com.hp.hpl.jena.query.Dataset;

@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:transform")
public class FedoraTransform extends AbstractResource {

    @InjectedSession
    protected Session session;

    private final Logger logger = getLogger(FedoraTransform.class);

    @Autowired(required = false)
    private TransformationFactory transformationFactory;

    /**
     * Register the LDPath configuration tree in JCR
     * 
     * @throws RepositoryException
     * @throws java.io.IOException
     */
    @PostConstruct
    public void setUpRepositoryConfiguration() throws RepositoryException,
            IOException {

        if (transformationFactory == null) {
            transformationFactory = new TransformationFactory();
        }

        final Session session = sessions.getInternalSession();
        final JcrTools jcrTools = new JcrTools(true);

        // register our CND
        jcrTools.registerNodeTypes(session, "ldpath.cnd");

        // create the configuration base path
        jcrTools.findOrCreateNode(session,
                "/fedora:system/fedora:transform", "fedora:configuration",
                "fedora:node_type_configuration");
        final Node node =
                jcrTools.findOrCreateNode(session,
                        LDPathTransform.CONFIGURATION_FOLDER + "default",
                        NodeType.NT_FOLDER, NodeType.NT_FOLDER);

        // register an initial demo program
        if (!node.hasNode(NodeType.NT_BASE)) {
            final Node base_config =
                    node.addNode(NodeType.NT_BASE, NodeType.NT_FILE);
            jcrTools.uploadFile(session, base_config.getPath(), getClass()
                    .getResourceAsStream(
                            "/ldpath/default/nt_base_ldpath_program.txt"));
        }

        session.save();
        session.logout();
    }

    /**
     * Execute an LDpath program transform
     * 
     * @param pathList
     * @return Binary blob
     * @throws RepositoryException
     */
    @GET
    @Path("{program}")
    @Produces({MediaType.APPLICATION_JSON})
    @Timed
    public Object evaluateLdpathProgram(@PathParam("path")
    final List<PathSegment> pathList, @PathParam("program")
    final String program) throws RepositoryException, LDPathParseException {

        try {
            final String path = toPath(pathList);
            final FedoraResource object =
                    nodeService.getObject(session, path);

            final Transformation t =
                    getNodeTypeTransform(object.getNode(), program);

            final Dataset propertiesDataset =
                    object.getPropertiesDataset(new HttpGraphSubjects(
                            session, FedoraNodes.class, uriInfo));

            return t.apply(propertiesDataset);

        } finally {
            session.logout();
        }
    }

    /**
     * Get the LDPath output as a JSON stream appropriate for e.g. Solr
     * 
     * @param pathList
     * @param requestBodyStream
     * @return
     * @throws RepositoryException
     * @throws LDPathParseException
     */
    @POST
    @Consumes({APPLICATION_RDF_LDPATH, contentTypeSPARQLQuery})
    @Produces({MediaType.APPLICATION_JSON, WebContent.contentTypeTextTSV,
            WebContent.contentTypeTextCSV, WebContent.contentTypeSSE,
            WebContent.contentTypeTextPlain,
            WebContent.contentTypeResultsJSON,
            WebContent.contentTypeResultsXML,
            WebContent.contentTypeResultsBIO,
            WebContent.contentTypeTurtle, WebContent.contentTypeN3,
            WebContent.contentTypeNTriples, WebContent.contentTypeRDFXML})
    @Timed
    public Object evaluateTransform(@PathParam("path")
    final List<PathSegment> pathList, @HeaderParam("Content-Type")
    final MediaType contentType, final InputStream requestBodyStream)
            throws RepositoryException, LDPathParseException {

        try {
            final String path = toPath(pathList);
            final FedoraResource object =
                    nodeService.getObject(session, path);
            final Dataset propertiesDataset =
                    object.getPropertiesDataset(new HttpGraphSubjects(
                            session, FedoraNodes.class, uriInfo));

            final Transformation t =
                    transformationFactory.getTransform(contentType,
                            requestBodyStream);
            return t.apply(propertiesDataset);
        } finally {
            session.logout();
        }

    }

}
