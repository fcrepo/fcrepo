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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.fcrepo.kernel.models.FedoraResource;
import org.springframework.stereotype.Component;

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

        try {
            session.importXML(path, new JCRXMLValidatingInputStreamBridge(stream),
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        } catch (IOException e) {
            if (e.getCause() != null && (e.getCause() instanceof InvalidSerializationFormatException)) {
                throw (InvalidSerializationFormatException) e.getCause();
            } else {
                throw e;
            }
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            // These come from ModeShape when there's various problems in the formatting of the XML
            // that are not caught by JCRXMLValidatingInputStreamBridge.
            throw new InvalidSerializationFormatException("Invalid JCR/XML."
                    + (e.getMessage() != null ? " (" + e.getMessage() + ")" : ""));
        }

    }

    /**
     * An InputStream that wraps another input stream reading XML events off
     * of the stream and loosely validating whether it's a JCR/XML XML stream.
     *
     * This InputStream maintains a buffer of variable size that is never
     * bigger than the serialization of a single XMLEvent.
     * source InputStream --> XMLEventReader --> custom validation --> XMLEventWriter --> buffer --> InputStream
     */
    static class JCRXMLValidatingInputStreamBridge extends InputStream {

        private XMLReaderBridge buffer;

        JCRXMLValidatingInputStreamBridge(final InputStream wrappedInputStream) throws IOException {
            buffer = new XMLReaderBridge(wrappedInputStream);
        }

        @Override
        public int available() {
            return buffer.size();
        }

        @Override
        public void close() throws IOException {
                buffer.closeInputStream();
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        public int read(final byte[] b) throws IOException {
            return buffer.read(b, 0, b.length);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            return buffer.read(b, off, len);
        }

        @Override
        public void reset() throws IOException {
            throw new IOException();
        }

        @Override
        public long skip(final long n) throws IOException {
            final byte[] b = new byte[1024];
            for (long i = 0; i < n ; ) {
                final int bytesRead = read(b, 0, (int) Math.min((long) b.length, n - i));
                if (bytesRead == 0) {
                    return i;
                }
                i += bytesRead;
            }
            return n;
        }

        @Override
        public int read() throws IOException {
            final byte[] b = new byte[1];
            if (0 == buffer.read(b, 0, 1)) {
                return -1;
            } else {
                return b[0];
            }
        }

        /**
         * A class that represents the buffer (or bridge) between the InputStream passed
         * and the InputStream implementation that wraps it.
         */
        private static class XMLReaderBridge extends OutputStream {

            private byte[] bytebuffer;
            int size;
            int offset;

            private XMLEventReader reader;

            private XMLEventWriter writer;

            public XMLReaderBridge(final InputStream xmlInputStream) throws IOException {
                final XMLInputFactory factory = XMLInputFactory.newFactory();
                try {
                    reader = factory.createXMLEventReader(xmlInputStream);
                } catch (XMLStreamException e) {
                    throw new IOException(
                            new InvalidSerializationFormatException(
                                    e.getMessage() == null ? "Invalid XML." : e.getMessage()));
                }

                bytebuffer = new byte[1024 * 16];
                size = 0;
                try {
                    writer = XMLOutputFactory.newFactory().createXMLEventWriter(this);
                } catch (XMLStreamException e) {
                    throw new IOException(
                            new InvalidSerializationFormatException(
                                    e.getMessage() == null ? "Invalid XML." : e.getMessage()));
                }
            }

            /**
             * Gets the number of bytes currently buffered.
             */
            public int size() {
                return size;
            }

            /**
             * Reads at least one byte and copies them to the specified
             * position in the passed byte array (removing them from this
             * buffer).
             */
            public int read(final byte[] b, final int off, final int len) throws IOException {
                try {
                    fillBuffer(1);
                } catch (InvalidSerializationFormatException e) {
                    throw new IOException(e);
                }
                final int bytesToRead = Math.min(len, size());
                if (bytesToRead == 0) {
                    // EOF
                    return -1;
                }
                if (bytesToRead + offset > bytebuffer.length) {
                    final int sizeBeforeWrap = bytebuffer.length - offset;
                    final int sizeAfterWrap = bytesToRead - sizeBeforeWrap;
                    System.arraycopy(bytebuffer, offset, b, off, sizeBeforeWrap);
                    System.arraycopy(bytebuffer, 0, b, off + sizeBeforeWrap, (bytesToRead - sizeBeforeWrap));
                    offset = sizeAfterWrap;
                } else {
                    System.arraycopy(bytebuffer, offset, b, off, bytesToRead);
                    offset = offset + bytesToRead;
                }
                size -= bytesToRead;
                return bytesToRead;
            }

            /**
             * Reads enough of the underlying input stream to ensure that at
             * least 'length' bytes are available between the leftovers and
             * fresh buffer.
             */
            private void fillBuffer(final int length) throws IOException, InvalidSerializationFormatException {
                try {
                    while (size() < length && reader.hasNext()) {
                        final XMLEvent event = reader.nextEvent();
                        validateEvent(event);
                        writer.add(event);
                        writer.flush();
                    }
                } catch (XMLStreamException e) {
                    throw new IOException(
                            new InvalidSerializationFormatException(
                                    e.getMessage() == null ? "Invalid XML." : e.getMessage()));
                }
                if (!reader.hasNext()) {
                    try {
                        reader.close();
                        writer.close();
                    } catch (XMLStreamException e) {
                        throw new IOException(
                                new InvalidSerializationFormatException(
                                        e.getMessage() == null ? "Invalid XML." : e.getMessage()));
                    }
                }
            }

            public void closeInputStream() throws IOException {
                try {
                    writer.close();
                    reader.close();
                } catch (XMLStreamException e) {
                    throw new IOException(
                            new InvalidSerializationFormatException(
                                    e.getMessage() == null ? "Invalid XML." : e.getMessage()));
                }
            }

            @Override
            public void write(final int b) throws IOException {
                if (size == bytebuffer.length) {
                    final byte[] newbuffer = new byte[bytebuffer.length * 2];
                    System.arraycopy(bytebuffer, offset, newbuffer, 0, bytebuffer.length - offset);
                    System.arraycopy(bytebuffer, 0, newbuffer, bytebuffer.length - offset, offset);
                    bytebuffer = newbuffer;
                }
                bytebuffer[offset + size % bytebuffer.length] = (byte) b;
                size ++;
            }

            /**
             * Determine if the event is reasonable within a JCRXML file.
             * This isn't a terribly sophisticated validation, it just checks for
             * certain node types.
             */
            private void validateEvent(final XMLEvent event) throws InvalidSerializationFormatException {
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

        }
    }

}
