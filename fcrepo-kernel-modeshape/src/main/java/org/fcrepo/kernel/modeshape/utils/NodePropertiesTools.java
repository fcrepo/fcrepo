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
package org.fcrepo.kernel.modeshape.utils;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static org.fcrepo.kernel.modeshape.FedoraJcrConstants.FIELD_DELIMITER;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isExternalNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isInternalReferenceProperty;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isMultivaluedProperty;
import static org.fcrepo.kernel.modeshape.utils.UncheckedPredicate.uncheck;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jena.rdf.model.Resource;

import org.apache.jena.vocabulary.RDF;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.exception.IdentifierConversionException;
import org.fcrepo.kernel.api.exception.NoSuchPropertyDefinitionException;
import org.fcrepo.kernel.api.identifiers.IdentifierConverter;
import org.slf4j.Logger;

/**
 * Tools for replacing, appending and deleting JCR node properties
 * @author Chris Beer
 * @author ajs6f
 * @since May 10, 2013
 */
public class NodePropertiesTools {

    private static final Logger LOGGER = getLogger(NodePropertiesTools.class);

    /**
     * Given a JCR node, property and value, either:
     *  - if the property is single-valued, replace the existing property with
     *    the new value
     *  - if the property is multivalued, append the new value to the property
     * @param node the JCR node
     * @param propertyName a name of a JCR property (either pre-existing or
     *   otherwise)
     * @param newValue the JCR value to insert
     * @throws RepositoryException if repository exception occurred
     */
    public void appendOrReplaceNodeProperty(final Node node, final String propertyName, final Value newValue)
        throws RepositoryException {

        final Property property;

        // if it already exists, we can take some shortcuts
        if (node.hasProperty(propertyName)) {

            property = node.getProperty(propertyName);

            if (property.isMultiple()) {
                LOGGER.debug("Appending value {} to {} property {}", newValue,
                             PropertyType.nameFromValue(property.getType()),
                             propertyName);

                // if the property is multi-valued, go ahead and append to it.
                final List<Value> newValues = new ArrayList<>(asList(node.getProperty(propertyName).getValues()));

                if (!newValues.contains(newValue)) {
                    newValues.add(newValue);
                    property.setValue(newValues.toArray(new Value[newValues.size()]));
                }
            } else {
                // or we'll just overwrite its single value
                LOGGER.debug("Overwriting {} property {} with new value {}", PropertyType.nameFromValue(property
                        .getType()), propertyName, newValue);
                property.setValue(newValue);
            }
        } else {
            // we're creating a new property on this node, so we check whether it should be multi-valued
            boolean isMultiple = true;
            try {
                isMultiple = isMultivaluedProperty(node, propertyName);
            } catch (final NoSuchPropertyDefinitionException e) {
                // simply represents a new kind of property on this node
            }
            if (isMultiple) {
                LOGGER.debug("Creating new multivalued {} property {} with " +
                             "initial value [{}]",
                             PropertyType.nameFromValue(newValue.getType()),
                             propertyName, newValue);
                property = node.setProperty(propertyName, new Value[]{newValue}, newValue.getType());
            } else {
                LOGGER.debug("Creating new single-valued {} property {} with " +
                             "initial value {}",
                             PropertyType.nameFromValue(newValue.getType()),
                             propertyName, newValue);
                property = node.setProperty(propertyName, newValue, newValue.getType());
            }
        }

        if (!property.isMultiple() && !isInternalReferenceProperty.test(property)) {
            final String referencePropertyName = getReferencePropertyName(propertyName);
            if (node.hasProperty(referencePropertyName)) {
                node.getProperty(referencePropertyName).remove();
            }
        }
    }

    /**
     * Add a reference placeholder from one node to another in-domain resource
     * @param idTranslator the id translator
     * @param node the node
     * @param propertyName the property name
     * @param resource the resource
     * @throws RepositoryException if repository exception occurred
     */
    public void addReferencePlaceholders(final IdentifierConverter<Resource,FedoraResource> idTranslator,
                                          final Node node,
                                          final String propertyName,
                                          final Resource resource) throws RepositoryException {

        try {
            final Node refNode = getJcrNode(idTranslator.convert(resource));

            if (isExternalNode.test(refNode)) {
                // we can't apply REFERENCE properties to external resources
                return;
            }

            final String referencePropertyName = getReferencePropertyName(propertyName);

            if (!isMultivaluedProperty(node, propertyName)) {
                if (node.hasProperty(referencePropertyName)) {
                    node.getProperty(referencePropertyName).remove();
                }

                if (node.hasProperty(propertyName)) {
                    node.getProperty(propertyName).remove();
                }
            }

            final Value v = node.getSession().getValueFactory().createValue(refNode, true);
            appendOrReplaceNodeProperty(node, referencePropertyName, v);

        } catch (final IdentifierConversionException e) {
            // no-op
        }
    }

    /**
     * Remove a reference placeholder that links one node to another in-domain resource
     * @param idTranslator the id translator
     * @param node the node
     * @param propertyName the property name
     * @param resource the resource
     * @throws RepositoryException if repository exception occurred
     */
    public void removeReferencePlaceholders(final IdentifierConverter<Resource,FedoraResource> idTranslator,
                                             final Node node,
                                             final String propertyName,
                                             final Resource resource) throws RepositoryException {

        final String referencePropertyName = getReferencePropertyName(propertyName);

        final Node refNode = getJcrNode(idTranslator.convert(resource));
        final Value v = node.getSession().getValueFactory().createValue(refNode, true);
        removeNodeProperty(node, referencePropertyName, v);
    }
    /**
     * Given a JCR node, property and value, remove the value (if it exists)
     * from the property, and remove the
     * property if no values remove
     *
     * @param node the JCR node
     * @param propertyName a name of a JCR property (either pre-existing or
     *   otherwise)
     * @param valueToRemove the JCR value to remove
     * @throws RepositoryException if repository exception occurred
     */
    public void removeNodeProperty(final Node node, final String propertyName, final Value valueToRemove)
        throws RepositoryException {
        LOGGER.debug("Request to remove {}", valueToRemove);
        // if the property doesn't exist, we don't need to worry about it.
        if (node.hasProperty(propertyName)) {

            final Property property = node.getProperty(propertyName);
            final String strValueToRemove = valueToRemove.getString();
            final String strValueToRemoveWithoutStringType = removeStringTypes(strValueToRemove);

            if (property.isMultiple()) {
                final AtomicBoolean remove = new AtomicBoolean();
                final Value[] newValues = stream(node.getProperty(propertyName).getValues()).filter(uncheck(v -> {
                    final String strVal = removeStringTypes(v.getString());

                    LOGGER.debug("v is '{}', valueToRemove is '{}'", v, strValueToRemove );
                    if (strVal.equals(strValueToRemoveWithoutStringType)) {
                        remove.set(true);
                        return false;
                    }

                    return true;
                })).toArray(Value[]::new);

                // we only need to update the property if we did anything.
                if (remove.get()) {
                    if (newValues.length == 0) {
                        LOGGER.debug("Removing property '{}'", propertyName);
                        property.remove();
                    } else {
                        LOGGER.debug("Removing value '{}' from property '{}'", strValueToRemove, propertyName);
                        property.setValue(newValues);
                    }
                } else {
                    LOGGER.debug("Value not removed from property name '{}' (value '{}')", propertyName,
                            strValueToRemove);
                }
            } else {

                final String strPropVal = property.getValue().getString();
                final String strPropValWithoutStringType = removeStringTypes(strPropVal);

                LOGGER.debug("Removing string '{}'", strValueToRemove);
                if (StringUtils.equals(strPropValWithoutStringType, strValueToRemoveWithoutStringType)) {
                    LOGGER.debug("single value: Removing value from property '{}'", propertyName);
                    property.remove();
                } else {
                    LOGGER.debug("Value not removed from property name '{}' (property value: '{}';compare value: '{}')",
                            propertyName, strPropVal, strValueToRemove);
                    throw new RepositoryException("Property '" + propertyName + "': Unable to remove value '" +
                            StringUtils.substring(strValueToRemove, 0, 50) + "'");
                }
            }
        }
    }

    private String removeStringTypes(final String value) {
        if (value != null) {
            // Remove string datatype
            String v = value.replace(FIELD_DELIMITER + XSDstring.getURI(), "");

            // Remove lang datatype
            v = v.replace(FIELD_DELIMITER + RDF.dtLangString.getURI(), FIELD_DELIMITER);

            // Remove lang placeholder
            return v.replace(FIELD_DELIMITER + FIELD_DELIMITER, "");
        }
        return null;
    }
}
