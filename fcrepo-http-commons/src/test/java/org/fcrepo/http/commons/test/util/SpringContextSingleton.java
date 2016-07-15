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
package org.fcrepo.http.commons.test.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


/**
 * Retrieves the application context in which it is configured, for testing
 * purposes only.
 *
 * @since September 19, 2014
 * @author Gregory Jansen
 */
public class SpringContextSingleton implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /* (non-Javadoc)
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(
     * org.springframework.context.ApplicationContext)
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext)
            throws BeansException {
        SpringContextSingleton.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
