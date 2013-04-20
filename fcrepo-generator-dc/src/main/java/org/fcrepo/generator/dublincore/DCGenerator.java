
package org.fcrepo.generator.dublincore;

import java.io.InputStream;

import javax.jcr.Node;

public interface DCGenerator {

    InputStream getStream(final Node node);

}
