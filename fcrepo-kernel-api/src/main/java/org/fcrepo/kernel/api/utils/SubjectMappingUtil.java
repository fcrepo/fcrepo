/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.utils;

import static org.apache.jena.graph.NodeFactory.createURI;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * Utility for remapping subjects in rdf triples.
 *
 * @author bbpennel
 */
public class SubjectMappingUtil {

    private SubjectMappingUtil() {
        // Empty constructor for static utility class
    }

    /**
     * Maps the subject of t from resourceUri to destinationUri to produce a new Triple.
     * If the triple does not have the subject resourceUri, then the triple is unchanged.
     *
     * @param t triple to be remapped.
     * @param resourceUri resource subject uri to be remapped.
     * @param destinationUri subject uri for the resultant triple.
     * @return triple with subject remapped to destinationUri or the original subject.
     */
    public static Triple mapSubject(final Triple t, final String resourceUri, final String destinationUri) {
        final Node destinationNode = createURI(destinationUri);
        return mapSubject(t, resourceUri, destinationNode);
    }

    /**
     * Maps the subject of t from resourceUri to destinationNode to produce a new Triple.
     * If the triple does not have the subject resourceUri, then the triple is unchanged.
     *
     * @param t triple to be remapped.
     * @param resourceUri resource subject uri to be remapped.
     * @param destinationNode subject node for the resultant triple.
     * @return triple with subject remapped to destinationNode or the original subject.
     */
    public static Triple mapSubject(final Triple t, final String resourceUri, final Node destinationNode) {
        final Node tripleSubj = t.getSubject();
        final String tripleSubjUri = tripleSubj.getURI();
        final Node subject;
        if (tripleSubjUri.equals(resourceUri)) {
            subject = destinationNode;
        } else if (tripleSubjUri.startsWith(resourceUri)) {
            // If the subject begins with the originating resource uri, such as a hash uri, then rebase
            // the portions of the subject after the resource uri to the destination uri.
            final String suffix = tripleSubjUri.substring(resourceUri.length());
            subject = createURI(destinationNode.getURI() + suffix);
        } else {
            subject = t.getSubject();
        }
        return new Triple(subject, t.getPredicate(), t.getObject());
    }
}
