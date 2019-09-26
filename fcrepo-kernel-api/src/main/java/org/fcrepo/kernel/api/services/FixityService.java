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
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.models.Binary;

import java.net.URI;
import java.util.Collection;

public interface FixityService {

  /**
   * Get the fixity of this binary compared to metadata stored in the repository
   * @param binary the binary resource to get fixity for
   * @return the fixity of this binary compared to metadata stored in the repository
   */
  RdfStream getFixity(Binary binary);

  /**
   * Get the fixity of this binary in a given repository's binary store.
   * @param binary the binary resource to compare
   * @param contentDigest the checksum to compare against
   * @param size the expected size of the binary
   * @return the fixity of the binary
   */
  RdfStream getFixity(Binary binary, URI contentDigest, long size);


  /**
   * Digest this binary with the digest algorithms provided
   * @param binary the binary resource to digest
   * @param algorithms the digest algorithms to be used
   * @return the checksums of this binary
   * @throws org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException if unsupported digest algorithm occurred
   */
  Collection<URI> checkFixity(Binary binary, Collection<String> algorithms) throws UnsupportedAlgorithmException;
}
