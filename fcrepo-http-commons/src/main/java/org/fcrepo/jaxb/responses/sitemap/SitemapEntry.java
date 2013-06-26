
package org.fcrepo.jaxb.responses.sitemap;

import java.net.URI;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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

    public SitemapEntry() {
        loc = null;
        lastmod = null;
    }

    public SitemapEntry(final URI loc) throws RepositoryException {
        this.loc = loc;
        lastmod = Calendar.getInstance();
    }

    public SitemapEntry(final URI loc, final Calendar lastmod)
            throws RepositoryException {
        this.loc = loc;
        this.lastmod = lastmod;
    }

}