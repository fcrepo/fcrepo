/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api;

/**
 * Convenience class with constants for commonly used Fedora types.
 *
 * @author ajs6f
 * @since Apr 25, 2013
 */
public interface FedoraTypes {

    String FEDORA_ID_PREFIX = "info:fedora";

    String FCR_ACL = "fcr:acl";

    String FCR_METADATA = "fcr:metadata";

    String FCR_VERSIONS = "fcr:versions";

    String FCR_FIXITY = "fcr:fixity";

    String FCR_TOMBSTONE = "fcr:tombstone";

    String FCR_TX = "fcr:tx";
}
