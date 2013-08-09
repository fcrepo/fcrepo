/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.jms.legacy;

import java.io.Reader;
import java.util.UUID;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;

/**
 * Create and parse ATOM events
 */
public abstract class EntryFactory {

    private static final Abdera ABDERA = new Abdera();

    private static final Parser ABDERA_PARSER = ABDERA.getParser();

    public static final String FORMAT =
            "info:fedora/fedora-system:ATOM-APIM-1.0";

    // TODO get this out of the build properties
    public static final String SERVER_VERSION = "4.0.0-SNAPSHOT";

    private static final String TYPES_NS =
            "http://www.fedora.info/definitions/1/0/types/";

    public static final String VERSION_PREDICATE =
            "info:fedora/fedora-system:def/view#version";

    private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    public static final String FORMAT_PREDICATE =
            "http://www.fedora.info/definitions/1/0/types/formatURI";

    /**
     * Create a new base Abdera document for our ATOM entry
     * @return
     */
    static Entry newEntry() {
        final Entry entry = ABDERA.newEntry();
        entry.declareNS(XSD_NS, "xsd");
        entry.declareNS(TYPES_NS, "fedora-types");
        entry.setId("urn:uuid:" + UUID.randomUUID().toString());
        entry.addCategory(FORMAT_PREDICATE, FORMAT, "format");
        entry.addCategory(VERSION_PREDICATE, SERVER_VERSION, "version");
        return entry;
    }

    /**
     * Parse an ATOM entry document into Abdera
     * @param input
     * @return
     */
    static Entry parse(Reader input) {
        return (Entry) ABDERA_PARSER.parse(input).getRoot();
    }

}
