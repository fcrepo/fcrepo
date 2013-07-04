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
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.slf4j.Logger;

/**
 * Derive a Dublin Core document from the JCR properties within the DC
 * namespace.
 */
public class JcrPropertiesGenerator implements DCGenerator {

    private static final Logger LOGGER =
            getLogger(JcrPropertiesGenerator.class);

    public static final String[] SALIENT_DC_PROPERTY_NAMESPACES =
            new String[] {"dc:*"};

    @Override
    public InputStream getStream(final Node node) {

        final StringBuilder str = new StringBuilder();

        str.append("<oai_dc:dc "
                + "xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" "
                + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");

        try {
            final PropertyIterator iter =
                    node.getProperties(SALIENT_DC_PROPERTY_NAMESPACES);

            while (iter.hasNext()) {
                final Property property = iter.nextProperty();
                if (property.isMultiple()) {
                    for (final Value v : property.getValues()) {
                        str.append("\t<" + property.getName() + ">" +
                                v.getString() + "</" + property.getName() +
                                ">\n");
                    }
                } else {
                    str.append("\t<" + property.getName() + ">" +
                            property.getValue().getString() + "</" +
                            property.getName() + ">\n");

                }
            }

            str.append("</oai_dc:dc>");

            return new ByteArrayInputStream(str.toString().getBytes(UTF_8));
        } catch (final RepositoryException e) {
            LOGGER.error("Repository exception: {}", e);
            return null;
        }
    }
}
