
package org.fcrepo.jaxb.search;

import java.util.List;

/**
 * A search results object returned from a FedoraFieldSearch
 * @author Vincent Nguyen
 */
public class FieldSearchResult {

    private final List<ObjectFields> objectFieldsList;

    private int start;

    private int maxResults;

    private final int size;

    private String searchTerms;

    public FieldSearchResult(final List<ObjectFields> objectFieldsList,
            final int start, final int maxResults, final int size) {
        this.objectFieldsList = objectFieldsList;
        this.start = start;
        this.maxResults = maxResults;
        this.size = size;
    }

    public List<ObjectFields> getObjectFieldsList() {
        return objectFieldsList;
    }

    public int getStart() {
        return start;
    }

    public void setStart(final int start) {
        this.start = start;
    }

    public void setMaxResults(final int maxResults) {
        this.maxResults = maxResults;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public final int getSize() {
        return size;
    }

    public String getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(final String searchTerms) {
        this.searchTerms = searchTerms;
    }
}
