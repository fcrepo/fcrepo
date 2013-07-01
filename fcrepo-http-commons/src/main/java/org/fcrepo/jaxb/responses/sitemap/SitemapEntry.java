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

import java.net.URI;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Entry in a sitemap document
 *
 *  @TODO replace with a 3rd party sitemap impl
 */
@XmlRootElement(name = "url",
        namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
public class SitemapEntry {

    @XmlElement(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    private final URI loc;

    @XmlElement(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    private final Calendar lastmod;

    @XmlElement(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    private static final String changefreq = "monthly";

    @XmlElement(namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    private static final double priority = 0.8;

    /**
     * (default constructor used by JAX-B)
     */
    public SitemapEntry() throws RepositoryException {
        this(null, null);
    }

    /**
     * Sitemap entry for a URL with a default last modified date
     * 
     * @param loc
     * @throws RepositoryException
     */
    public SitemapEntry(final URI loc) throws RepositoryException {
        this(loc, Calendar.getInstance());
    }

    /**
     * Sitemap entry for a URL with a last modified date
     * 
     * @param loc
     * @param lastmod
     * @throws RepositoryException
     */
    public SitemapEntry(final URI loc, final Calendar lastmod)
        throws RepositoryException {
        this.loc = loc;
        this.lastmod = lastmod;
    }

}