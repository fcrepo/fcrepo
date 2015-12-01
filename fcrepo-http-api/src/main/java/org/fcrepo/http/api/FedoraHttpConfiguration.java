/*
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
package org.fcrepo.http.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author cabeer
 * @since 10/17/14
 */
@Component
public class FedoraHttpConfiguration {

    @Value("${fcrepo.http.ldp.putRequiresIfMatch:false}")
    private boolean putRequiresIfMatch;

    /**
     * Should PUT requests require an If-Match header?
     * @return put request if match
     */
    public boolean putRequiresIfMatch() {
        return putRequiresIfMatch;
    }
}
