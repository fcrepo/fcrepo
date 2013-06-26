package org.fcrepo;

import javax.jcr.Session;

/**
 * Additional methods introduced by our transaction-aware session
 */
public interface TxSession extends Session {
    /**
     * @return the transaction identifier associated with this session
     */
    String getTxId();
}
