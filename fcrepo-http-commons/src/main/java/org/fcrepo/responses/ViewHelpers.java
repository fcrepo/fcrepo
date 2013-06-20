package org.fcrepo.responses;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;
import org.fcrepo.RdfLexicon;
import org.slf4j.Logger;

import javax.ws.rs.core.UriInfo;
import java.util.Iterator;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class ViewHelpers {

    private final Logger LOGGER = getLogger(ViewHelpers.class);

    private static ViewHelpers instance = null;
    protected ViewHelpers() {
        // Exists only to defeat instantiation.
    }
    public static ViewHelpers getInstance() {
        if(instance == null) {
            instance = new ViewHelpers();
        }
        return instance;
    }

    public Iterator<Quad> getObjects(final DatasetGraph dataset, final Node subject, final Resource predicate) {
        return dataset.find(Node.ANY, subject, predicate.asNode(), Node.ANY);
    }

    public String getObjectTitle(final DatasetGraph dataset, final Node subject) {
        final Iterator<Quad> objects = getObjects(dataset, subject, RdfLexicon.DC_TITLE);

        if (objects.hasNext()) {
            return objects.next().getObject().getLiteralValue().toString();
        } else {
            return subject.getURI();
        }
    }

    public String getObjectsAsString(final DatasetGraph dataset, final Node subject, final Resource predicate) {
        final Iterator<Quad> iterator = getObjects(dataset, subject, predicate);


        if (iterator.hasNext()) {
        final Node object = iterator.next().getObject();

        if (object.isLiteral()) {
            final String s = object.getLiteralValue().toString();

            if (s.isEmpty()) {
                return "<empty>";
            } else {
                return s;
            }
        } else {
            return "&lt;<a href=\"" + object.getURI() + "\">" + object.getURI() + "</a>&gt;";
        }
        } else {
            return "";
        }
    }

   public Map<String, String> getNodeBreadcrumbs(final UriInfo uriInfo, final Node subject) {
       final String topic = subject.getURI();

       LOGGER.trace("Generating breadcrumbs for subject {}", subject);
       final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();


       final String baseUri = uriInfo.getBaseUri().toString();

       if ( !topic.startsWith(baseUri)) {
           LOGGER.trace("Topic wasn't part of our base URI {}", baseUri);
             return builder.build();
       }

       final String salientPath = topic.substring(baseUri.length());


       final String[] split = salientPath.split("/");

       StringBuilder cumulativePath = new StringBuilder();

       for ( final String path : split) {

           if (path.isEmpty()) {
               continue;
           }

           cumulativePath.append(path);

           final String uri = uriInfo.getBaseUriBuilder().path(cumulativePath.toString()).build().toString();

           LOGGER.trace("Adding breadcrumb for path segment {} => {}", path, uri);


           builder.put(path, uri);

           cumulativePath.append("/");

       }

       return builder.build();

   }

    public String getNamespacePrefix(final Model model, final String namespace) {
        final String nsURIPrefix = model.getNsURIPrefix(namespace);

        if (nsURIPrefix == null) {
            return namespace;
        } else {
            return nsURIPrefix + ":";
        }
    }

}
