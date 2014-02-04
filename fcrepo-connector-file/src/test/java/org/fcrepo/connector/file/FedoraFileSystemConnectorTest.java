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
package org.fcrepo.connector.file;

import org.junit.AfterClass;
import org.junit.Assert;
import org.fcrepo.http.commons.test.util.TestHelpers;
import org.infinispan.schematic.document.Document;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.federation.spi.ExtraPropertiesStore;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.basic.BasicName;

import javax.jcr.NamespaceRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

/**
 * @author Andrew Woods
 *         Date: 2/3/14
 */
public class FedoraFileSystemConnectorTest {

    private FedoraFileSystemConnector connector;

    private final static String directoryPath = System.getProperty("java.io.tmpdir");
    private final static File tmpFile = new File(directoryPath, "fedora-filesystemtest.txt");

    @Mock
    private NamespaceRegistry mockRegistry;

    @Mock
    private NodeTypeManager mockNodeTypeManager;

    @Mock
    private DocumentTranslator mockTranslator;

    @Mock
    private NameFactory mockNameFactory;

    @Mock
    private ValueFactories mockValueFactories;

    @Mock
    private ExtraPropertiesStore mockExtraPropertiesStore;

    private ExecutionContext mockContext = new ExecutionContext();


    @BeforeClass
    public static void beforeClass()
    {
        try {
            FileOutputStream outputStream = new FileOutputStream(tmpFile);
            outputStream.write("hello".getBytes());
        } catch (IOException e) {
            System.err.println("Error creating: " + tmpFile.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    @AfterClass
    public static void afterClass() {
        try {
            tmpFile.delete();
        } catch (Exception e) {
            System.err.println("Error deleting: " + tmpFile.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        connector = new FedoraFileSystemConnector();
        TestHelpers.setField(connector, "directoryPath", directoryPath);
        TestHelpers.setField(connector, "translator", mockTranslator);
        TestHelpers.setField(connector, "context", mockContext);
        TestHelpers.setField(connector, "extraPropertiesStore", mockExtraPropertiesStore);

        TestHelpers.setField(mockTranslator, "names", mockNameFactory);
        connector.initialize(mockRegistry, mockNodeTypeManager);
    }

    @Test
    public void testGetDocumentByIdNull() throws Exception {
        Document doc = connector.getDocumentById(null);
        Assert.assertNull(doc);
    }

    @Test
    public void testGetDocumentByIdDatastream() throws Exception {
        when(mockTranslator.getPrimaryTypeName(any(Document.class))).thenReturn(JcrConstants.NT_FILE);
        when(mockNameFactory.create(anyString())).thenReturn(new BasicName("", tmpFile.getName()));

        Document doc = connector.getDocumentById("/" + tmpFile.getName());
        Assert.assertNotNull(doc);
    }

    @Test
    public void testGetDocumentByIdContent() throws Exception {
        when(mockTranslator.getPrimaryTypeName(any(Document.class))).thenReturn(JcrConstants.NT_RESOURCE);
        when(mockNameFactory.create(anyString())).thenReturn(new BasicName("", tmpFile.getName()));

        Property binaryProperty = Mockito.mock(Property.class, "BinaryProperty");
        BinaryValue binaryValue = Mockito.mock(BinaryValue.class, "BinayValue");
        when(binaryProperty.getFirstValue()).thenReturn(binaryValue);
        when(mockTranslator.getProperty(any(Document.class), Mockito.eq(JCR_DATA))).thenReturn(binaryProperty);

        Document doc = connector.getDocumentById("/" + tmpFile.getName());
        Assert.assertNotNull(doc);
    }

}
