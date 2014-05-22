/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.auth.roles.common;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.hp.hpl.jena.rdf.model.Property;

/**
 * Reference values for access roles node types and paths.
 *
 * @author Gregory Jansen
 */
public class Constants {

    /**
     * No public constructor for utility class
     */
    private Constants() {
    }

    /**
     * Authorization JCR names
     *
     * @author Gregory Jansen
     */
    public static enum JcrName {
        rbaclAssignable(), Rbacl(), Assignment(), rbacl(), assignment(),
        principal(), role();

        private String expandedName;

        private String qualifiedName;

        private Property property;

        public static final String NS_URI =
                "http://fedora.info/definitions/v4/authorization#";

        public static final String NS_PREFIX = "authz";

        JcrName() {
            this.expandedName = '{' + NS_URI + '}' + this.name();
            this.qualifiedName = NS_PREFIX + ':' + this.name();
            this.property = createProperty(NS_URI + this.name());
        }

        /**
         * Get the fully qualified name of this JCR type.
         *
         * @return expanded name
         */
        public String getExpanded() {
            return this.expandedName;
        }

        /**
         * Get the qualified name of this JCR type.
         *
         * @return qualified name
         */
        public String getQualified() {
            return this.qualifiedName;
        }

        /**
         * Get the property for this URI.
         *
         * @return a Jena property
         */
        public Property getProperty() {
            return this.property;
        }

        /*
         * (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return getQualified();
        }

    }

    /**
     * Adds access roles prefix "authz" to the given session.
     * @param session
     * @throws RepositoryException
     */
    public static void registerPrefixes(final Session session)
        throws RepositoryException {
        session.setNamespacePrefix(JcrName.NS_PREFIX, JcrName.NS_URI);
    }
}
