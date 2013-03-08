package org.fcrepo.generator.rdf;

import com.hp.hpl.jena.rdf.model.Resource;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class JcrPropertiesGenerator implements TripleGenerator {

    @Override
    public void updateResourceFromNode(final Resource resource, final Node node) throws RepositoryException {
        final PropertyIterator properties = node.getProperties();

        while(properties.hasNext()) {

            final Property property = (Property) properties.next();

            if(property.isMultiple()) {
                for(final Value v : property.getValues()) {
                    addPropertyToResource(resource, property, v);
                }
            }
            else {
                addPropertyToResource(resource, property, property.getValue());
            }
        }
    }

    private void addPropertyToResource(final Resource resource, final Property property, final Value v) throws RepositoryException {
        String n = property.getName();

        final String[] parts = n.split(":", 2);

        String namespace = property.getParent().getSession().getWorkspace().getNamespaceRegistry().getURI(parts[0]);

        String local_part = parts[1];

        resource.addProperty(resource.getModel().createProperty(namespace, local_part), v.getString());
   }
}
