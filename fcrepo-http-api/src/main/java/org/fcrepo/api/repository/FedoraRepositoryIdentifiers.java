package org.fcrepo.api.repository;

import org.fcrepo.api.FedoraIdentifiers;
import org.springframework.stereotype.Component;

import javax.ws.rs.Path;

@Component
@Path("/rest/fcr:pid")
public class FedoraRepositoryIdentifiers extends FedoraIdentifiers {
}
