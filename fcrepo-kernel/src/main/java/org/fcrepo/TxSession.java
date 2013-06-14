package org.fcrepo;

import javax.jcr.Session;

public interface TxSession extends Session {
    String getTxId();
}
