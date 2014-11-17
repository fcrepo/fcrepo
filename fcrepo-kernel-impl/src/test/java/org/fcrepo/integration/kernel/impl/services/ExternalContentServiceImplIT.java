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
package org.fcrepo.integration.kernel.impl.services;

import org.apache.commons.io.IOUtils;
import org.fcrepo.integration.kernel.impl.identifiers.ContainerWrapper;
import org.fcrepo.kernel.services.ExternalContentService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.InputStream;
import java.net.URI;

import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertEquals;

/**
 * @author osmandin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-container.xml")
public class ExternalContentServiceImplIT {

    private static String PREFIX = "http://localhost:";

    private int getPort() {
        return parseInt(System.getProperty("test.port", "8080"));
    }

    @Inject
    private ContainerWrapper containerWrapper;

    private void addHandler(final String data, final String path) {
        containerWrapper.addHandler(data, path);
    }

    @Inject
    private ExternalContentService externalContentService;

    @Test
    public void shouldFetch() throws Exception {
        addHandler("test", "/exttest1");
        final URI url = new URI(PREFIX + getPort() + "/exttest1");
        final InputStream is = externalContentService.retrieveExternalContent(url);
        assertEquals(IOUtils.toString(is), "test");
    }

}
