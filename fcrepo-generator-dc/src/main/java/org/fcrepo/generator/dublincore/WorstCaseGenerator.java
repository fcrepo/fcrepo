
package org.fcrepo.generator.dublincore;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.jcr.Node;

import org.slf4j.Logger;

public class WorstCaseGenerator implements DCGenerator {

    private static final Logger logger = getLogger(WorstCaseGenerator.class);

    @Override
    public InputStream getStream(final Node node) {
        final String str =
                "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ></oai_dc:dc>";

        return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
    }
}
