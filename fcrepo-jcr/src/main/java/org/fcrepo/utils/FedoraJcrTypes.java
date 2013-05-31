/**
 * The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.utils;

/**
 * Convenience class with constants for commonly used JCR types.
 *
 * @author ajs6f
 * @date Apr 25, 2013
 */
public interface FedoraJcrTypes {

    String FEDORA_RESOURCE = "fedora:resource";

    String FEDORA_DATASTREAM = "fedora:datastream";

    String FEDORA_OBJECT = "fedora:object";

    String FEDORA_BINARY = "fedora:binary";

    String JCR_LASTMODIFIED = "jcr:lastModified";

    String JCR_CREATED = "jcr:created";

    String JCR_CREATEDBY = "jcr:createdby";

    String CONTENT_SIZE = "fedora:size";

    String CONTENT_DIGEST = "fedora:digest";

    String FCR_CONTENT = "fcr:content";

    String ROOT = "mode:root";
}
