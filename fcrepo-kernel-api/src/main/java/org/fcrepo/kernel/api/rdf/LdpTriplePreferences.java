/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.kernel.api.rdf;

/**
 * Kernel level API to hold the LdpPreferTag and internal logic decisions.
 * @author whikloj
 * @since 6.0.0
 */
public interface LdpTriplePreferences {

    /**
     * What the prefer tag choice is.
     */
    enum PreferChoice {
        INCLUDE,
        EXCLUDE,
        SILENT
    }

    /**
     * @return Whether to display user rdf based on this preference tag and internal defaults.
     */
    boolean displayUserRdf();

    /**
     * @return Whether to display membership triples based on this preference tag and internal defaults.
     */
    boolean displayMembership();

    /**
     * @return Whether to display containment triples based on this preference tag and internal defaults.
     */
    boolean displayContainment();

    /**
     * @return Whether to display inbound reference triples based on this preference tag and internal defaults.
     */
    boolean displayReferences();

    /**
     * @return Whether to display contained resources' triples based on this preference tag and internal defaults.
     */
    boolean displayEmbed();

    /**
     * @return Whether to display server managed triples based on this preference tag and internal defaults.
     */
    boolean displayServerManaged();

}
