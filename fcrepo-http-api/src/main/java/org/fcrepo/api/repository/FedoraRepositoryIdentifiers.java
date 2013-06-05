package org.fcrepo.api.repository;

import javax.ws.rs.Path;

import org.fcrepo.api.FedoraIdentifiers;
import org.springframework.stereotype.Component;

@Component
@Path("/fcr:pid")
public class FedoraRepositoryIdentifiers extends FedoraIdentifiers {
}
