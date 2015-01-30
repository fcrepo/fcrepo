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
package org.fcrepo.integration;

import org.fcrepo.transform.http.responses.ResultSetStreamingOutput;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

import javax.ws.rs.ApplicationPath;

/**
 * @author osmandin
 */
@ApplicationPath("/")
public class TestResourceConfig extends ResourceConfig {

    public TestResourceConfig() {
        register(RequestContextFilter.class);
        register(ResultSetStreamingOutput.class);
        register(ResultSetStreamingOutputIT.TestHttpResource.class);
    }
}