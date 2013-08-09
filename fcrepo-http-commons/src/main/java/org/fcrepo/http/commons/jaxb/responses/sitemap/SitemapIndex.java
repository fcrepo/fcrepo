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

package org.fcrepo.http.commons.jaxb.responses.sitemap;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A Sitemap index is a sitemap of other sitemaps
 *
 *  @TODO replace with a 3rd party sitemap impl
 */
@XmlRootElement(name = "sitemapindex",
        namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
@XmlType(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
public class SitemapIndex {

    private final List<SitemapEntry> entries = new ArrayList<SitemapEntry>();

    /**
     * Create a new sitemap index (sitemap-of-sitemaps)
     */
    public SitemapIndex() {

    }

    /**
     * Add an entry to the sitemap
     * 
     * @param e
     */
    public void appendSitemapEntry(final SitemapEntry e) {
        entries.add(e);
    }

    /**
     * Get all the sitemap's entries
     * 
     * @return
     */
    @XmlElement(name = "sitemap",
            namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    public List<SitemapEntry> getSitemapEntries() {
        return entries;
    }
}