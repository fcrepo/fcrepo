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
package org.fcrepo.kernel.modeshape;

import static com.google.common.collect.ImmutableSet.of;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;

import java.util.Set;

import org.apache.jena.rdf.model.Property;

/**
 * @author acoburn
 */
public final class RdfJcrLexicon {

    /**
     *  The core JCR namespace.
     */
    public static final String JCR_NAMESPACE = "http://www.jcp.org/jcr/1.0";

    public static final String MODE_NAMESPACE = "http://www.modeshape.org/1.0";

    public static final String MIX_NAMESPACE = "http://www.jcp.org/jcr/mix/1.0";

    public static final String JCR_NT_NAMESPACE = "http://www.jcp.org/jcr/nt/1.0";

    // IMPORTANT JCR PROPERTIES
    public static final Property HAS_PRIMARY_IDENTIFIER =
            createProperty(REPOSITORY_NAMESPACE + "uuid");
    public static final Property HAS_PRIMARY_TYPE =
            createProperty(REPOSITORY_NAMESPACE + "primaryType");
    public static final Property HAS_NODE_TYPE =
            createProperty(REPOSITORY_NAMESPACE + "hasNodeType");
    public static final Property HAS_MIXIN_TYPE =
            createProperty(REPOSITORY_NAMESPACE + "mixinTypes");

    public static final Set<Property> jcrProperties = of(
            HAS_PRIMARY_IDENTIFIER, HAS_PRIMARY_TYPE, HAS_NODE_TYPE,
            HAS_MIXIN_TYPE);

    private RdfJcrLexicon() {
        // prevent instantiation
    }
}
