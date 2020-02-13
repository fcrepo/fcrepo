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

import static edu.wisc.library.ocfl.api.OcflOption.MOVE_SOURCE;
import static java.lang.String.format;
import static org.fcrepo.persistence.api.CommitOption.NEW_VERSION;
import static org.fcrepo.persistence.api.CommitOption.UNVERSIONED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.wisc.library.ocfl.core.storage.filesystem.FileSystemOcflStorage;
import org.apache.commons.io.IOUtils;
import org.fcrepo.persistence.api.exceptions.PersistentItemNotFoundException;
import org.fcrepo.persistence.api.exceptions.PersistentSessionClosedException;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflObjectVersion;
import edu.wisc.library.ocfl.api.model.ObjectDetails;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionDetails;
import edu.wisc.library.ocfl.api.model.VersionId;
import edu.wisc.library.ocfl.core.OcflRepositoryBuilder;
import edu.wisc.library.ocfl.core.extension.layout.config.DefaultLayoutConfig;

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

        final var repoDir = tempFolder.newFolder("ocfl-repo").toPath();
        final var workDir = tempFolder.newFolder("ocfl-work").toPath();

        ocflRepository = new OcflRepositoryBuilder()
                .layoutConfig(DefaultLayoutConfig.flatPairTreeConfig())
                .workDir(workDir)
                .storage(FileSystemOcflStorage.builder().repositoryRoot(repoDir).build())
                .buildMutable();

        session = makeNewSession();
    }

    private DefaultOCFLObjectSession makeNewSession() throws Exception {
        if (stagingPath == null || !stagingPath.toFile().exists()) {
            stagingPath = tempFolder.newFolder("obj1-staging").toPath();
        }
        return new DefaultOCFLObjectSession(OBJ_ID, stagingPath, ocflRepository);
    }

    @Test
    public void writeNewFile_ToNewVersion_NewObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        final String versionId = session.commit(NEW_VERSION);

        assertEquals("v1", versionId);
        assertNoMutableHead(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void writeNewFile_ToMHead_NewObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(UNVERSIONED);

        assertMutableHeadPopulated(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void write_OverwriteStagedFile_NewVersion_NewObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        // Change the same file again
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        final String versionId = session.commit(NEW_VERSION);

        assertEquals("v1", versionId);
        assertNoMutableHead(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT2);
    }

    @Test
    public void write_ReplaceFile_NewVersion_ExistingObject() throws Exception {
        final var preStagingPath = tempFolder.newFolder("prestage").toPath();
        Files.writeString(preStagingPath.resolve(FILE1_SUBPATH), FILE_CONTENT1);
        ocflRepository.putObject(ObjectVersionId.head(OBJ_ID), preStagingPath, null);

        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        final String versionId = session.commit(NEW_VERSION);

        assertEquals("v2", versionId);
        assertNoMutableHead(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT2);
        assertFileInVersion(OBJ_ID, "v1", FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void write_ReplaceFileInMHead_ExistingObject() throws Exception {
        final var preStagingPath = tempFolder.newFolder("prestage").toPath();
        Files.writeString(preStagingPath.resolve(FILE1_SUBPATH), FILE_CONTENT1);
        ocflRepository.stageChanges(ObjectVersionId.head(OBJ_ID), null, updater -> {
            updater.addPath(preStagingPath, "", MOVE_SOURCE);
        });

        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        final String versionId = session.commit(UNVERSIONED);

        assertEquals("Multiple commits to head should stay on version", "v2", versionId);
        assertMutableHeadPopulated(OBJ_ID);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT2);
    }

    @Test
    public void writeToMHeadThenNewVersion() throws Exception {
        // Write first file to mutable head
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(UNVERSIONED);

        assertMutableHeadPopulated(OBJ_ID);

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

    @Test(expected = PersistentSessionClosedException.class)
    public void write_CommittedSession() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        session.write(FILE2_SUBPATH, fileStream(FILE_CONTENT1));
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void read_ObjectNotExist() throws Exception {
        session.read(FILE1_SUBPATH);
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void read_FileNotExist() throws Exception {
        Files.writeString(stagingPath.resolve(FILE1_SUBPATH), FILE_CONTENT1);
        ocflRepository.putObject(ObjectVersionId.head(OBJ_ID), stagingPath, null);

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
        session.commit(UNVERSIONED);

        final var session2 = makeNewSession();
        assertStreamMatches(FILE_CONTENT1, session2.read(FILE1_SUBPATH));
    }

    @Test
    public void read_FromVersion() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        assertStreamMatches(FILE_CONTENT1, session2.read(FILE1_SUBPATH));
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
        session.commit(UNVERSIONED);

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

        final var session2 = makeNewSession();
        // Try a path that doesn't exist
        session2.read(FILE2_SUBPATH, "v1");
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

        final var session2 = makeNewSession();
        // version that hasn't been created yet
        session2.read(FILE2_SUBPATH, "v99");
    }

    @Test
    public void read_CommittedSession() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        assertStreamMatches(FILE_CONTENT1, session2.read(FILE1_SUBPATH));
    }

    // Verify that subpaths with parent directories are created and readable
    @Test
    public void nestedPath_NewVersion_NewObject() throws Exception {
        final String nestedSubpath = "path/to/the/file.txt";
        session.write(nestedSubpath, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        assertStreamMatches(FILE_CONTENT1, session2.read(nestedSubpath));
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
        assertEquals(1, stagingPath.resolve(OBJ_ID).toFile().listFiles().length);

        session.delete(FILE1_SUBPATH);

        assertEquals(1, stagingPath.toFile().listFiles().length);
        assertEquals(0, stagingPath.resolve(OBJ_ID).toFile().listFiles().length);
    }

    @Test
    public void delete_FromStaged_OtherFilesVersioned() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.write(FILE2_SUBPATH, fileStream(FILE_CONTENT2));

        // Try a path that doesn't exist
        session2.delete(FILE2_SUBPATH);
        session2.commit(NEW_VERSION);

        assertFileNotInHeadVersion(OBJ_ID, FILE2_SUBPATH);
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void delete_FromMHead() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(UNVERSIONED);

        final var session2 = makeNewSession();
        session2.delete(FILE1_SUBPATH);
        session2.commit(UNVERSIONED);

        assertFileNotInHeadVersion(OBJ_ID, FILE1_SUBPATH);
    }

    @Test
    public void delete_FromVersion() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.delete(FILE1_SUBPATH);
        session2.commit(NEW_VERSION);

        assertFileNotInHeadVersion(OBJ_ID, FILE1_SUBPATH);
        assertFileInVersion(OBJ_ID, "v1", FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void delete_FromStagedAndVersion() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        session2.delete(FILE1_SUBPATH);
        session2.commit(NEW_VERSION);

        assertFileNotInHeadVersion(OBJ_ID, FILE1_SUBPATH);
        assertFileInVersion(OBJ_ID, "v1", FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void delete_FromStagedAndMHead() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(UNVERSIONED);

        final var session2 = makeNewSession();
        session2.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        session2.delete(FILE1_SUBPATH);
        session2.commit(UNVERSIONED);

        assertFileNotInHeadVersion(OBJ_ID, FILE1_SUBPATH);
        assertFileNotInVersion(OBJ_ID, "v1", FILE1_SUBPATH);
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void delete_FromDeletedObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        // Try to delete a file from an object that was just deleted
        session2.deleteObject();
        session2.delete(FILE1_SUBPATH);
        session2.commit(NEW_VERSION);

        assertFileNotInHeadVersion(OBJ_ID, FILE1_SUBPATH);
        assertFileNotInVersion(OBJ_ID, "v1", FILE1_SUBPATH);
    }

    @Test
    public void delete_FileAddedAfterObjectDeleted() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        // Try to delete a file from an object that was just deleted
        session2.deleteObject();
        // Adding 2 files so that staging won't be empty when committing
        session2.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        session2.write(FILE2_SUBPATH, fileStream(FILE_CONTENT2));
        session2.delete(FILE1_SUBPATH);
        session2.commit(NEW_VERSION);

        assertFileNotInHeadVersion(OBJ_ID, FILE1_SUBPATH);
        assertFileInHeadVersion(OBJ_ID, FILE2_SUBPATH, FILE_CONTENT2);

        assertOnlyFirstVersionExists();
    }

    @Test(expected = PersistentSessionClosedException.class)
    public void delete_CommittedSession() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        session.delete(FILE1_SUBPATH);
    }

    @Test
    public void deleteObject_ObjectExists() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.deleteObject();
        session2.commit(NEW_VERSION);

        assertFalse("Deleted object must not exist",
                ocflRepository.containsObject(OBJ_ID));
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void deleteObject_NotExists() throws Exception {
        session.deleteObject();
    }

    @Test
    public void deleteObject_StagedChanges_ObjectNotCreated() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.deleteObject();
        session.commit(NEW_VERSION);

        assertFalse("Deleted object must not exist",
                ocflRepository.containsObject(OBJ_ID));
    }

    @Test
    public void deleteObject_CreateAndRecreateSameSession() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.deleteObject();
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        session.commit(NEW_VERSION);

        // State of object should reflect post-delete object state
        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT2);
    }

    @Test
    public void deleteObject_Recreate() throws Exception {
        // Create object
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        // Delete object
        final var session2 = makeNewSession();
        session2.deleteObject();
        session2.commit(NEW_VERSION);

        // Recreate object
        final var session3 = makeNewSession();
        session3.write(FILE1_SUBPATH, fileStream(FILE_CONTENT2));
        session3.commit(NEW_VERSION);

        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT2);

        assertOnlyFirstVersionExists();
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void read_FromDeletedObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.deleteObject();
        session2.read(FILE1_SUBPATH);
    }

    @Test(expected = PersistentSessionClosedException.class)
    public void deleteObject_CommittedSession() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        session.deleteObject();
    }

    @Test(expected = PersistentItemNotFoundException.class)
    public void readVersion_FromDeletedObject() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.deleteObject();
        session2.read(FILE1_SUBPATH, "v1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void commit_WithoutOption() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(null);
    }

    @Test(expected = PersistentStorageException.class)
    public void commit_NewObject_NoContents() throws Exception {
        session.commit(NEW_VERSION);
    }

    @Test
    public void commit_NewVersion_NoChanges() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.commit(NEW_VERSION);

        final ObjectDetails objDetails = ocflRepository.describeObject(OBJ_ID);
        final var versionMap = objDetails.getVersionMap();
        assertEquals("Two versions should exist", 2, versionMap.size());

        assertFileInVersion(OBJ_ID, "v1", FILE1_SUBPATH, FILE_CONTENT1);
        assertFileInVersion(OBJ_ID, "v2", FILE1_SUBPATH, FILE_CONTENT1);
    }

    @Test
    public void commit_MHead_NoChanges() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(UNVERSIONED);

        final var session2 = makeNewSession();
        session2.commit(UNVERSIONED);

        final ObjectDetails objDetails = ocflRepository.describeObject(OBJ_ID);
        final var versionMap = objDetails.getVersionMap();
        assertEquals("Only the mutable head and initial version exist", 2, versionMap.size());

        assertFileInVersion(OBJ_ID, "v2", FILE1_SUBPATH, FILE_CONTENT1);
        assertFileNotInVersion(OBJ_ID, "v1", FILE1_SUBPATH);
    }

    @Test(expected = PersistentSessionClosedException.class)
    public void commit_CommittedSession() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        session.commit(NEW_VERSION);
    }

    @Test
    public void close_SessionWithChanges() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        final var session2 = makeNewSession();
        session2.write(FILE2_SUBPATH, fileStream(FILE_CONTENT2));
        session2.close();

        assertOnlyFirstVersionExists();

        assertFileInHeadVersion(OBJ_ID, FILE1_SUBPATH, FILE_CONTENT1);
        assertFileNotInHeadVersion(OBJ_ID, FILE2_SUBPATH);
    }

    @Test
    public void close_CommittedSession() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);

        session.close();
    }

    @Test
    public void listVersions() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.commit(NEW_VERSION);
        session.close();

        final var session1 = makeNewSession();

        session1.commit(NEW_VERSION);
        session1.close();

        final var session2 = makeNewSession();
        session2.commit(UNVERSIONED);
        session2.close();

        final var session3 = makeNewSession();

        final List<VersionDetails> versions = session3.listVersions();
        session3.close();

        assertEquals("First version in list is not \"v1\"", "v1", versions.get(0).getVersionId().toString());
        assertEquals("Second version in list is not \"v2\"", "v2", versions.get(1).getVersionId().toString());
        assertEquals("There should be exactly two versions",2, versions.size());
    }

    @Test
    public void listHeadSubpaths() throws Exception {
        session.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session.write(FILE2_SUBPATH, fileStream(FILE_CONTENT2));
        session.commit(NEW_VERSION);
        session.close();

        final var session2 = makeNewSession();

        final List<String> subpaths = session2.listHeadSubpaths().collect(Collectors.toList());

        session2.close();

        assertEquals("Expected 2 subpaths", 2, subpaths.size());
        Arrays.asList(FILE1_SUBPATH, FILE2_SUBPATH).stream().forEach(subpath -> {
            assertTrue(format("Expected subpath %s to be presented", subpath), subpaths.contains(subpath));
        });
    }

    @Test
    public void ensureFilesDontStageTogether() throws Exception {
        final String obj1ID = UUID.randomUUID().toString();
        final String obj2ID = UUID.randomUUID().toString();
        final var session1 = new DefaultOCFLObjectSession(obj1ID, stagingPath, ocflRepository);
        final var session2 = new DefaultOCFLObjectSession(obj2ID, stagingPath, ocflRepository);
        session1.write(FILE1_SUBPATH, fileStream(FILE_CONTENT1));
        session2.write(FILE2_SUBPATH, fileStream(FILE_CONTENT2));
        session1.commit(NEW_VERSION);
        session2.commit(NEW_VERSION);
        assertFileInVersion(obj1ID, "v1", FILE1_SUBPATH, FILE_CONTENT1);
        assertFileNotInVersion(obj1ID, "v1", FILE2_SUBPATH);
        assertFileInVersion(obj2ID, "v1", FILE2_SUBPATH, FILE_CONTENT2);
        assertFileNotInVersion(obj2ID, "v1", FILE1_SUBPATH);
    }

    private static InputStream fileStream(final String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private void assertOnlyFirstVersionExists() {
        final ObjectDetails objDetails = ocflRepository.describeObject(OBJ_ID);
        final var versionMap = objDetails.getVersionMap();
        assertEquals("Only a single version should remain", 1, versionMap.size());
        assertTrue("First version must be present",
                versionMap.containsKey(VersionId.fromString("v1")));
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

    private void assertFileNotInHeadVersion(final String objId, final String subpath) {
        final OcflObjectVersion objVersion = ocflRepository.getObject(ObjectVersionId.head(objId));
        assertFalse(String.format("File %s must not be in %s head version", subpath, objId),
                objVersion.containsFile(subpath));
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
}
