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

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.atom.Category;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;

/**
 * Create and parse ATOM events
 */
public abstract class EntryFactory {

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
        final Entry entry = new Entry();

        //not used:
        //entry.declareNS(XSD_NS, "xsd");
        //entry.declareNS(TYPES_NS, "fedora-types");

        entry.setId("urn:uuid:" + UUID.randomUUID().toString());

        List<Category> categoryList = new ArrayList<Category>();
        Category formatCategory = new Category();
        formatCategory.setLabel("format");
        formatCategory.setScheme(FORMAT_PREDICATE);
        formatCategory.setTerm(FORMAT);
        categoryList.add(formatCategory);
        Category versionCategory = new Category();
        versionCategory.setLabel("version");
        versionCategory.setScheme(VERSION_PREDICATE);
        versionCategory.setTerm(SERVER_VERSION);
        categoryList.add(versionCategory);
        entry.setCategories(categoryList);
        //entry.addCategory(FORMAT_PREDICATE, FORMAT, "format");
        //entry.addCategory(VERSION_PREDICATE, SERVER_VERSION, "version");

        return entry;
    }

    /**
     * Parse an ATOM entry document into ROME
     * @param input
     * @return
     * @throws IOException
     * @throws FeedException
     * @throws IllegalArgumentException
     */
    static Entry parse(Reader input) throws IllegalArgumentException,
            FeedException, IOException {
        WireFeedInput wireFeedInput = new WireFeedInput();
        WireFeed wiredFeed = wireFeedInput.build(input);
        Feed f = (Feed)wiredFeed;
        //TODO return List<Entry>
        return (Entry) f.getEntries().get(0);
    }

}
