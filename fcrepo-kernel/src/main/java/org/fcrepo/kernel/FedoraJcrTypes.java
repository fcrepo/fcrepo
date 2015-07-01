/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.kernel;

import static com.google.common.collect.ImmutableList.of;
import java.util.List;

/**
 * Convenience class with constants for commonly used JCR types.
 *
 * @author ajs6f
 * @since Apr 25, 2013
 */
public interface FedoraJcrTypes {

    String FEDORA_RESOURCE = "fedora:Resource";

    String FEDORA_NON_RDF_SOURCE_DESCRIPTION = "fedora:NonRdfSourceDescription";

    String FEDORA_BINARY = "fedora:Binary";

    String FEDORA_PAIRTREE = "fedora:Pairtree";

    String FEDORA_TOMBSTONE = "fedora:Tombstone";

    String FEDORA_SKOLEM = "fedora:Skolem";

    String FEDORA_CONTAINER = "fedora:Container";

    String LDP_BASIC_CONTAINER = "ldp:BasicContainer";

    String LDP_DIRECT_CONTAINER = "ldp:DirectContainer";

    String LDP_INDIRECT_CONTAINER = "ldp:IndirectContainer";

    String LDP_INSERTED_CONTENT_RELATION = "ldp:insertedContentRelation";

    String JCR_LASTMODIFIED = "jcr:lastModified";

    String JCR_CREATED = "jcr:created";

    String JCR_CREATEDBY = "jcr:createdBy";

    String PREMIS_FILE_NAME = "premis:hasOriginalName";

    String CONTENT_SIZE = "premis:hasSize";

    String CONTENT_DIGEST = "premis:hasMessageDigest";

    String FCR_METADATA = "fcr:metadata";

    String FCR_VERSIONS = "fcr:versions";

    String ROOT = "mode:root";

    String FROZEN_NODE = "nt:frozenNode";

    String JCR_FROZEN_NODE = "jcr:frozenNode";

    String FROZEN_MIXIN_TYPES = "jcr:frozenMixinTypes";

    String JCR_PRIMARY_TYPE = "jcr:primaryType";

    String JCR_MIXIN_TYPES = "jcr:mixinTypes";

    String VERSIONABLE = "mix:versionable";

    String LDP_HAS_MEMBER_RELATION = "ldp:hasMemberRelation";
    String LDP_IS_MEMBER_OF_RELATION = "ldp:isMemberOfRelation";
    String LDP_MEMBER_RESOURCE = "ldp:membershipResource";

    List<String> EXPOSED_PROTECTED_JCR_TYPES = of(JCR_LASTMODIFIED, JCR_CREATED, JCR_CREATEDBY,
            JCR_PRIMARY_TYPE, JCR_MIXIN_TYPES);
}
