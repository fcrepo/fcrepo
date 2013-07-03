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
package org.fcrepo.auth.oauth.integration.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.fcrepo.webxml.WebAppConfig;
import org.fcrepo.webxml.bind.ContextParam;
import org.fcrepo.webxml.bind.FilterMapping;
import org.fcrepo.webxml.bind.Listener;
import org.fcrepo.webxml.bind.ServletMapping;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBinding {

    private final Logger LOGGER = LoggerFactory.getLogger(TestBinding.class);

    @Test
    public void testBinding() throws JAXBException {
        LOGGER.trace("Executing testBinding()");
        final JAXBContext context = JAXBContext.newInstance(WebAppConfig.class);
        final Unmarshaller u = context.createUnmarshaller();
        final WebAppConfig o =
                (WebAppConfig) u.unmarshal(getClass().getResourceAsStream(
                        "/web.xml"));
        assertEquals("Fedora-on-ModeShape", o.displayName());
        assertTrue(o.contextParams().contains(
                new ContextParam("contextConfigLocation",
                        "classpath:spring-test/rest.xml; "
                                + "classpath:spring-test/repo.xml; "
                                + "classpath:spring-test/security.xml")));
        assertTrue(o
                .listeners()
                .contains(
                        new Listener(null,
                                "org.springframework.web.context.ContextLoaderListener")));
        final ServletMapping sm =
                o.servletMappings("jersey-servlet").iterator().next();
        assertNotNull(sm);
        assertEquals("/*", sm.urlPattern());

        FilterMapping fm = o.filterMappings("TokenFilter").iterator().next();
        assertNotNull(fm);
        assertEquals("/token", fm.urlPattern());

        fm = o.filterMappings("OpFilter").iterator().next();
        assertNotNull(fm);
        assertEquals("/rest/objects/authenticated/*", fm.urlPattern());

    }
}
