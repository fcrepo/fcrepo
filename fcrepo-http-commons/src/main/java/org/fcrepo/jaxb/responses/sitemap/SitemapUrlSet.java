
package org.fcrepo.jaxb.responses.sitemap;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "urlset", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
public class SitemapUrlSet {

    @XmlElement(name = "url", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    private final List<SitemapEntry> sitemapEntries =
            new ArrayList<SitemapEntry>();

    public SitemapUrlSet() {

    }

    public void appendSitemapEntry(final SitemapEntry e) {
        sitemapEntries.add(e);
    }

    public List<SitemapEntry> getSitemapEntries() {
        return sitemapEntries;
    }
}