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
package org.fcrepo.kernel.impl.models;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.fcrepo.kernel.api.Transaction;
import org.fcrepo.kernel.api.exception.InvalidChecksumException;
import org.fcrepo.kernel.api.models.Binary;
import org.fcrepo.kernel.api.models.ResourceFactory;
import org.fcrepo.kernel.api.services.policy.StoragePolicyDecisionPoint;
import org.fcrepo.persistence.api.PersistentStorageSessionManager;


/**
 * Implementation of a Non-RDF resource.
 *
 * @author bbpennel
 */
public class BinaryImpl extends FedoraResourceImpl implements Binary {

    /**
     * Construct the binary
     *
     * @param id
     * @param tx
     * @param pSessionManager
     * @param resourceFactory
     */
    public BinaryImpl(final String id, final Transaction tx, final PersistentStorageSessionManager pSessionManager,
            final ResourceFactory resourceFactory) {
        super(id, tx, pSessionManager, resourceFactory);
    }

    @Override
    public InputStream getContent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setContentStream(final InputStream content) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setContent(final InputStream content, final String contentType, final Collection<URI> checksums,
            final String originalFileName, final StoragePolicyDecisionPoint storagePolicyDecisionPoint)
            throws InvalidChecksumException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setExternalContent(final String contentType, final Collection<URI> checksums, final String originalFileName,
            final String externalHandling, final String externalUrl) throws InvalidChecksumException {
        // TODO Auto-generated method stub

    }

    @Override
    public long getContentSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public URI getContentDigest() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean isProxy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean isRedirect() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProxyURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setProxyURL(final String url) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getRedirectURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRedirectURL(final String url) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getMimeType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFilename() {
        // TODO Auto-generated method stub
        return null;
    }

}
