/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.services;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.UnsupportedAlgorithmException;
import org.fcrepo.kernel.api.models.Binary;

import java.net.URI;
import java.util.Collection;

/**
 * Service which calculates and compares digests for binary objects
 *
 * @author peichman
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
   * @return The result of the fixity check.
   */
  RdfStream checkFixity(Binary binary);
}
