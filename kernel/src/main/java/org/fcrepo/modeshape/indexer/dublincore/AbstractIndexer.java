package org.fcrepo.modeshape.indexer.dublincore;


import java.io.InputStream;

import javax.jcr.Node;

public abstract class AbstractIndexer {

    public AbstractIndexer() {
    }

    public abstract InputStream getStream(Node node);

}
