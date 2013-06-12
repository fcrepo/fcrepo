package org.fcrepo.api.repository;


import static javax.ws.rs.core.Response.created;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.annotation.Resource;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.fcrepo.AbstractResource;
import org.fcrepo.api.FedoraImport;
import org.fcrepo.exception.InvalidChecksumException;
import org.fcrepo.serialization.FedoraObjectSerializer;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Path("/fcr:import")
public class FedoraRepositoryImport extends FedoraImport {
}
