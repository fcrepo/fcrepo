package org.fcrepo.jaxb.responses.sitemap;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "urlset", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
public class SitemapUrlSet {


    @XmlElement(name = "url", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
    private List<SitemapEntry> sitemapEntries = new ArrayList<SitemapEntry>();

    public SitemapUrlSet() {

    }

    public void appendSitemapEntry(SitemapEntry e) {
        sitemapEntries.add(e);
    }
    public List<SitemapEntry> getSitemapEntries() {
        return sitemapEntries;
    }
}