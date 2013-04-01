package org.fcrepo.generator.dublincore;

import org.slf4j.Logger;

import javax.jcr.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.slf4j.LoggerFactory.getLogger;

public class JcrPropertiesGenerator implements DCGenerator {

    private static final Logger logger = getLogger(JcrPropertiesGenerator.class);

    @Override
    public InputStream getStream(Node node) {


        String str = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">";

        try {
            final String[] nameGlobs = new String[] { "dc:*"};
            PropertyIterator iter = node.getProperties(nameGlobs);

            while(iter.hasNext()) {
                Property property = iter.nextProperty();
                if(property.isMultiple()) {
                    for(final Value v : property.getValues()) {
                        str += "\t<" + property.getName() + ">" + v.getString() + "</" + property.getName() + ">\n";
                    }
                }
                else {
                    str += "\t<" + property.getName() + ">" + property.getValue().getString() + "</" + property.getName() + ">\n";

                }
            }

            str += "</oai_dc:dc>";

            return new ByteArrayInputStream(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Exception rendering properties: {}", e);
            return null;
        } catch (RepositoryException e) {
            logger.error("Repository exception: {}", e);
            return null;
        }
    }
}
