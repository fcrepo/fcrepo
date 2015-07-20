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
package org.fcrepo.kernel.modeshape.services;

import org.fcrepo.kernel.api.services.ExternalContentService;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * @author cabeer
 */
@Component
public class ExternalContentServiceImpl implements ExternalContentService {

    @Inject
    private HttpClientConnectionManager connManager;

    /**
     * Retrieve the content at the URI using the global connection pool.
     * @param sourceUri the source uri
     * @return the content at the URI using the global connection pool
     * @throws IOException if IO exception occurred
     */
    @Override
    public InputStream retrieveExternalContent(final URI sourceUri) throws IOException {
        final HttpGet httpGet = new HttpGet(sourceUri);
        final CloseableHttpClient client = getCloseableHttpClient();
        final HttpResponse response = client.execute(httpGet);
        return response.getEntity().getContent();
    }

    @VisibleForTesting
    protected CloseableHttpClient getCloseableHttpClient() {
        return HttpClients.createMinimal(connManager);
    }

    @VisibleForTesting
    protected void setConnManager(final HttpClientConnectionManager connManager) {
        this.connManager = connManager;
    }
}
