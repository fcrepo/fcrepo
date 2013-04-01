package org.fcrepo.generator.dublincore;

import org.slf4j.Logger;

import javax.jcr.Node;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.slf4j.LoggerFactory.getLogger;

public class WorstCaseGenerator implements DCGenerator {

    private static final Logger logger = getLogger(WorstCaseGenerator.class);

    @Override
    public InputStream getStream(Node node) {
        String str = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ></oai_dc:dc>";

        try {
            return new ByteArrayInputStream(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.warn("logged exception", e);
            return null;
        }
    }
}
