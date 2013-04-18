
package org.fcrepo.jaxb.search;

/**
 * A basic object with core properties
 * @author Vincent Nguyen
 */
public class ObjectFields {

    private String pid;

    private String label;

    private String path;

    private String state;

    private String ownerId;

    private String createdDate;

    private String modifiedDate;

    public String getPid() {
        return pid;
    }

    public void setPid(final String pid) {
        this.pid = pid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final String ownerId) {
        this.ownerId = ownerId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final String createdDate) {
        this.createdDate = createdDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(final String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
}
