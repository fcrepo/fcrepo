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
package org.fcrepo.http.commons;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContextEvent;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class wraps the standard Spring ContextLoaderListener in order to catch initialization errors.
 * 
 * @author awoods
 * @since 2016-06-09
 */
public class FedoraContextLoaderListener extends ContextLoaderListener {

    private static final org.slf4j.Logger LOGGER = getLogger(FedoraContextLoaderListener.class);

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        try {
            super.contextInitialized(event);
        } catch (final BeanDefinitionStoreException e) {
            final String msg = "\n" +
                    "=====================================================================\n" +
                    "=====================================================================\n" +
                    "---------- FEDORA CONFIGURATION ERROR ----------\n" +
                    "\n" +
                    "See documentation specific to your version of Fedora\n" +
                    "https://wiki.duraspace.org/display/FEDORA6x/Application+Configuration\n" +
                    "\n" +
                    "=====================================================================\n" +
                    "=====================================================================\n";
            LOGGER.error(msg);
        }

    }

}
