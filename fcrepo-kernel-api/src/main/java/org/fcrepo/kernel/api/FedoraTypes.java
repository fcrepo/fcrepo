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
package org.fcrepo.kernel.api;

/**
 * Convenience class with constants for commonly used Fedora types.
 *
 * @author ajs6f
 * @since Apr 25, 2013
 */
public interface FedoraTypes {

    String FEDORA_BINARY = "fedora:Binary";

    String FEDORA_CONTAINER = "fedora:Container";

    String FEDORA_NON_RDF_SOURCE_DESCRIPTION = "fedora:NonRdfSourceDescription";

    String FEDORA_PAIRTREE = "fedora:Pairtree";

    String FEDORA_REPOSITORY_ROOT = "fedora:RepositoryRoot";

    String FEDORA_RESOURCE = "fedora:Resource";

    String FEDORA_SKOLEM = "fedora:Skolem";

    String FEDORA_TOMBSTONE = "fedora:Tombstone";

    String FEDORA_TIME_MAP = "fedora:TimeMap";

    String FEDORA_WEBAC_ACL = "webac:Acl";

    String MEMENTO = "memento:Memento";

    String MEMENTO_DATETIME = "memento:mementoDatetime";

    String MEMENTO_ORIGINAL = "memento:original";

    String LDP_BASIC_CONTAINER = "ldp:BasicContainer";

    String LDP_CONTAINER = "ldp:Container";

    String LDP_DIRECT_CONTAINER = "ldp:DirectContainer";

    String LDP_INDIRECT_CONTAINER = "ldp:IndirectContainer";

    String LDP_INSERTED_CONTENT_RELATION = "ldp:insertedContentRelation";

    String LDP_NON_RDF_SOURCE = "ldp:NonRDFSource";

    String LDP_RDF_SOURCE = "ldp:RDFSource";

    String FEDORA_LASTMODIFIED = "fedora:lastModified";

    String FEDORA_CREATED = "fedora:created";

    String FEDORA_LASTMODIFIEDBY = "fedora:lastModifiedBy";

    String FEDORA_CREATEDBY = "fedora:createdBy";

    String FILENAME = "ebucore:filename";

    String HAS_MIME_TYPE = "ebucore:hasMimeType";

    String CONTENT_SIZE = "premis:hasSize";

    String CONTENT_DIGEST = "premis:hasMessageDigest";

    String DEFAULT_DIGEST_ALGORITHM = "fedoraconfig:defaultDigestAlgorithm";

    String FCR_ACL = "fcr:acl";

    String FCR_METADATA = "fcr:metadata";

    String FCR_VERSIONS = "fcr:versions";

    String FCR_FIXITY = "fcr:fixity";

    String LDP_HAS_MEMBER_RELATION = "ldp:hasMemberRelation";

    String LDP_IS_MEMBER_OF_RELATION = "ldp:isMemberOfRelation";

    String LDP_MEMBER_RESOURCE = "ldp:membershipResource";
}
