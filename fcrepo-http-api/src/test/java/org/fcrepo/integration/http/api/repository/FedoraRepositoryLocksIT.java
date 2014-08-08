/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration.http.api.repository;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.fcrepo.jcr.FedoraJcrTypes;
import org.junit.Test;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.junit.Assert.assertEquals;

/**
 * @author lsitu
 */
public class FedoraRepositoryLocksIT extends AbstractResourceIT implements FedoraJcrTypes {

    @Test
    public void testLockAccess() throws IOException {
        HttpResponse response = lockRoot();
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());
        response = unlockRoot();
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    /**
     * Attempts to lock the root.
     */
    private HttpResponse lockRoot() throws IOException {
        final HttpPost post = new HttpPost(getServerPath ("") + "/" + FCR_LOCK);
        return client.execute(post);
    }

    /**
     * Attempts to unlock the locked root.
     */
    private HttpResponse unlockRoot() throws IOException {
        final HttpDelete delete = new HttpDelete(getServerPath ("") + "/" + FCR_LOCK);
        return client.execute(delete);
    }
}

