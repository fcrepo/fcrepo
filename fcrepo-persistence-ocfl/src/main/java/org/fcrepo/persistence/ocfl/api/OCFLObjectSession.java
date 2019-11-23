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

package org.fcrepo.persistence.ocfl.api;

import java.io.InputStream;
import java.util.List;

import edu.wisc.library.ocfl.api.model.VersionDetails;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

/**
 * A session for building and tracking the state of an OCFL object within a persistence session.
 *
 * @author bbpennel
 */
public interface OCFLObjectSession {

    /**
     * The default commit option configured for the OCFL Object represented by this session.
     * If no default commit option has been defined for the specific OCFL Object, the
     * value returned will be the system defined global default commit option.
     * @return The default commit option
     */
    CommitOption getDefaultCommitOption();

    /**
     * Write the provided content to specified subpath.
     *
     * @param subpath path of the resource to write, relative to the OCFL object
     * @param stream stream of content to write
     * @return information about the data written.
     * @throws PersistentStorageException thrown if unable to persist content
     */
    WriteOutcome write(String subpath, InputStream stream) throws PersistentStorageException;

    /**
     * Delete a file from this ocfl object.
     *
     * @param subpath path of the file relative to a version of an ocfl object
     * @throws PersistentStorageException if unable to delete file
     */
    void delete(String subpath) throws PersistentStorageException;

    /**
     * Delete the object specified by this session
     *
     * @throws PersistentStorageException if unable to delete the object
     */
    void deleteObject() throws PersistentStorageException;

    /**
     * Read the state of the file at the specified subpath within the ocfl object as it exists within the current
     * session.
     *
     * @param subpath path of the file relative to a version of an ocfl object
     * @return contents of the file as an InputStream.
     * @throws PersistentStorageException if unable to read the file
     */
    InputStream read(String subpath) throws PersistentStorageException;

    /**
     * Read the state of the file at subpath from the specified version of the OCFL object.
     *
     * @param subpath path relative to the object
     * @param version identifier of the version. If null, the head state of the file will be returned.
     * @return the contents of the file from the specified version
     * @throws PersistentStorageException if unable to read the file
     */
    InputStream read(String subpath, String version) throws PersistentStorageException;

    /**
     * Verify that the change set in this session can be committed. A PersistentStorageException is thrown if there
     * are any conflicts that would prevent a commit.
     */
    void prepare();

    /**
     * Commit the change set from this session to the OCFL object, following the strategy suggested by commitOption.
     * Creates the OCFL object if it did not previous exist.
     *
     * @param commitOption option indicating where changes should be committed.
     * @return identifier of the version committed
     * @throws PersistentStorageException if unable to commit
     */
    String commit(CommitOption commitOption) throws PersistentStorageException;

    /**
     * Close this session without committing changes.
     *
     * @throws PersistentStorageException if unable to close the session.
     */
    void close() throws PersistentStorageException;


    /**
     * Return the list of immutable versions associated with this OCFL Object in chronological order.
     * @return The list of versions
     * @throws PersistentStorageException If the versions cannot be read due to the underlying session being closed
     *                                    or for some other reason.
     */
    List<VersionDetails> listVersions() throws PersistentStorageException;

}