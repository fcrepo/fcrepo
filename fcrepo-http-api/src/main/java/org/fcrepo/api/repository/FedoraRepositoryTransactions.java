
package org.fcrepo.api.repository;

import org.fcrepo.api.FedoraTransactions;
import org.fcrepo.session.InjectedSession;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.jcr.Session;
import javax.ws.rs.Path;

@Component
@Scope("prototype")
@Path("/fcr:tx")
public class FedoraRepositoryTransactions extends FedoraTransactions {

    @InjectedSession
    protected Session session;
}
