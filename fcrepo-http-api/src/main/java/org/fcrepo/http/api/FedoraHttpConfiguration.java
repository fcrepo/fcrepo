/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author cabeer
 * @since 10/17/14
 */
@Component
public class FedoraHttpConfiguration {

    @Value("${fcrepo.http.ldp.putRequiresIfMatch:false}")
    private boolean putRequiresIfMatch;

    /**
     * Should PUT requests require an If-Match header?
     * @return put request if match
     */
    public boolean putRequiresIfMatch() {
        return putRequiresIfMatch;
    }
}
