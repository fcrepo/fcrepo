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
 * Kernel level API to hold the LdpPreferTag decisions.
 * @author whikloj
 * @since 6.0.0
 */
public interface LdpTriplePreferences {

    /**
     * What the prefer tag choice is.
     */
    enum preferChoice {
        INCLUDE("Include"),
        EXCLUDE("Exclude"),
        SILENT("Silent");

        private String value;

        preferChoice(final String value) {
            this.value = value;
        }
    }

    /**
     * @return Whether this preference tag asks to include or omit a minimal container or is silent.
     */
    preferChoice preferMinimal();

    /**
     * @return Whether this preference tag asks to include or omit membership triples or is silent.
     */
    preferChoice prefersMembership();

    /**
     * @return Whether this preference tag asks to include or omit containment triples or is silent.
     */
    preferChoice prefersContainment();

    /**
     * @return Whether this preference tag asks to include or omit reference triples or is silent.
     */
    preferChoice prefersReferences();

    /**
     * @return Whether this preference tag asks to include or omit embedded triples or is silent.
     */
    preferChoice prefersEmbed();

    /**
     * @return Whether this preference tag asks to include or omit server managed triples or is silent.
     */
    preferChoice prefersServerManaged();

}
