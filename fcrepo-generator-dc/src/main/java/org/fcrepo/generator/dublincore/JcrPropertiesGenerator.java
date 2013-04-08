package org.fcrepo.generator.dublincore;

import org.slf4j.Logger;

import javax.jcr.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.slf4j.LoggerFactory.getLogger;

public class JcrPropertiesGenerator implements DCGenerator {

    private static final Logger logger = getLogger(JcrPropertiesGenerator.class);
    public static final String[] SALIENT_DC_PROPERTY_NAMESPACES = new String[] { "dc:*" };

    @Override
    public InputStream getStream(Node node) {

        StringBuilder str = new StringBuilder();

        str.append("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");

        try {
            PropertyIterator iter = node.getProperties(SALIENT_DC_PROPERTY_NAMESPACES);

            while(iter.hasNext()) {
                Property property = iter.nextProperty();
                if(property.isMultiple()) {
                    for(final Value v : property.getValues()) {
                        str.append("\t<" + property.getName() + ">" + v.getString() + "</" + property.getName() + ">\n");
                    }
                }
                else {
                    str.append("\t<" + property.getName() + ">" + property.getValue().getString() + "</" + property.getName() + ">\n");

                }
            }

            str.append("</oai_dc:dc>");

            return new ByteArrayInputStream(str.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Exception rendering properties: {}", e);
            return null;
        } catch (RepositoryException e) {
            logger.error("Repository exception: {}", e);
            return null;
        }
    }
}
