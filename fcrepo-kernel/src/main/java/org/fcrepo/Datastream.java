
package org.fcrepo;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.fcrepo.utils.FedoraJcrTypes.DC_TITLE;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.InputStream;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.modeshape.common.SystemFailureException;

import com.google.common.base.Function;

public class Datastream {

    Node node;

    public Datastream(Node n) {
        this.node = n;
    }

    public Node getNode() {
        return node;
    }

    public InputStream getContent() throws ValueFormatException,
            PathNotFoundException, RepositoryException {
        return node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getBinary()
                .getStream();
    }

    public String getMimeType() throws ValueFormatException,
            PathNotFoundException, RepositoryException {
        return node.hasProperty("fedora:contentType") ? node.getProperty(
                "fedora:contentType").getString() : "application/octet-stream";
    }

    public String getLabel() throws PathNotFoundException, RepositoryException {
        if (node.hasProperty(DC_TITLE)) {

            Property labels = node.getProperty(DC_TITLE);
            String label;
            if (!labels.isMultiple())
                label = node.getProperty(DC_TITLE).getString();
            else {
                label = on('/').join(map(labels.getValues(), value2string));
            }
            return label;
        } else
            return "";

    }

    public void setLabel(String label) throws ValueFormatException,
            VersionException, LockException, ConstraintViolationException,
            RepositoryException {
        node.setProperty(DC_TITLE, label);
        node.getSession().save();
    }

    private Function<Value, String> value2string =
            new Function<Value, String>() {

                @Override
                public String apply(Value v) {
                    try {
                        return v.getString();
                    } catch (RepositoryException e) {
                        throw new SystemFailureException(e);
                    } catch (IllegalStateException e) {
                        throw new SystemFailureException(e);
                    }
                }
            };

    private static <From, To> Collection<To> map(From[] input,
            Function<From, To> f) {
        return transform(copyOf(input), f);
    }
}
