package org.fcrepo.jaxb.responses.sitemap;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "sitemapindex", namespace = "http://www.sitemaps.org/schemas/sitemap/0.9")
public class SitemapIndex {

    private List<SitemapEntry> entries = new ArrayList<SitemapEntry>();
    public SitemapIndex() {

    }

    public void appendSitemapEntry(SitemapEntry e) {
        entries.add(e);
    }

    @XmlElement(name = "sitemap")
    public List<SitemapEntry> getSitemapEntries() {
        return entries;
    }
}