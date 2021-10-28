/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.config;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Digest Algorith enum
 * @author cbeer
 *
 * Moved to its own class for Fedora 6.0.0
 */
public enum DigestAlgorithm {
    SHA1("SHA", "urn:sha1", "sha-1", "sha1"),
    SHA256("SHA-256", "urn:sha-256", "sha256"),
    SHA512("SHA-512", "urn:sha-512", "sha512"),
    SHA512256("SHA-512/256", "urn:sha-512/256", "sha512/256"),
    MD5("MD5", "urn:md5"),
    MISSING("NONE", "missing");

    final private String algorithm;
    final private String scheme;
    final private Set<String> aliases;

    DigestAlgorithm(final String alg, final String scheme, final String... aliases) {
        this.algorithm = alg;
        this.scheme = scheme;
        this.aliases = Stream.concat(Arrays.stream(aliases), Stream.of(algorithm)).map(String::valueOf)
                        .map(String::toLowerCase).collect(toSet());
    }

    /**
     * Return the scheme associated with the provided algorithm (e.g. SHA-1 returns urn:sha1)
     *
     * @param alg for which scheme is requested
     * @return scheme
     */
    public static String getScheme(final String alg) {
        return Arrays.stream(values()).filter(value ->
                value.algorithm.equalsIgnoreCase(alg) || value.algorithm.replace("-", "").equalsIgnoreCase(alg)
        ).findFirst().orElse(MISSING).scheme;
    }

    /**
     * Return enum value for the provided scheme (e.g. urn:sha1 returns SHA-1)
     *
     * @param argScheme for which enum is requested
     * @return enum value associated with the arg scheme
     */
    public static DigestAlgorithm fromScheme(final String argScheme) {
        return Arrays.stream(values()).filter(value -> value.scheme.equalsIgnoreCase(argScheme)
        ).findFirst().orElse(MISSING);
    }

    /**
     * Return enum value for the provided algorithm
     *
     * @param alg algorithm name to seek
     * @return enum value associated with the algorithm name, or missing if not found
     */
    public static DigestAlgorithm fromAlgorithm(final String alg) {
        final String seek = alg.toLowerCase();
        return Arrays.stream(values())
                .filter(value -> value.aliases.contains(seek))
                .findFirst()
                .orElse(MISSING);
    }

    /**
     * Return true if the provided algorithm is included in this enum
     *
     * @param alg to test
     * @return true if arg algorithm is supported
     */
    public static boolean isSupportedAlgorithm(final String alg) {
        return !getScheme(alg).equals(MISSING.scheme);
    }

    /**
     * @return the aliases
     */
    public Set<String> getAliases() {
        return aliases;
    }

    /**
     * @return the algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }
}
