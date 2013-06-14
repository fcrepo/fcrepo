package org.fcrepo.integration.api;

import com.hp.hpl.jena.update.GraphStore;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.fcrepo.test.util.TestHelpers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class FedoraWorkspacesIT extends AbstractResourceIT {

    @Test
    public void shouldDemonstratePathsAndWorkspaces() throws IOException, RepositoryException {
        final HttpPost httpCreateWorkspace = new HttpPost(serverAddress + "fcr:workspaces/some-workspace");
        final HttpResponse createWorkspaceResponse = execute(httpCreateWorkspace);
        assertEquals(201, createWorkspaceResponse.getStatusLine().getStatusCode());


        final HttpPost httpPost = new HttpPost(serverAddress + "workspace:some-workspace/FedoraWorkspacesTest");
        final HttpResponse response = execute(httpPost);
        assertEquals(201, response.getStatusLine().getStatusCode());


        final HttpGet httpGet = new HttpGet(serverAddress + "workspace:some-workspace/FedoraWorkspacesTest");
        final HttpResponse profileResponse = execute(httpGet);
        assertEquals(200, profileResponse.getStatusLine().getStatusCode());
        final GraphStore graphStore = TestHelpers.parseTriples(profileResponse.getEntity().getContent());
        logger.info(graphStore.toString());
    }
}
