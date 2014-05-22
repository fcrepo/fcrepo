/**
 * Copyright 2014 DuraSpace, Inc.
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

import java.net.URI;
import java.util.Calendar;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Entry in a sitemap document
 *
 * @author awoods
 *
 * TODO replace with a 3rd party sitemap impl
 */
@XmlRootElement(name = "url")
public class SitemapEntry {

    @XmlElement
    private final URI loc;

    @XmlElement
    private final Calendar lastmod;

    @XmlElement
    private static final String changefreq = "monthly";

    @XmlElement
    private static final double priority = 0.8;

    /**
     * (default constructor used by JAX-B)
     */
    public SitemapEntry() {
        this(null, null);
    }

    /**
     * Sitemap entry for a URL with a default last modified date
     *
     * @param loc
     */
    public SitemapEntry(final URI loc) {
        this(loc, Calendar.getInstance());
    }

    /**
     * Sitemap entry for a URL with a last modified date
     *
     * @param loc
     * @param lastmod
     */
    public SitemapEntry(final URI loc, final Calendar lastmod) {
        this.loc = loc;
        this.lastmod = lastmod;
    }

}
