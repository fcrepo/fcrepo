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
package org.fcrepo.serialization;

import org.apache.commons.io.IOUtils;
import org.fcrepo.kernel.models.FedoraResource;
import org.springframework.stereotype.Component;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serialize a FedoraObject using the modeshape-provided JCR/XML format
 *
 * @author cbeer
 */
@Component
public class JcrXmlSerializer extends BaseFedoraObjectSerializer {

    @Override
    public String getKey() {
        return JCR_XML;
    }

    @Override
    public String getMediaType() {
        return "application/xml";
    }

    @Override
    /**
     * Serialize JCR/XML with options for recurse and skipBinary.
     * @param obj
     * @param out
     * @param skipBinary
     * @param recurse
     * @throws RepositoryException
     * @throws IOException
     */
    public void serialize(final FedoraResource obj,
                          final OutputStream out,
                          final boolean skipBinary,
                          final boolean recurse)
            throws RepositoryException, IOException {
        final Node node = obj.getNode();
        // jcr/xml export system view implemented for noRecurse:
        // exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
        node.getSession().exportSystemView(obj.getPath(), out, skipBinary, !recurse);
    }

    @Override
    public void deserialize(final Session session, final String path,
            final InputStream stream) throws RepositoryException, IOException, InvalidSerializationFormatException {

        final File temp = File.createTempFile("fcrepo-unsanitized-input", ".xml");
        final FileOutputStream fos = new FileOutputStream(temp);
        try {
            IOUtils.copy(stream, fos);
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(fos);
        }
        validateJCRXML(temp);
        try (final InputStream tmpInputStream = new TempFileInputStream(temp)) {
            session.importXML(path, tmpInputStream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            // These come from ModeShape when there's various problems in the formatting of the XML
            // that are not caught by JCRXMLValidatingInputStreamBridge.
            throw new InvalidSerializationFormatException("Invalid JCR/XML."
                    + (e.getMessage() != null ? " (" + e.getMessage() + ")" : ""));
        }
    }

    private void validateJCRXML(final File file) throws InvalidSerializationFormatException, IOException {
        try (final FileInputStream fis = new FileInputStream(file)) {
            final XMLEventReader reader = XMLInputFactory.newFactory().createXMLEventReader(fis);
            while (reader.hasNext()) {
                final XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) {
                    final StartElement startElement = event.asStartElement();
                    final QName name = startElement.getName();
                    if (!(name.getNamespaceURI().equals("http://www.jcp.org/jcr/sv/1.0")
                            && (name.getLocalPart().equals("node") || name.getLocalPart().equals("property")
                            || name.getLocalPart().equals("value")))) {
                        throw new InvalidSerializationFormatException(
                                "Unrecognized element \"" + name.toString() + "\", in import XML.");
                    }
                }
            }
            reader.close();
        } catch (XMLStreamException e) {
            throw new InvalidSerializationFormatException("Unable to parse XML"
                    + (e.getMessage() != null ? " (" + e.getMessage() + ")." : "."));
        }
    }

    /**
     * A FileInputStream that deletes the file when closed.
     */
    private static final class TempFileInputStream extends FileInputStream {

        private File f;

        /**
         * A constructor whose passed file's content is exposed by this
         * TempFileInputStream, and which will be deleted when this
         * InputStream is closed.
         * @param f
         * @throws FileNotFoundException
         */
        public TempFileInputStream(final File f) throws FileNotFoundException {
            super(f);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (f != null) {
                    f.delete();
                    f = null;
                }
            }
        }
    }
}
