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
package org.fcrepo.auth.roles.common.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Scott Prater
 */
public class RolesFadTestObjectBean {

    private String path;

    private final List<Map<String, String>> datastreams;

    private final List<Map<String, String>> acls;

    private final Map<String, List<Map<String, String>>> datastreamACLs;

    public RolesFadTestObjectBean() {
        this.datastreams = new ArrayList<>();
        this.acls = new ArrayList<>();
        this.datastreamACLs = new HashMap<>();
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public List<Map<String, String>> getDatastreams() {
        return this.datastreams;
    }

    public void addDatastream(final String dsid, final String content) {
        final Map<String, String> datastream = new HashMap<>();
        datastream.put(dsid, content);
        datastreams.add(datastream);
    }

    public List<Map<String, String>> getACLs() {
        return this.acls;
    }

    public void addACL(final String principal, final String role) {
        final Map<String, String> acl = new HashMap<>();
        acl.put(principal, role);
        acls.add(acl);
    }

    public List<Map<String, String>> getDatastreamACLs(final String dsid) {
        return this.datastreamACLs.get(dsid);
    }

    public void addDatastreamACL(final String dsid, final String principal,
            final String role) {
        List<Map<String, String>> acl_list;
        final Map<String, String> acl = new HashMap<>();
        acl.put(principal, role);
        if (this.datastreamACLs.get(dsid) == null) {
            acl_list = new ArrayList<>();
            this.datastreamACLs.put(dsid, acl_list);
        }
        this.datastreamACLs.get(dsid).add(acl);
    }
}
