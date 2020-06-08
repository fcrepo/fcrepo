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

import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.models.Binary;

import java.net.URI;
import java.util.Collection;

/**
 * Service which calculates and compares digests for binary objects
 */
public interface FixityService {
  /**
   * Calculate the requested set of digests for the provided binary
   * @param binary the binary resource to
   * @param algorithms set of digest algorithms to calculate
   * @return list of calculated digests
   * @throws UnsupportedAlgorithmException if unsupported digest algorithms were provided
   */
  Collection<URI> getFixity(Binary binary, Collection<String> algorithms) throws UnsupportedAlgorithmException;

  /**
   * Digest this binary with the digest algorithms provided
   * @param binary the binary resource to digest
   * @param algorithms the digest algorithms to be used
   * @return the checksums of this binary
   * @throws org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException if unsupported digest algorithm occurred
   */
  Collection<URI> checkFixity(Binary binary, Collection<String> algorithms) throws UnsupportedAlgorithmException;
}
