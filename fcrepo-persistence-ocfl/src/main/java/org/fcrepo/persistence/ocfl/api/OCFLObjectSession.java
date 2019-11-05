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
     */
    void write(String subpath, InputStream stream);

    /**
     * Delete a file from this ocfl object.
     *
     * @param subpath path of the file relative to a version of an ocfl object
     */
    void delete(String subpath);

    /**
     * Read the state of the file at the specified subpath within the ocfl object as it exists within the current
     * session.
     *
     * @param subpath path of the file relative to a version of an ocfl object
     * @return contents of the file as an InputStream.
     */
    InputStream read(String subpath);

    /**
     * Read the state of the file at subpath from the specified version of the OCFL object.
     *
     * @param subpath path relative to the object
     * @param version identifier of the version
     * @return the contents of the file from the specified version
     */
    InputStream read(String subpath, String version);

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
     */
    void commit(CommitOption commitOption);

}