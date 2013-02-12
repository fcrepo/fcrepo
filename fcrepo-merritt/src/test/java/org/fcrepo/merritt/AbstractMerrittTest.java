package org.fcrepo.merritt;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "/spring-test/merritt.xml", "/spring-test/repo.xml"})
public abstract class AbstractMerrittTest {

    protected static final int SERVER_PORT = 8080;

    protected static final String HOSTNAME = "localhost";

    protected static final String serverAddress = "http://" + HOSTNAME + ":" +
            SERVER_PORT + "/";

    protected final HttpClient client = new HttpClient();

    protected static Logger logger;

    /**
     * The JCR repository at the heart of Fedora.
     */
    @Inject
    protected Repository repo;

    @BeforeClass
    public static void setLogger() {
        logger = LoggerFactory.getLogger(AbstractMerrittTest.class);
    }

    protected String getStateForPath(String path) throws IOException {
        GetMethod getMerritState = new GetMethod(serverAddress
                + "state/" + path);

        int status = client.executeMethod(getMerritState);

        assertEquals(200, status);

        final String state_text = getMerritState.getResponseBodyAsString();
        logger.debug("Found the state info:\n" + state_text);

        return state_text;
    }

    protected GetMethod getContentForPath(String path) throws IOException {
        GetMethod getMerritState = new GetMethod(serverAddress
                + "content/" + path);

        int status = client.executeMethod(getMerritState);

        assertEquals(200, status);

        return getMerritState;
    }


    protected GetMethod getBagitPath(String path) throws IOException {
        GetMethod getBagitRequest = new GetMethod(serverAddress
                + "bagit/" + path);

        int status = client.executeMethod(getBagitRequest);

        assertEquals(200, status);

        return getBagitRequest;
    }
}
