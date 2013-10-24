/**
 * Copyright 2013 DuraSpace, Inc.
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
package org.fcrepo.kernel.utils;

import static org.fcrepo.kernel.utils.FedoraTypesUtils.getDefinitionForPropertyName;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.fcrepo.kernel.exception.NoSuchPropertyDefinitionException;
import org.slf4j.Logger;

/**
 * Tools for replacing, appending and deleting JCR node properties
 * @author Chris Beer
 * @date May 10, 2013
 */
public abstract class NodePropertiesTools {

    private static final Logger logger = getLogger(NodePropertiesTools.class);

    /**
     * Given a JCR node, property and value, either:
     *  - if the property is single-valued, replace the existing property with
     *    the new value
     *  - if the property is multivalued, append the new value to the property
     * @param node the JCR node
     * @param propertyName a name of a JCR property (either pre-existing or
     *   otherwise)
     * @param newValue the JCR value to insert
     * @throws RepositoryException
     */
    public static void appendOrReplaceNodeProperty(final Node node,
                                                   final String propertyName,
                                                   final Value newValue)
        throws RepositoryException {

        // if it already exists, we can take some shortcuts
        if (node.hasProperty(propertyName)) {

            final Property property = node.getProperty(propertyName);

            if (property.isMultiple()) {
                logger.debug("Appending value {} to {} property {}", newValue,
                             PropertyType.nameFromValue(property.getType()),
                             propertyName);

                // if the property is multi-valued, go ahead and append to it.
                final ArrayList<Value> newValues = new ArrayList<Value>();
                Collections.addAll(newValues,
                                   node.getProperty(propertyName).getValues());

                if (!newValues.contains(newValue)) {
                    newValues.add(newValue);
                    property.setValue(newValues
                                      .toArray(new Value[newValues.size()]));
                }
            } else {
                // or we'll just overwrite it
                logger.debug("Overwriting {} property {} with new value {}",
                             PropertyType.nameFromValue(property.getType()),
                             propertyName, newValue);
                property.setValue(newValue);
            }
        } else {
            boolean isMultiple = false;
            try {
                isMultiple = isMultivaluedProperty(node, propertyName);

            } catch (final NoSuchPropertyDefinitionException e) {
                // simply represents a new kind of property on this node
            }
            if (isMultiple) {
                logger.debug("Creating new multivalued {} property {} with " +
                             "initial value [{}]",
                             PropertyType.nameFromValue(newValue.getType()),
                             propertyName, newValue);
                node.setProperty(propertyName, new Value[]{newValue}, newValue.getType());
            } else {
                logger.debug("Creating new single-valued {} property {} with " +
                             "initial value {}",
                             PropertyType.nameFromValue(newValue.getType()),
                             propertyName, newValue);
                node.setProperty(propertyName, newValue, newValue.getType());
            }
        }

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
     * @throws RepositoryException
     */
    public static void removeNodeProperty(final Node node,
                                          final String propertyName,
                                          final Value valueToRemove)
        throws RepositoryException {
        // if the property doesn't exist, we don't need to worry about it.
        if (node.hasProperty(propertyName)) {

            final Property property = node.getProperty(propertyName);

            if (FedoraTypesUtils.isMultipleValuedProperty.apply(property)) {

                final ArrayList<Value> newValues = new ArrayList<Value>();

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
                    if (newValues.size() == 0) {
                        logger.debug("Removing property {}", propertyName);
                        property.setValue((Value[])null);
                    } else {
                        logger.debug("Removing value {} from property {}",
                                     valueToRemove, propertyName);
                        property
                            .setValue(newValues
                                      .toArray(new Value[newValues.size()]));
                    }
                }

            } else {
                if (property.getValue().equals(valueToRemove)) {
                    logger.debug("Removing value {} property {}", propertyName);
                    property.setValue((Value)null);
                }
            }
        }
    }

    /**
     * Get the JCR property type ID for a given property name. If unsure, mark
     * it as UNDEFINED.
     *
     * @param node the JCR node to add the property on
     * @param propertyName the property name
     * @return a PropertyType value
     * @throws RepositoryException
     */
    public static int getPropertyType(final Node node,
                                      final String propertyName)
        throws RepositoryException {
        final PropertyDefinition def =
            getDefinitionForPropertyName(node, propertyName);

        if (def == null) {
            return PropertyType.UNDEFINED;
        }

        return def.getRequiredType();
    }

    /**
     * Determine if a given JCR property name is single- or multi- valued.
     * If unsure, choose the least restrictive
     * option (multivalued)
     *
     * @param node the JCR node to check
     * @param propertyName the property name
     *   (which may or may not already exist)
     * @return true if the property is (or could be) multivalued
     * @throws RepositoryException
     */
    public static boolean isMultivaluedProperty(final Node node,
                                                final String propertyName)
        throws RepositoryException {
        final PropertyDefinition def =
            getDefinitionForPropertyName(node, propertyName);

        if (def == null) {
            throw new NoSuchPropertyDefinitionException();
        }

        return def.isMultiple();
    }

}
