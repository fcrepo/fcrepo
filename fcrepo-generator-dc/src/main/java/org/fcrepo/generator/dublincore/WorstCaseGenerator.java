package org.fcrepo.generator.dublincore;

import javax.jcr.Node;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class WorstCaseGenerator implements DCGenerator {

    @Override
    public InputStream getStream(Node node) {
        String str = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" ></oai_dc:dc>";

        try {
            return new ByteArrayInputStream(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
