
package org.fcrepo.legacy.foxml;

import java.io.IOException;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FOXMLSequencer extends Sequencer {

    private final Logger log = LoggerFactory.getLogger(FOXMLSequencer.class);

    private FOXMLParser parser;

    public String description;

    @Override
    public boolean execute(Property inputProperty, Node outputNode,
            Context context) {

        log.debug("Now sequencing FOXML with: ");
        try {
            log.debug(parser.report());
            parser.parse(inputProperty.getBinary().getStream(), outputNode);
            return true;
        } catch (Exception e) {
            log.error("Failed to parse FOXML!", e);
            return false;
        }

    }

    @Override
    public void initialize(NamespaceRegistry registry,
            NodeTypeManager nodeTypeManager) throws RepositoryException,
            IOException {
        // registerDefaultMimeTypes("text/xml");
        getLogger().debug(
                "Initializing " + getClass().getCanonicalName() + "[" +
                        getName() + "]");
        parser = new FOXMLParser();
    }

}