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

import static java.util.UUID.randomUUID;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Test;

/**
 * <p>FedoraBatchIT class.</p>
 *
 * @author lsitu
 * @author cbeer
 */
public class FedoraRepositoryBatchIT extends AbstractResourceIT {

    @Test
    public void testRetrieveMultipartDatastreams() throws Exception {
        final String pid = "";
        final String dsid1 = randomUUID().toString();
        final String dsid2 = randomUUID().toString();

        final HttpPost post =
            new HttpPost(getServerPath(pid) + "/fcr:batch");
        final MultipartEntityBuilder multiPartEntityBuilder =
                MultipartEntityBuilder.create().addTextBody(dsid1, "asdfg", TEXT_PLAIN)
                        .addTextBody(dsid2, "qwerty", TEXT_PLAIN);

        post.setEntity(multiPartEntityBuilder.build());

        final HttpResponse postResponse = client.execute(post);
        assertEquals(201, postResponse.getStatusLine().getStatusCode());

        // TODO: we should actually evaluate the multipart response for the
        // things we're expecting
        final HttpGet getDSesMethod =
            new HttpGet(getServerPath(pid) + "/fcr:batch");
        final HttpResponse response = client.execute(getDSesMethod);
        assertEquals(200, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());

        assertTrue("Didn't find the first datastream!",
                compile("asdfg", DOTALL).matcher(content).find());
        assertTrue("Didn't find the second datastream!", compile("qwerty",
                DOTALL).matcher(content).find());

    }
}
