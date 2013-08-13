/**
 * Copyright 2013 DuraSpace, Inc.
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

package org.fcrepo.storage.policy;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.tika.io.IOUtils;
import org.junit.Test;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;

// Runs integration test for restful interface for storage policies
// TODO set up and tear down for tests
public class PolicyGeneratorIT extends AbstractResourceIT {

   
    @Test
    public void testPolicyCreateByPost() throws Exception {
        final HttpPost objMethod = HttpPostObjMethod();
        StringEntity input = new StringEntity("mix:mimeType image/tiff tiff/store", "UTF-8");
        input.setContentType(APPLICATION_FORM_URLENCODED);
        objMethod.setEntity(input);
        final HttpResponse response = client.execute(objMethod);
        assertEquals(response.getStatusLine().getStatusCode(), 200);     
    }
    
    @Test
    public void testInvalidPoliciesCreateByPost() throws Exception {
        final HttpPost objMethod = HttpPostObjMethod();
        StringEntity input = new StringEntity("mix:newType image/tiff tiff/store", "UTF-8");
        input.setContentType(APPLICATION_FORM_URLENCODED);
        objMethod.setEntity(input);
        final HttpResponse response = client.execute(objMethod);
        assertEquals(response.getStatusLine().getStatusCode(), 500);     
    }
    
    @Test
    public void testPoliyDelete() throws Exception {
        testPolicyCreateByPost();
        final HttpDelete objMethod = HttpDeleteObjMethod("mix:mimeType");
        final HttpResponse response = client.execute(objMethod);
        assertEquals(response.getStatusLine().getStatusCode(), 200);    
        final HttpGet objGetMethod = HttpGetObjMethod(null);
        //assertEquals(200, getStatus(objMethod));
        final HttpResponse getResponse =
            client.execute(objGetMethod);
        assertEquals(IOUtils.toString(getResponse.getEntity().getContent()),
            "No Policies Found");
    }   
   
}
