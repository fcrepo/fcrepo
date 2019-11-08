/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.persistence.ocfl.impl;

import static org.fcrepo.persistence.ocfl.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.ocfl.api.CommitOption.MUTABLE_HEAD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static edu.wisc.library.ocfl.api.OcflOption.MOVE_SOURCE;
import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflObjectVersion;
import edu.wisc.library.ocfl.api.model.CommitInfo;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.mapping.ObjectIdPathMapperBuilder;
import edu.wisc.library.ocfl.core.storage.FileSystemOcflStorage;

/**
 * @author bbpennel
 *
 */
public class DefaultOCFLObjectSessionTest {

    private final static String OBJ_ID = "obj1";

    private final static String FILE1_SUBPATH = "test_file1.txt";

    private final static String FILE2_SUBPATH = "test_file2.txt";

    private final static String FILE_CONTENT1 = "Some content";

    private final static String FILE_CONTENT2 = "Content, 6.0";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path stagingPath;

    private DefaultOCFLObjectSession session;

    private MutableOcflRepository ocflRepository;

    @Before
    public void setup() throws Exception {
        tempFolder.create();

        stagingPath = tempFolder.newFolder("obj1-staging").toPath();
        final var repoDir = tempFolder.newFolder("ocfl-repo").toPath();
        final var workDir = tempFolder.newFolder("ocfl-work").toPath();

        ocflRepository = new OcflRepositoryBuilder().buildMutable(
                new FileSystemOcflStorage(repoDir,
                        new ObjectIdPathMapperBuilder().buildFlatMapper()),
                workDir);

        session = makeNewSession();
    }

    private DefaultOCFLObjectSession makeNewSession() {
        return new DefaultOCFLObjectSession(OBJ_ID, stagingPath, ocflRepository);
    }

    @Test
    public void writeNewFile_ToNewVersion_NewObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream());
        final String versionId = session.commit(NEW_VERSION);

        assertEquals("v1", versionId);
        assertNoMutableHead(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void writeNewFile_ToMHead_NewObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream());
        session.commit(MUTABLE_HEAD);

        assertMutableHeadPopulated(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void commitWithoutOption() throws Exception {
        session.write(FILE1_SUBPATH, fileStream());
        session.commit(null);
    }

    @Test
    public void overwriteStagedFile_NewVersion_NewObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        // Change the same file again
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        final String versionId = session.commit(NEW_VERSION);

        assertEquals("v1", versionId);
        assertNoMutableHead(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT2);
    }

    @Test
    public void replaceFile_NewVersion_ExistingObject() throws Exception {
        final var preStagingPath = tempFolder.newFolder("prestage").toPath();
        Files.writeString(preStagingPath.resolve(FILE1_SUBPATH), FILE_CONTENT1);
        ocflRepository.putObject(ObjectVersionId.head(OBJ_ID), preStagingPath, new CommitInfo());

        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        final String versionId = session.commit(NEW_VERSION);

        assertEquals("v2", versionId);
        assertNoMutableHead(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT2);
        assertFileInVersion(OBJ_ID, "v1", FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void replaceFile_MHead_ExistingObject() throws Exception {
        final var preStagingPath = tempFolder.newFolder("prestage").toPath();
        Files.writeString(preStagingPath.resolve(FILE1_SUBPATH), FILE_CONTENT1);
        ocflRepository.stageChanges(ObjectVersionId.head(OBJ_ID), new CommitInfo(), updater -> {
            updater.addPath(preStagingPath, "", MOVE_SOURCE);
        });

        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        final String versionId = session.commit(MUTABLE_HEAD);

        assertEquals("Multiple commits to head should stay on version", "v2", versionId);
        assertMutableHeadPopulated(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT2);
    }

    @Test
    public void writeToMHeadThenNewVersion() throws Exception {
        // Write first file to mutable head
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(MUTABLE_HEAD);

        assertMutableHeadPopulated(OBJ_ID);

        stagingPath = tempFolder.newFolder("obj1-staging").toPath();
        session = new DefaultOCFLObjectSession(OBJ_ID, stagingPath, ocflRepository);

        // Commit changes to new version in second commit
        session.write(FILE2_SUBPATH, fileStream(FILE_CONTENT2));
        final String versionId = session.commit(NEW_VERSION);

        assertEquals("v2", versionId);
        assertNoMutableHead(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT1);
        assertFileInHeadVersion(OBJ_ID, FILE2_SUBPATH, FILE_CONTENT2);
        // Initial version is empty since created with mutable head
        assertFileNotInVersion(OBJ_ID, "v1", FILE1_SUBPATH);
        assertFileNotInVersion(OBJ_ID, "v1", FILE2_SUBPATH);
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void read_ObjectNotExist() throws Exception {
        session.read(FILE1_SUBPATH);
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void read_FileNotExist() throws Exception {
        Files.writeString(stagingPath.resolve(FILE1_SUBPATH), FILE_CONTENT1);
        ocflRepository.putObject(ObjectVersionId.head(OBJ_ID), stagingPath, new CommitInfo());

        session.read(FILE2_SUBPATH);
    }

    @Test
    public void read_FromStaged_NewObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));

        assertStreamMatches(FILE_CONTENT1, session.read(FILE1_SUBPATH));
    }

    @Test
    public void read_FromMHead() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(MUTABLE_HEAD);

        assertStreamMatches(FILE_CONTENT1, session.read(FILE1_SUBPATH));
    }

    @Test
    public void read_FromVersion() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        assertStreamMatches(FILE_CONTENT1, session.read(FILE1_SUBPATH));
    }

    @Test
    public void read_FromStagedAndVersion() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));

        assertStreamMatches(FILE_CONTENT2, session2.read(FILE1_SUBPATH));
        assertStreamMatches(FILE_CONTENT1, session2.read(FILE1_SUBPATH, "v1"));
    }

    @Test
    public void read_FromStagedAndMHead() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(MUTABLE_HEAD);

        final var session2 = makeNewSession();
        session2.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));

        assertStreamMatches(FILE_CONTENT2, session2.read(FILE1_SUBPATH));
        // initial version when creating with mutable head is version 2
        assertStreamMatches(FILE_CONTENT1, session2.read(FILE1_SUBPATH, "v2"));
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void read_FromVersion_NotExist() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        // Try a path that doesn't exist
        session.read(FILE2_SUBPATH, "v1");
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void read_FromVersion_ObjectNotExist() throws Exception {
        // No object created
        session.read(FILE2_SUBPATH, "v1");
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void read_FromNonExistentVersion() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        // version that hasn't been created yet
        session.read(FILE2_SUBPATH, "v99");
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void delete_FileNotExist() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        // Try a path that doesn't exist
        session2.delete(FILE2_SUBPATH);
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void delete_ObjectNotExist() throws Exception {
        // Object not created or populated yet
        session.delete(FILE1_SUBPATH);
    }

    @Test
    public void delete_FromStaged_InitialVersion() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));

        assertEquals(1, stagingPath.toFile().listFiles().length);

        session.delete(FILE1_SUBPATH);

        assertEquals(0, stagingPath.toFile().listFiles().length);
    }

    @Test
    public void delete_FromStaged_VersionsExist() throws Exception {

    }

    @Test
    public void delete_FromMHead() throws Exception {

    }

    @Test
    public void delete_FromVersion() throws Exception {

    }

    @Test
    public void delete_FromStagedAndVersion() throws Exception {

    }

    @Test
    public void delete_FromStagedAndMHead() throws Exception {

    }

    private InputStream fileStream() {
        return fileStream(FILE_CONTENT1);
    }

    private InputStream fileStream(final String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private void assertFileInHeadVersion(final String objId, final String subpath, final String content) {
        final OcflObjectVersion objVersion = ocflRepository.getObject(ObjectVersionId.head(objId));
        assertFileContents(subpath, objVersion, content);
    }

    private void assertFileInVersion(final String objId, final String versionId, final String subpath,
            final String content) {
        final OcflObjectVersion objVersion = ocflRepository.getObject(ObjectVersionId.version(objId, versionId));
        assertFileContents(subpath, objVersion, content);
    }

    private void assertFileNotInVersion(final String objId, final String versionId, final String subpath) {
        final OcflObjectVersion objVersion = ocflRepository.getObject(ObjectVersionId.version(objId, versionId));
        assertFalse(String.format("File %s must not be in %s version %s", subpath, objId, versionId),
                objVersion.containsFile(subpath));
    }

    private void assertFileContents(final String subpath, final OcflObjectVersion objVersion, final String content) {
        assertStreamMatches(content, objVersion.getFile(subpath).getStream());
    }

    private void assertStreamMatches(final String expectedContent, final InputStream contentStream) {
        try {
            assertEquals(expectedContent, IOUtils.toString(contentStream, "UTF-8"));
        } catch (final IOException e) {
            fail(format("Failed to read stream due to %s", e.getMessage()));
        }
    }

    private void assertMutableHeadPopulated(final String objId) {
        assertTrue("Mutable head must be populated for " + objId, ocflRepository.hasStagedChanges(objId));
    }

    private void assertNoMutableHead(final String objId) {
        assertFalse("No mutable head should be present for " + objId, ocflRepository.hasStagedChanges(objId));
    }

    // read file
    //   does not exist
    //   does exist
    //   is in staged changes
    //   is in both
    // from past version
    // from invalid version

    // write file that doesn't exist
    // write file that is already staged
    // write file in sub dir

    // multiple writes

    // delete (are these meaningful before a commit?)
    //   file that is staged
    //   file that is not staged
    //   file that is staged and already exists
    //   doesn't exist

    // commit
    //   a new object to new version
    //   a new object to head
    //   commit to existing object new v
    //   commit to existing obj head
    //   commit to new version with existing head
    //   no commit option new obj
    //   no commit option existing

}
