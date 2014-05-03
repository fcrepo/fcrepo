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

package org.fcrepo.generator.dublincore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.jcr.Node;

import org.slf4j.Logger;

/**
 * If all other DC generators fail, just publish an empty XML document
 *
 * @author cbeer
 */
public class WorstCaseGenerator implements DCGenerator {

    private static final Logger LOGGER = getLogger(WorstCaseGenerator.class);

    @Override
    public InputStream getStream(final Node node) {
        LOGGER.debug("Writing an empty oai dc document");
        final String str =
                "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" />";

        return new ByteArrayInputStream(str.getBytes(UTF_8));
    }
}
