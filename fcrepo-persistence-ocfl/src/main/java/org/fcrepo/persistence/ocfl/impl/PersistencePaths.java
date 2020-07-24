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

package org.fcrepo.persistence.ocfl.impl;

import org.fcrepo.kernel.api.identifiers.FedoraId;

/**
 * This class maps Fedora resources to locations on disk. It is based on this wiki:
 * https://wiki.lyrasis.org/display/FF/Design+-+Fedora+OCFL+Object+Structure
 *
 * @author pwinckles
 * @since 6.0.0
 */
public final class PersistencePaths {

    private static final String HEADER_DIR = ".fcrepo/";
    private static final String ROOT_PREFIX = "fcr-root";
    private static final String CONTAINER_PREFIX = "fcr-container";
    private static final String ACL_SUFFIX = "~fcr-acl";
    private static final String DESCRIPTION_SUFFIX = "~fcr-desc";
    private static final String RDF_EXTENSION = ".nt";
    private static final String JSON_EXTENSION = ".json";

    private enum ResourceType {
        CONTAINER,
        BINARY,
        CONTAINER_ACL,
        BINARY_ACL,
        BINARY_DESCRIPTION;

        static ResourceType fromRdfId(final FedoraId resourceId) {
            if (resourceId.isDescription()) {
                return BINARY_DESCRIPTION;
            }
            return CONTAINER;
        }

    }

    private PersistencePaths() {
        // static class
    }

    /**
     * Returns the path to the resourceId's header file. The rootId is the ide of the resource that's at the root of
     * and OCFL object. In the case of atomic resources the rootId and resourceId are one and the same. They are only
     * different for archival parts.
     *
     * @param rootId the id of the resource at the root of the OCFL object
     * @param resourceId the id of the resource to get the header path for
     * @return path to header file
     */
    public static String headerPath(final FedoraId rootId, final FedoraId resourceId) {
        String path;

        if (isRoot(rootId, resourceId)) {
            path = ROOT_PREFIX;
        } else {
            path = resolveRelativePath(rootId, resourceId);
        }

        if (resourceId.isAcl()) {
            path += ACL_SUFFIX;
        } else if (resourceId.isDescription()) {
            path += DESCRIPTION_SUFFIX;
        }

        return headerPath(path);
    }

    /**
     * Returns the path to the content file associated to resourceId. This method should only be used for non-rdf
     * resources. The rootId is the ide of the resource that's at the root of and OCFL object. In the case of atomic
     * resources the rootId and resourceId are one and the same. They are only different for archival parts.
     *
     * @param rootId the id of the resource at the root of the OCFL object
     * @param resourceId the id of the non-rdf resource to get the content path for
     * @return path to header file
     */
    public static String nonRdfContentPath(final FedoraId rootId, final FedoraId resourceId) {
        return resolveContentPath(ResourceType.BINARY, rootId, resourceId);
    }

    /**
     * Returns the path to the content file associated to resourceId. This method should only be used for rdf
     * resources. It should NOT be used for ACL resources. The rootId is the ide of the resource that's at the root of
     * and OCFL object. In the case of atomic resources the rootId and resourceId are one and the same. They are only
     * different for archival parts.
     *
     * @param rootId the id of the resource at the root of the OCFL object
     * @param resourceId the id of the rdf resource to get the content path for
     * @return path to header file
     */
    public static String rdfContentPath(final FedoraId rootId, final FedoraId resourceId) {
        if (resourceId.isAcl()) {
            throw new IllegalArgumentException("You must use resolveAclContentPath() for ACL resources.");
        }
        return resolveContentPath(ResourceType.fromRdfId(resourceId), rootId, resourceId);
    }

    /**
     * Returns the path to the content file associated to resourceId. This method should only be used for ACL resources.
     * The rootId is the ide of the resource that's at the root of and OCFL object. In the case of atomic resources the
     * rootId and resourceId are one and the same. They are only different for archival parts.
     *
     * @param describesRdfResource indicates if the acl is associated to a rdf resource
     * @param rootId the id of the resource at the root of the OCFL object
     * @param resourceId the id of the acl resource to get the content path for
     * @return path to header file
     */
    public static String aclContentPath(final boolean describesRdfResource,
                                        final FedoraId rootId, final FedoraId resourceId) {
        if (describesRdfResource) {
            return resolveContentPath(ResourceType.CONTAINER_ACL, rootId, resourceId);
        }
        return resolveContentPath(ResourceType.BINARY_ACL, rootId, resourceId);
    }

    /**
     * Returns true if the path is a header file.
     *
     * @param path path to test
     * @return true if header file
     */
    public static boolean isHeaderFile(final String path) {
        return path.startsWith(HEADER_DIR);
    }

    private static String resolveContentPath(final ResourceType resourceType,
                                             final FedoraId rootId, final FedoraId resourceId) {
        if (isRoot(rootId, resourceId)) {
            if (resourceType == ResourceType.CONTAINER) {
                return CONTAINER_PREFIX + RDF_EXTENSION;
            } else if (resourceType == ResourceType.CONTAINER_ACL) {
                return CONTAINER_PREFIX + ACL_SUFFIX + RDF_EXTENSION;
            }
        }

        var path = resolveRelativePath(rootId, resourceId);

        if (resourceType == ResourceType.BINARY_DESCRIPTION) {
            path += DESCRIPTION_SUFFIX + RDF_EXTENSION;
        } else if (resourceType == ResourceType.CONTAINER) {
            path += "/" + CONTAINER_PREFIX + RDF_EXTENSION;
        } else if (resourceType == ResourceType.CONTAINER_ACL) {
            path += "/" + CONTAINER_PREFIX + ACL_SUFFIX + RDF_EXTENSION;
        } else if (resourceType == ResourceType.BINARY_ACL) {
            path += ACL_SUFFIX + RDF_EXTENSION;
        }

        return path;
    }

    private static String resolveRelativePath(final FedoraId rootId, final FedoraId resourceId) {
        if (isRoot(rootId, resourceId)) {
            final var idStr = resourceId.getBaseId();
            return idStr.substring(idStr.lastIndexOf('/') + 1);
        }
        if (!resourceId.getBaseId().startsWith(rootId.getBaseId())) {
            throw new IllegalArgumentException(String.format("The IDs %s and %s are unrelated",
                    resourceId, rootId));
        }
        return resourceId.getBaseId().substring(rootId.getBaseId().length() + 1);
    }

    private static String headerPath(final String path) {
        return HEADER_DIR + path + JSON_EXTENSION;
    }

    private static boolean isRoot(final FedoraId rootId, final FedoraId resourceId) {
        return rootId.getBaseId().equals(resourceId.getBaseId());
    }

}
