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
package org.fcrepo.http.api;


import org.fcrepo.http.api.responses.StreamingBaseHtmlProvider;
import org.springframework.context.annotation.Scope;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static org.fcrepo.http.commons.domain.ServerHeader.SERVER;
import static org.fcrepo.http.commons.domain.ServerHeader.serverVersion;
import static javax.ws.rs.core.Response.ok;

/**
 * @author md5wz
 * @since 11/12/2014
 */
@Scope("request")
@Path("/fcr:assets")
public class ViewAssets {

    /**
     * Gets the common css file referenced in all velocity views.
     * @return the common css file referenced in all velocity views
     */
    @GET
    @Path("common.css")
    @Produces({"text/css; charset=utf-8", "*/*"})
    public Response getViewCss() {
        return ok().header(SERVER, serverVersion())
            .entity(this.getClass().getResourceAsStream(StreamingBaseHtmlProvider.commonCssLocation)).build();
    }

    /**
     * Gets the common js file referenced in all velocity views.
     * @return the response
     */
    @GET
    @Path("common.js")
    @Produces({"text/javascript; charset=utf-8", "*/*"})
    public Response getViewJs() {
        return ok().header(SERVER, serverVersion())
            .entity(this.getClass().getResourceAsStream(StreamingBaseHtmlProvider.commonJsLocation)).build();
    }
}
