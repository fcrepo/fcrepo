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
    enum preferChoice {
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
