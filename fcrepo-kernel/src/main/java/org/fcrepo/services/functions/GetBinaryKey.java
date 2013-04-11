
package org.fcrepo.services.functions;

import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

import com.google.common.base.Function;

public class GetBinaryKey implements Function<Node, BinaryKey> {

    @Override
    public BinaryKey apply(Node input) {
        try {
            return ((BinaryValue) input.getNode(JCR_CONTENT).getProperty(
                    JCR_DATA).getBinary()).getKey();
        } catch (RepositoryException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
