
package org.fcrepo.api.repository;

import javax.jcr.Session;
import javax.ws.rs.Path;
import org.fcrepo.api.FedoraImport;
import org.fcrepo.session.InjectedSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This stub is a hack to mount the functionality of FedoraImport at the root of
 * this webapp. Without it, the globbing from FedoraNodes would own this path
 * instead.
 */
@Component
@Scope("prototype")
@Path("/fcr:export")
public class FedoraRepositoryExport extends FedoraImport {

    @InjectedSession
    protected Session session;

}
