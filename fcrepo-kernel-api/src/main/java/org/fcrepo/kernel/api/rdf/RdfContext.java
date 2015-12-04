/*
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
package org.fcrepo.kernel.api.rdf;

/**
 * A collection of RDF contexts that can be used to extract triples from FedoraResources
 *
 * @author acoburn
 * @since Dec 4, 2015
 */
public enum RdfContext {

    /* Acl Context */
    ACL,

    /* Child Context */
    CHILDREN,

    /* Binary Content Context */
    CONTENT,

    /* Fixity Context */
    FIXITY,

    /* HashURI Context */
    HASH_URI,

    /* LDP Containment Context */
    LDP_CONTAINMENT,

    /* LDP Membership Context */
    LDP_MEMBERSHIP,

    /* LDP Type Context */
    LDP,

    /* Parent Resource Context */
    PARENT,

    /* Rdf Properties Context */
    PROPERTIES,

    /* Rdf Type Context */
    RDF_TYPE,

    /* Inbound References Context */
    REFERENCES,

    /* Repository Root Context */
    ROOT,

    /* Skokem Node Context */
    SKOLEM,

    /* Versioning Context */
    VERSIONS
}

