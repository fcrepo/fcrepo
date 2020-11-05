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

package org.fcrepo.config;

import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * A base class for property configs
 *
 * @author dbernstein
 * @since 6.0.0
 */
@PropertySources({
        @PropertySource(value = FedoraPropsConfig.DEFAULT_FCREPO_CONFIG_FILE_DEFAULT_PROP_SOURCE,
                ignoreResourceNotFound = true),
        @PropertySource(value = FedoraPropsConfig.FCREPO_CONFIG_FILE_PROP_SOURCE, ignoreResourceNotFound = true)
})
abstract class BasePropsConfig {

    public static final String DEFAULT_FCREPO_CONFIG_FILE_DEFAULT_PROP_SOURCE =
            "file:fcrepo-home/config/fcrepo.properties";
    public static final String FCREPO_CONFIG_FILE_PROP_SOURCE = "file:${" + FedoraPropsConfig.FCREPO_CONFIG_FILE + "}";

}
