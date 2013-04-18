
package org.fcrepo.generator.rdf;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

public interface TripleSource<T> {

    /**
     * Creates Lists of simple triples from some resource.
     * The optional UriInfo service is to be used if this
     * TripleSource is able to return resolvable URIs
     * 
     * @param source The resource to be triplified
     * @param uriInfo Optional UriInfo service
     * @return A List of Triples.
     * @throws RepositoryException
     */
    List<Triple> getTriples(final T source, final UriInfo... uriInfo)
            throws RepositoryException;

    /**
     * Ultra-simple triple abstraction.
     * 
     * @author ajs6f
     *
     */
    public static class Triple {

        public Triple(final String s, final String p, final String o) {
            subject = s;
            predicate = p;
            object = o;

        }

        public String subject;

        public String predicate;

        public String object;

        @Override
        public String toString() {
            return "[ " + subject + " " + predicate + " " + object + ". ]";
        }
    }

}
