/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.services;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.inject.Inject;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.fcrepo.kernel.api.FedoraSession;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.BinaryService;
import org.fcrepo.kernel.api.services.ContainerService;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.api.services.VersionService;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_MEMENTO;
import static org.fcrepo.kernel.api.FedoraTypes.FEDORA_MEMENTO_DATETIME;
import static org.fcrepo.kernel.modeshape.FedoraResourceImpl.LDPCV_TIME_MAP;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This service exposes management of node versioning.  Instead of invoking
 * the JCR VersionManager methods, this provides a level of indirection that
 * allows for special handling of features built on top of JCR such as user
 * transactions.
 * @author Mike Durbin
 */

@Component
public class VersionServiceImpl extends AbstractService implements VersionService {

    private static final Logger LOGGER = getLogger(VersionService.class);

    private static final DateTimeFormatter MEMENTO_DATETIME_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("GMT"));

    /**
     * The repository object service
     */
    @Inject
    protected ContainerService containerService;

    /**
     * The bitstream service
     */
    @Inject
    protected BinaryService binaryService;

    @Inject
    protected NodeService nodeService;

    @Override
    public FedoraResource createVersion(final FedoraSession session, final FedoraResource resource) {
        return createVersion(session, resource, Instant.now(), true);
    }

    @Override
    public FedoraResource createVersion(final FedoraSession session, final FedoraResource resource,
            final Instant dateTime, final boolean fromExisting) {

        final String mementoPath = makeMementoPath(resource, dateTime);
        final Calendar mementoDatetime = GregorianCalendar.from(ZonedDateTime.ofInstant(dateTime, ZoneId.of("UTC")));

        if (exists(session, mementoPath)) {
            throw new RepositoryRuntimeException(new ItemExistsException(
                    "Memento " + mementoPath + " already exists for resource " + resource.getPath()));
        }

        if (fromExisting) {
            LOGGER.debug("Creating memento {} for resource {} using existing state", mementoPath, resource.getPath());
            nodeService.copyObject(session, resource.getPath(), mementoPath);
        } else {
            LOGGER.debug("Creating memento {} for resource {}", mementoPath, resource.getPath());
        }

        final FedoraResource mementoResource = getMementoResource(session, resource, mementoPath);

        try {
            final Node mementoNode = findNode(session, mementoPath);
            if (mementoNode.canAddMixin(FEDORA_MEMENTO)) {
                mementoNode.addMixin(FEDORA_MEMENTO);
            }
            mementoNode.setProperty(FEDORA_MEMENTO_DATETIME, mementoDatetime);
        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }

        return mementoResource;
    }

    private FedoraResource getMementoResource(final FedoraSession session, final FedoraResource resource,
            final String mementoPath) {
        if (resource instanceof FedoraBinary) {
            return binaryService.findOrCreate(session, mementoPath);
        } else {
            return containerService.findOrCreate(session, mementoPath);
        }
    }

    private String makeMementoPath(final FedoraResource resource, final Instant datetime) {
        return resource.getPath() + "/" + LDPCV_TIME_MAP + "/" + MEMENTO_DATETIME_ID_FORMATTER.format(datetime);
    }
}
