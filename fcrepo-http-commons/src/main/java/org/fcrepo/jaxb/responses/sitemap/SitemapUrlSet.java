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

package org.fcrepo.jaxb.responses.sitemap;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Suspiciously similar to {@link SitemapIndex}, a sitemap of entries.
 *
 * @TODO replace with a 3rd party sitemap impl
 */
@XmlRootElement(name = "urlset",
        namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
public class SitemapUrlSet {

    private final List<SitemapEntry> sitemapEntries =
            new ArrayList<SitemapEntry>();

    /**
     * Create a new sitemap with the default settings
     */
    public SitemapUrlSet() {

    }

    /**
     * Add an entry to the sitemap
     * 
     * @param e
     */
    public void appendSitemapEntry(final SitemapEntry e) {
        sitemapEntries.add(e);
    }

    /**
     * Get all the sitemap entries
     * 
     * @return
     */
    @XmlElement(name = "url",
             namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    public List<SitemapEntry> getSitemapEntries() {
        return sitemapEntries;
    }
}