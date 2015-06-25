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
package org.fcrepo.kernel.impl.utils;

import static com.google.common.collect.Iterables.toArray;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.getReferencePropertyName;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isInternalReferenceProperty;
import static org.fcrepo.kernel.impl.utils.FedoraTypesUtils.isMultivaluedProperty;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import com.hp.hpl.jena.rdf.model.Resource;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.exception.IdentifierConversionException;
import org.fcrepo.kernel.exception.NoSuchPropertyDefinitionException;
import org.fcrepo.kernel.identifiers.IdentifierConverter;
import org.fcrepo.kernel.services.functions.JcrPropertyFunctions;
import org.modeshape.jcr.IsExternal;
import org.slf4j.Logger;

/**
 * Tools for replacing, appending and deleting JCR node properties
 * @author Chris Beer
 * @since May 10, 2013
 */
public class NodePropertiesTools {

    private static final Logger LOGGER = getLogger(NodePropertiesTools.class);
    private static final IsExternal isExternal = new IsExternal();

    /**
     * Given a JCR node, property and value, either:
     *  - if the property is single-valued, replace the existing property with
     *    the new value
     *  - if the property is multivalued, append the new value to the property
     * @param node the JCR node
     * @param propertyName a name of a JCR property (either pre-existing or
     *   otherwise)
     * @param newValue the JCR value to insert
     * @return the property
     * @throws RepositoryException if repository exception occurred
     */
    public Property appendOrReplaceNodeProperty(final Node node, final String propertyName, final Value newValue)
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
                final List<Value> newValues = new ArrayList<>();
                Collections.addAll(newValues,
                                   node.getProperty(propertyName).getValues());

                if (!newValues.contains(newValue)) {
                    newValues.add(newValue);
                    property.setValue(toArray(newValues, Value.class));
                }
            } else {
                // or we'll just overwrite it
                LOGGER.debug("Overwriting {} property {} with new value {}", PropertyType.nameFromValue(property
                        .getType()), propertyName, newValue);
                property.setValue(newValue);
            }
        } else {
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
                node.setProperty(referencePropertyName, (Value[]) null);
            }
        }

        return property;
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
            final Node refNode = idTranslator.convert(resource).getNode();

            if (isExternal.apply(refNode)) {
                // we can't apply REFERENCE properties to external resources
                return;
            }

            final String referencePropertyName = getReferencePropertyName(propertyName);

            if (!isMultivaluedProperty(node, propertyName)) {
                if (node.hasProperty(referencePropertyName)) {
                    node.setProperty(referencePropertyName, (Value[]) null);
                }

                if (node.hasProperty(propertyName)) {
                    node.setProperty(propertyName, (Value) null);
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

        final Node refNode = idTranslator.convert(resource).getNode();
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
     * @return the property
     * @throws RepositoryException if repository exception occurred
     */
    public Property removeNodeProperty(final Node node, final String propertyName, final Value valueToRemove)
        throws RepositoryException {
        final Property property;

        // if the property doesn't exist, we don't need to worry about it.
        if (node.hasProperty(propertyName)) {

            property = node.getProperty(propertyName);

            if (JcrPropertyFunctions.isMultipleValuedProperty.apply(property)) {

                final List<Value> newValues = new ArrayList<>();

                boolean remove = false;

                for (final Value v : node.getProperty(propertyName).getValues()) {
                    if (v.equals(valueToRemove)) {
                        remove = true;
                    } else {
                        newValues.add(v);
                    }
                }

                // we only need to update the property if we did anything.
                if (remove) {
                    if (newValues.isEmpty()) {
                        LOGGER.debug("Removing property {}", propertyName);
                        property.setValue((Value[])null);
                    } else {
                        LOGGER.debug("Removing value {} from property {}",
                                     valueToRemove, propertyName);
                        property
                            .setValue(toArray(newValues, Value.class));
                    }
                }

            } else {
                if (property.getValue().equals(valueToRemove)) {
                    LOGGER.debug("Removing value from property {}", propertyName);
                    property.setValue((Value)null);
                }
            }
        } else {
            property = null;
        }

        return property;
    }
}
