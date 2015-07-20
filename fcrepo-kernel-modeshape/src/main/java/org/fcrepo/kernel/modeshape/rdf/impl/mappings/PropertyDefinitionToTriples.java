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
package org.fcrepo.kernel.modeshape.rdf.impl.mappings;

import static com.google.common.base.Throwables.propagate;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDanyURI;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDboolean;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDdate;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDdecimal;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDlong;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static com.hp.hpl.jena.vocabulary.RDFS.range;
import static java.util.Collections.emptyIterator;
import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.URI;
import static javax.jcr.PropertyType.WEAKREFERENCE;
import static javax.jcr.PropertyType.nameFromValue;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;

import org.fcrepo.kernel.api.utils.iterators.RdfStream;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * Utility for moving Property Definitions into RDFS triples
 * @author cbeer
 */
public class PropertyDefinitionToTriples extends ItemDefinitionToTriples<PropertyDefinition> {

    private static final Logger LOGGER = getLogger(PropertyDefinitionToTriples.class);

    /**
     * A JCR type for which we know no RDF equivalent.
     */
    private static final Node UNMAPPED_TYPE = createURI(REPOSITORY_NAMESPACE
            + "UnmappedType");

    /**
     * A map from JCR types to RDF types.
     */
    private static final Map<Integer, XSDDatatype> JCR_TYPE_TO_XSD_DATATYPE =
            ImmutableMap.<Integer, XSDDatatype>builder()
            .put(BOOLEAN, XSDboolean)
            .put(DATE, XSDdate)
            .put(DECIMAL, XSDdecimal)
            .put(DOUBLE, XSDdecimal)
            .put(LONG, XSDlong)
            .put(URI, XSDanyURI)
            .put(REFERENCE, XSDanyURI)
            .put(WEAKREFERENCE, XSDanyURI)
            .put(PATH, XSDanyURI)
            .put(BINARY, XSDstring)
            .put(STRING, XSDstring).build();

    /**
     * Translate ItemDefinitions into triples. The definitions will hang off
     * the provided RDF Node
     * @param domain the given domain
     */
    public PropertyDefinitionToTriples(final Node domain) {
        super(domain);
    }

    @Override
    public Iterator<Triple> apply(final PropertyDefinition input) {

        if (!input.getName().contains(":")) {
            LOGGER.debug("Received property definition with no namespace: {}",
                    input.getName());
            LOGGER.debug("This cannot be serialized into several RDF formats, " +
                                 "so we assume it is internal and discard it.");
            // TODO find a better way...
            return emptyIterator();
        }

        try {
            // skip range declaration for unknown types
            final int requiredType = input.getRequiredType();

            final Node rangeForJcrType = getRangeForJcrType(requiredType);

            if (!rangeForJcrType.equals(UNMAPPED_TYPE)) {
                LOGGER.trace("Adding RDFS:range for property: {} with required type: {} as: {}",
                    input.getName(), nameFromValue(requiredType), rangeForJcrType.getURI());
                final Triple propertyTriple =
                    create(getResource(input).asNode(), range.asNode(),
                            rangeForJcrType);
                return new RdfStream(propertyTriple).concat(super.apply(input));
            }
            LOGGER.trace(
                    "Skipping RDFS:range for property: {} with unmappable type: {}",
                    input.getName(), nameFromValue(requiredType));
            return super.apply(input);
        } catch (final RepositoryException e) {
            throw propagate(e);
        }
    }

    /**
     * Convert a JCR type to an RDF data type.
     *
     * @param requiredType a JCR PropertyType
     * @return rdf node of data type
     */
    private static Node getRangeForJcrType(final int requiredType) {
        return JCR_TYPE_TO_XSD_DATATYPE.containsKey(requiredType)
            ? createURI(JCR_TYPE_TO_XSD_DATATYPE.get(requiredType).getURI())
            : UNMAPPED_TYPE;
    }
}
