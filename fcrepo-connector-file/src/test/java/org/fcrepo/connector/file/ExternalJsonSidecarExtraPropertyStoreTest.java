/**
 * Copyright 2015 DuraSpace, Inc.
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

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Json;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.basic.BasicName;
import org.modeshape.jcr.value.basic.BasicSingleValueProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.Files.createTempDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mike Durbin
 */
@RunWith(MockitoJUnitRunner.class)
public class ExternalJsonSidecarExtraPropertyStoreTest {

    @Mock
    private FedoraFileSystemConnector mockConnector;

    @Mock
    private DocumentTranslator mockTranslator;

    private static final String FEDERATION_ROOT = "/federation-root";
    private static final String FILE_PATH = "/federation-root/file";
    private static final Name KEY1 = new BasicName("info", "one");
    private static final Name KEY2 = new BasicName("info", "two");
    private static final Name KEY3 = new BasicName("info", "three");
    private static final Name KEY4 = new BasicName("info", "four");
    private static final Name LANG_PROP = new BasicName("lang", "de");
    private static final Property PROP1 = new BasicSingleValueProperty(LANG_PROP, "eins");
    private static final Property PROP2 = new BasicSingleValueProperty(LANG_PROP, "zwei");
    private static final Property PROP3 = new BasicSingleValueProperty(LANG_PROP, "drei");

    @Before
    public void setUp() {
        when(mockConnector.fileFor("/")).thenReturn(new File(FEDERATION_ROOT));
        when(mockConnector.isContentNode("/")).thenReturn(false);
        when(mockConnector.isRoot("/")).thenReturn(true);
        when(mockConnector.fileFor("/file")).thenReturn(new File(FILE_PATH));
        when(mockConnector.isContentNode("/file")).thenReturn(false);
        when(mockConnector.fileFor("/file/fcr:content")).thenReturn(new File(FILE_PATH));
        when(mockConnector.isContentNode("/file/fcr:content")).thenReturn(true);
        when(mockConnector.fileFor("/test/test")).thenReturn(new File(FEDERATION_ROOT + "/test/test"));
        when(mockConnector.isContentNode("/test/test")).thenReturn(false);
    }

    @Test
    public void testSidecarFile() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);
        assertEquals(new File(tmp, "federation-root.modeshape.json"), store.sidecarFile("/"));
        assertEquals(new File(tmp, "file.modeshape.json"), store.sidecarFile("/file"));
        assertEquals(new File(tmp, "file.content.modeshape.json"), store.sidecarFile("/file/fcr:content"));
        assertEquals(new File(tmp + "/test", "test.modeshape.json"), store.sidecarFile("/test/test"));
    }

    @Test
    public void testEmptyPropertyFile() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);

        assertFalse(store.contains("/file"));
        assertTrue(store.getProperties("/file").isEmpty());
    }

    @Test
    public void testStoreProperties() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);

        final Map<Name, Property> properties = new HashMap<>();

        store.storeProperties("/file", properties);

        assertFalse(store.contains("/file"));
        assertTrue(store.getProperties("/file").isEmpty());

        properties.put(KEY1, PROP1);
        properties.put(KEY2, PROP2);
        properties.put(KEY3, PROP3);
        properties.put(KEY4, null);

        store.storeProperties("/file", properties);
        assertTrue(store.contains("/file"));

        properties.forEach((key, property) -> {
            if (property != null) {
                verify(mockTranslator).setProperty(any(EditableDocument.class), eq(property), anyObject(), anyObject());
            }
        });
        verify(mockTranslator, times(3)).setProperty(any(EditableDocument.class),
                    any(Property.class), anyObject(), anyObject());
    }

    @Test
    public void testStoreExistingProperties() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        final File sidecarFile = new File(tmp, "file.modeshape.json");
        tmp.deleteOnExit();
        final String jsonString = "{}";
        final FileOutputStream fos = new FileOutputStream(sidecarFile);
        fos.write(jsonString.getBytes("UTF-8"));
        fos.close();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);

        final Map<Name, Property> properties = new HashMap<>();

        assertTrue(store.contains("/file"));

        properties.put(KEY1, PROP1);
        properties.put(KEY2, PROP2);
        properties.put(KEY3, PROP3);
        properties.put(KEY4, null);

        store.storeProperties("/file", properties);

        properties.forEach((key, property) -> {
            if (property != null) {
                verify(mockTranslator).setProperty(any(EditableDocument.class), eq(property), anyObject(), anyObject());
            }
        });
        verify(mockTranslator, times(3)).setProperty(any(EditableDocument.class),
                    any(Property.class), anyObject(), anyObject());
    }


    @Test
    public void testUpdateProperties() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);

        final Map<Name, Property> properties = new HashMap<>();

        store.updateProperties("/file", properties);

        assertFalse(store.contains("/file"));
        assertTrue(store.getProperties("/file").isEmpty());

        properties.put(KEY1, PROP1);
        properties.put(KEY2, PROP2);
        properties.put(KEY3, null);

        store.updateProperties("/file", properties);

        properties.forEach((key, property) -> {
            if (property == null) {
                verify(mockTranslator).removeProperty(any(EditableDocument.class), eq(key), anyObject(), anyObject());
            } else {
                verify(mockTranslator).setProperty(any(EditableDocument.class), eq(property), anyObject(), anyObject());
            }
        });
    }

    @Test
    public void testUpdateExistingProperties() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();
        final File sidecarFile = new File(tmp, "file.modeshape.json");
        final String jsonString = "{}";
        final FileOutputStream fos = new FileOutputStream(sidecarFile);
        fos.write(jsonString.getBytes("UTF-8"));
        fos.close();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);

        assertTrue(store.contains("/file"));

        final Map<Name, Property> properties = new HashMap<>();

        properties.put(KEY1, PROP1);
        properties.put(KEY2, PROP2);
        properties.put(KEY3, null);

        store.updateProperties("/file", properties);

        properties.forEach((key, property) -> {
            if (property == null) {
                verify(mockTranslator).removeProperty(any(EditableDocument.class), eq(key), anyObject(), anyObject());
            } else {
                verify(mockTranslator).setProperty(any(EditableDocument.class), eq(property), anyObject(), anyObject());
            }
        });
    }

    @Test
    public void testRemoveProperties() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);

        final Map<Name, Property> properties = new HashMap<>();

        properties.put(KEY1, PROP1);
        properties.put(KEY2, PROP2);
        properties.put(KEY3, PROP3);

        store.updateProperties("/file", properties);
        assertTrue(store.contains("/file"));

        store.removeProperties("/file");
        assertFalse(store.contains("/file"));
    }

    @Test
    public void testGetProperties() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();
        final File sidecarFile = new File(tmp, "file.content.modeshape.json");
        final String jsonString = "{" +
            "\"properties\" : { " +
                "\"http://www.jcp.org/jcr/1.0\" : {" +
                    "\"created\" : { " +
                        "\"$date\" : \"2008-09-23T15:19:20.000-04:00\" } } ," +
                "\"http://fedora.info/definitions/v4/repository#\" : {" +
                    "\"digest\" : { " +
                        "\"$uri\" : \"urn:sha1:6e1a2e24a4cc3dde495877019f53830b8f1d20e3\" } } } }";
        final FileOutputStream fos = new FileOutputStream(sidecarFile);
        fos.write(jsonString.getBytes("UTF-8"));
        fos.close();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);

        assertTrue(store.contains("/file/fcr:content"));

        final FileInputStream sidecarStream = new FileInputStream(sidecarFile);
        final Document document = Json.read(sidecarStream, false);
        final Map<Name, Property> results = new HashMap<>();

        store.getProperties("/file/fcr:content");

        verify(mockTranslator).getProperties(eq(document), eq(results));
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testGetPropertiesWithException() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();
        final File sidecarFile = new File(tmp, "file.modeshape.json");
        final String jsonString = "{ THIS ISN'T JSON !";
        final FileOutputStream fos = new FileOutputStream(sidecarFile);
        fos.write(jsonString.getBytes("UTF-8"));
        fos.close();

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);

        store.getProperties("/file");
    }

}
