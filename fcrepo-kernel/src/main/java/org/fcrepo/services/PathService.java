package org.fcrepo.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathService {

    private static final Logger logger = LoggerFactory
            .getLogger(DatastreamService.class);


    public static String getObjectJcrNodePath(String pid) {
        return "/objects/" + pid;
    }

    public static String getDatastreamJcrNodePath(String pid, String dsid) {
        return getObjectJcrNodePath(pid) + "/" + dsid;
    }
}
