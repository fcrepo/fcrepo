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

package org.fcrepo.storage.policy;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.lang.StringUtils;
import org.fcrepo.http.commons.AbstractResource;
import org.fcrepo.kernel.services.policy.Policy;
import org.fcrepo.kernel.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.http.commons.session.InjectedSession;
import org.modeshape.jcr.api.JcrTools;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;

/**
 * RESTful interface to create and manage storage policies
 * 
 * 
 * @author osmandin
 * @date Aug 14, 2013
 */

@Component
@Scope("prototype")
@Path("/{path: .*}/fcr:storagepolicy")
public class StoragePolicy extends AbstractResource {

    @InjectedSession
    protected Session session;

    @Context
    protected HttpServletRequest request;

    @Autowired(required = true)
    protected StoragePolicyDecisionPoint storagePolicyDecisionPoint;

    private JcrTools jcrTools;

    public static final String POLICY_RESOURCE = "policies";

    private static final Logger LOGGER = getLogger(StoragePolicy.class);

    /**
     * Initialize
     * 
     * @throws RepositoryException
     * @throws IOException
     */
    @PostConstruct
    public void setUpRepositoryConfiguration() throws RepositoryException,
        IOException {
        final JcrTools jcrTools = getJcrTools();
        Session session = null;
        try {
            session = sessions.getInternalSession();
            jcrTools.findOrCreateNode(session,
                "/fedora:system/fedora:storage_policy", null);
            session.save();
            LOGGER.debug("Created configuration node");
        } catch (final Exception e) {
            throw e;
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * POST to store nodeType and hint
     * 
     * @param request For now, follows pattern: mix:mimeType image/tiff
     *        store-hint
     * @return status
     */

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Timed
    public Response post(final @PathParam("path") String path,
                         final String request) throws Exception {
        LOGGER.debug("POST Received request param: {}", request);
        Response.ResponseBuilder response;

        if (!path.equalsIgnoreCase(POLICY_RESOURCE)) {
            response = Response.status(405);
            return response.entity(
                    "POST method not allowed on " + getUriInfo().getAbsolutePath() +
                            ", try /policies/fcr:storagepolicy")
                    .build();
        }

        final JcrTools jcrTools = getJcrTools();
        final String[] str = StringUtils.split(request); // simple split for now
        validateArgs(str.length);
        try {
            final Node node =
                jcrTools.findOrCreateNode(session,
                    "/fedora:system/fedora:storage_policy", "test");
            if (isValidNodeTypeProperty(session, str[0]) ||
                isValidConfigurationProperty(str[0])) {
                node.setProperty(str[0], new String[] {str[1] + ":" + str[2]});

                // TODO (for now) instantiate PolicyType based on mix:mimeType
                Policy policy = newPolicyInstance(str[0], str[1], str[2]);
                // TODO (for now) based on object comparison using equals()
                if (storagePolicyDecisionPoint.contains(policy)) {
                    throw new PolicyTypeException("Property already exists!");
                }
                storagePolicyDecisionPoint.addPolicy(policy);
                session.save();
                LOGGER.debug("Saved PDS hint {}", request);

                response = Response.created(getUriInfo().getBaseUriBuilder()
                                                    .path(str[0])
                                                    .path("fcr:storagepolicy")
                                                    .build());
            } else {
                throw new PolicyTypeException(
                        "Invalid property type specified: " + str[0]);
            }
        } catch (final Exception e) {
            response = Response.serverError().entity(e.getMessage());
        } finally {
            session.logout();
        }

        return response.build();
    }

    /**
     * For nodeType n or runtime property p get org.fcrepo.binary.Policy
     * implementation. Note: Signature might need to change, or a more
     * sophisticated method used, as implementation evolves.
     * 
     * @param propertyType
     * @param itemType
     * @param value
     * @return
     * @throws PolicyTypeException
     */
    protected Policy newPolicyInstance(final String propertyType,
        final String itemType, final String value) throws PolicyTypeException {

        switch (propertyType) {
            case NodeType.MIX_MIMETYPE:
            case "mix:mimeType":
                return new MimeTypePolicy(itemType, value);
            default:
                throw new PolicyTypeException("Mapping not found");
        }
    }

    /**
     * Delete NodeType. TODO for deleting multiple values with in a NodeType,
     * the design of how things are stored will need to change.
     */
    @DELETE
    @Timed
    public Response deleteNodeType(@PathParam("path") final String nodeType)
        throws RepositoryException {
        try {
            // final String[] str = StringUtils.split(request);
            // validateArgs(str.length);
            LOGGER.debug("Deleting node property{}", nodeType);
            final Node node =
                jcrTools.findOrCreateNode(session,
                    "/fedora:system/fedora:storage_policy", "test");
            if (isValidNodeTypeProperty(session, nodeType)) {
                node.getProperty(nodeType).remove();
                session.save();

                // remove all MimeType intances (since thats only the stored
                // Policy for now.
                // TODO Once Policy is updated to display Policy type, this
                // would change
                storagePolicyDecisionPoint.removeAll();
                return Response.noContent().build();
            } else {
                throw new RepositoryException(
                    "Invalid property type specified.");
            }
        } finally {
            session.logout();
        }
    }

    /**
     * TODO (for now) prints org.fcrepo.binary.PolicyDecisionPoint
     *
     * @return
     * @throws RepositoryException
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Timed
    public Response get(final @PathParam("path") String path) throws Exception {
        if (POLICY_RESOURCE.equalsIgnoreCase(path)) {
            return getAllStoragePolicies();
        } else {
            return getStoragePolicy(path);
        }
    }

    private Response getAllStoragePolicies() throws Exception {
        if (storagePolicyDecisionPoint == null ||
            storagePolicyDecisionPoint.size() == 0) {
            return Response.ok("No Policies Found").build();
        }
        return Response.ok(storagePolicyDecisionPoint.toString()).build();
    }

    private Response getStoragePolicy(final String nodeType) {
        LOGGER.debug("Get storage policy for: {}", nodeType);
        Response.ResponseBuilder response;
        try {
            final Node node =
                    jcrTools.findOrCreateNode(session,
                                              "/fedora:system/fedora:storage_policy",
                                              "test");

            Property prop = node.getProperty(nodeType);
            if (null == prop) {
                throw new PathNotFoundException("Policy not found: " + nodeType);
            }

            Value[] values = prop.getValues();
            if (values != null && values.length > 0) {
                response = Response.ok(values[0].getString());
            } else {
                throw new PathNotFoundException("Policy not found: " + nodeType);
            }

        } catch (PathNotFoundException e) {
            response = Response.status(404).entity(e.getMessage());
        } catch (Exception e) {
            response = Response.serverError().entity(e.getMessage());

        } finally {
            session.logout();
        }
        return response.build();
    }

    /**
     * Verifies whether node type is valid
     * 
     * @param session
     * @param type
     * @return
     * @throws RepositoryException
     */
    private boolean isValidNodeTypeProperty(final Session session,
        final String type) throws RepositoryException {
        try {
            return session.getWorkspace().getNodeTypeManager()
                .getNodeType(type).getName().equals(type);
        } catch (NoSuchNodeTypeException e) {
            LOGGER.debug("No corresponding Node type found for: {}", type);
            return false;
        }
    }

    /**
     * Consult some list of configuration of non JCR properties (e.g. list of
     * applicable runtime configurations)
     * 
     * @return
     * @throws PolicyTypeException
     */
    private boolean isValidConfigurationProperty(String property)
        throws PolicyTypeException {
        // TODO (for now) returns false. For future, need to represent & eval.
        // non node type props
        return false;
    }

    /**
     * TODO (for now) Simple validation
     * 
     * @param inputSize
     * @throws IllegalArgumentException
     */
    private void validateArgs(int inputSize) throws IllegalArgumentException {
        if (inputSize != InputPattern.valueOf(request.getMethod()).requiredLength) {
            throw new IllegalArgumentException("Invalid Arg");
        }
        // could do further checking here
    }

    private enum InputPattern {
        POST(3), DELETE(3);

        private final int requiredLength;

        private InputPattern(int l) {
            requiredLength = l;
        }
    }

    private JcrTools getJcrTools() {
        if (null == jcrTools) {
            this.jcrTools = new JcrTools(true);
        }
        return jcrTools;
    }

    /**
     * Only for UNIT TESTING
     * @param jcrTools
     */
    public void setJcrTools(JcrTools jcrTools) {
        this.jcrTools = jcrTools;
    }

    private UriInfo getUriInfo() {
        return this.uriInfo;
    }

    /**
     * Only for UNIT TESTING
     * @param uriInfo
     */
    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }
}
