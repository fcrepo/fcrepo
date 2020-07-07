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

import org.fcrepo.kernel.api.utils.ContentDigest.DIGEST_ALGORITHM;
import org.fcrepo.persistence.api.CommitOption;
import org.fcrepo.persistence.api.WriteOutcome;
import org.fcrepo.persistence.api.exceptions.PersistentStorageException;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/**
 * A session for building and tracking the state of an OCFL object within a persistence session.
 *
 * @author bbpennel
 */
public interface OCFLObjectSession {

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
     * Register a transmission digest for the specified subpath, where the digest is of
     * the same algorithm as is used by the object this session pertains to.
     *
     * @param subpath path of the file relative to a version of an ocfl object
     * @param digest content-addressing digest of the file
     */
    void registerTransmissionDigest(String subpath, String digest);

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
     * Overrides the default {@link CommitOption} to use when the session is committed. By default, is
     * {@link CommitOption#NEW_VERSION} when fcrepo.autoversioning.enabled is true,
     * and {@link CommitOption#UNVERSIONED} when it is false
     *
     * @param commitOption the commit option
     */
    void setCommitOption(CommitOption commitOption);

    /**
     * @return the commit option that's configured on the session
     */
    CommitOption getCommitOption();

    /**
     * Verify that the change set in this session can be committed. A PersistentStorageException is thrown if there
     * are any conflicts that would prevent a commit.
     */
    void prepare();

    /**
     * Commit the change set from this session to the OCFL object, following the strategy suggested by commitOption.
     * Creates the OCFL object if it did not previous exist.
     *
     * @return identifier of the version committed
     * @throws PersistentStorageException if unable to commit
     */
    String commit() throws PersistentStorageException;

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
    List<OCFLVersion> listVersions() throws PersistentStorageException;

    /**
     * Return the list of immutable versions associated with this OCFL Object subpath in chronological order. Only
     * versions where the subpath changed are returned.
     *
     * @param subpath path relative to the object; if the subpath is blank all of the object versions are listed
     * @return The list of versions
     * @throws PersistentStorageException If the versions cannot be read due to the underlying session being closed
     *                                    or for some other reason.
     */
    List<OCFLVersion> listVersions(String subpath) throws PersistentStorageException;

    /**
     * The instant at which the session was created.
     * @return
     */
    Instant getCreated();

    /**
     * Lists the subpaths associated with the HEAD (ie the latest committed state)
     *
     * @return the subpaths as a stream
     * @throws PersistentStorageException If subpaths cannot be listed due to the underlying session being closed
     *                                    or for some other reason.
     */
    Stream<String> listHeadSubpaths() throws PersistentStorageException;

    /**
     * Get the digest algorithm used by the OCFL object which is the subject of this session
     *
     * @return the digest algorithm used by the OCFL object
     */
    DIGEST_ALGORITHM getObjectDigestAlgorithm();

    /**
     * Determine if the subpath does/will not exist in the HEAD of this object.
     *
     * @param subpath the subpath to the resource.
     *
     * @return true if this OCFL object is being deleted or has not been persisted yet or if this subpath has not been
     * persisted.
     */
    boolean isNewInSession(final String subpath);
}
