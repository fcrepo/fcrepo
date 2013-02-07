package org.fcrepo.modeshape.indexer.dublincore;

import javax.jcr.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class IndexFromJcrProperties extends AbstractIndexer {

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

            try {
                return new ByteArrayInputStream(str.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
            return null;
        }
    }
}
