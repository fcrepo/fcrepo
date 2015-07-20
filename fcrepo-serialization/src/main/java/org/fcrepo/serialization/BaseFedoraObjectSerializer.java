/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Auto-wire some helpful services for the FedoraObjectSerializer
 *
 * @author cbeer
 */
public abstract class BaseFedoraObjectSerializer implements
        FedoraObjectSerializer {

    @Autowired
    protected Repository repo;

    @Override
    public abstract void serialize(final FedoraResource obj,
                                   final OutputStream out,
                                   final boolean skipBinary,
                                   final boolean recurse) throws RepositoryException, IOException,
            InvalidSerializationFormatException;

    @Override
    public abstract void deserialize(final Session session, final String path,
            final InputStream stream) throws IOException, RepositoryException,
            InvalidChecksumException, InvalidSerializationFormatException;

}
