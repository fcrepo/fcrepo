
package org.fcrepo.generator.dublincore;

import java.io.InputStream;

import javax.jcr.Node;

public interface DCGenerator {

    public abstract InputStream getStream(final Node node);

}
