package org.fcrepo.generator.rdf;

import com.hp.hpl.jena.rdf.model.Resource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public interface TripleGenerator {

    public void updateResourceFromNode(Resource resource, Node node) throws RepositoryException;

}
