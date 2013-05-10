package org.fcrepo.utils;

import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.ArrayList;
import java.util.Collections;

import static org.fcrepo.utils.FedoraTypesUtils.getDefinitionForPropertyName;
import static org.slf4j.LoggerFactory.getLogger;

public class NodePropertiesTools {

    private static final Logger logger = getLogger(NodePropertiesTools.class);

    public static void appendOrReplaceNodeProperty(final Node node, final String propertyName, final Value newValue) throws RepositoryException {

        // if it already exists, we can take some shortcuts
        if (node.hasProperty(propertyName)) {

            final Property property = node.getProperty(propertyName);

            if (property.isMultiple()) {
                logger.debug("Appending value {} to {} property {}", newValue, PropertyType.nameFromValue(property.getType()), propertyName);

                // if the property is multi-valued, go ahead and append to it.
                ArrayList<Value> newValues = new ArrayList<Value>();
                Collections.addAll(newValues, node.getProperty(propertyName).getValues());
                newValues.add(newValue);

                property.setValue((Value[]) newValues.toArray(new Value[newValues.size()]));
            } else {
                // or we'll just overwrite it
                logger.debug("Overwriting {} property {} with new value {}", PropertyType.nameFromValue(property.getType()), propertyName, newValue);
                property.setValue(newValue);
            }
        } else {
            if (isMultivaluedProperty(node, propertyName)) {
                logger.debug("Creating new multivalued {} property {} with initial value [{}]", PropertyType.nameFromValue(newValue.getType()), propertyName, newValue);
                node.setProperty(propertyName, new Value[]{newValue});
            } else {
                logger.debug("Creating new single-valued {} property {} with initial value {}", PropertyType.nameFromValue(newValue.getType()), propertyName, newValue);
                node.setProperty(propertyName, newValue);
            }
        }

    }

    public static void removeNodeProperty(final Node node, final String propertyName, final Value valueToRemove) throws RepositoryException {
        // if the property doesn't exist, we don't need to worry about it.
        if (node.hasProperty(propertyName)) {

            final Property property = node.getProperty(propertyName);

            if (FedoraTypesUtils.isMultipleValuedProperty.apply(property)) {

                ArrayList<Value> newValues = new ArrayList<Value>();

                Collections.addAll(newValues, node.getProperty(propertyName).getValues());
                final boolean remove = newValues.remove(valueToRemove);

                // we only need to update the property if we did anything.
                if (remove) {
                    if (newValues.size() == 0) {
                        logger.debug("Removing property {}", propertyName);
                        property.setValue((Value[])null);
                    } else {
                        logger.debug("Removing value {} from property {}", valueToRemove, propertyName);
                        property.setValue((Value[]) newValues.toArray(new Value[newValues.size()]));
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

    public static int getPropertyType(final Node node, final String propertyName) throws RepositoryException {
        final PropertyDefinition def = getDefinitionForPropertyName(node, propertyName);

        if (def == null) {
            return PropertyType.UNDEFINED;
        }

        return def.getRequiredType();
    }

    public static boolean isMultivaluedProperty(final Node node, final String propertyName) throws RepositoryException {
        final PropertyDefinition def = getDefinitionForPropertyName(node, propertyName);

        if (def == null) {
            return true;
        }

        return def.isMultiple();
    }


}
