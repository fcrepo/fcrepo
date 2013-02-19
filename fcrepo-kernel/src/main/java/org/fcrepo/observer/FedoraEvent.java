
package org.fcrepo.observer;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

public class FedoraEvent implements Event {

    Event e;

    public FedoraEvent(Event e) {
        this.e = e;
    }

    @Override
    public int getType() {
        return e.getType();
    }

    @Override
    public String getPath() throws RepositoryException {
        return e.getPath();
    }

    @Override
    public String getUserID() {
        return e.getUserID();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return e.getIdentifier();
    }

    @Override
    public Map getInfo() throws RepositoryException {
        return null;
    }

    @Override
    public String getUserData() throws RepositoryException {
        return e.getUserData();
    }

    @Override
    public long getDate() throws RepositoryException {
        return e.getDate();
    }

}
