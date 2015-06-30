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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.modeshape.jcr.cache.document.DocumentTranslator;

import java.io.File;
import java.io.IOException;

import static java.nio.file.Files.createTempDirectory;
import static org.junit.Assert.assertEquals;
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
    @Test
    public void testSidecarFile() throws IOException {
        final File tmp = createTempDirectory("filesystem-federation").toFile();
        tmp.deleteOnExit();

        when(mockConnector.fileFor("/")).thenReturn(new File(FEDERATION_ROOT));
        when(mockConnector.isContentNode("/")).thenReturn(false);
        when(mockConnector.isRoot("/")).thenReturn(true);
        when(mockConnector.fileFor("/file")).thenReturn(new File(FILE_PATH));
        when(mockConnector.isContentNode("/file")).thenReturn(false);
        when(mockConnector.fileFor("/file/fcr:content")).thenReturn(new File(FILE_PATH));
        when(mockConnector.isContentNode("/file/fcr:content")).thenReturn(true);

        final ExternalJsonSidecarExtraPropertyStore store =
                new ExternalJsonSidecarExtraPropertyStore(mockConnector, mockTranslator, tmp);
        assertEquals(new File(tmp, "federation-root.modeshape.json"), store.sidecarFile("/"));
        assertEquals(new File(tmp, "file.modeshape.json"), store.sidecarFile("/file"));
        assertEquals(new File(tmp, "file.content.modeshape.json"), store.sidecarFile("/file/fcr:content"));
    }

}
