package org.fcrepo.merritt.api;

import org.fcrepo.merritt.AbstractMerrittTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrTools;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.modeshape.jcr.api.JcrConstants.*;


public class ContentServiceTest extends AbstractMerrittTest {

    final static protected JcrTools jcrTools = new JcrTools(true);

    @Before
    public void setupFixtureObjects() throws RepositoryException {

        final Session session = repo.login();

        final Node obj = jcrTools.findOrCreateNode(session, "/objects/object_id", "nt:folder");

        final Node ds = jcrTools.findOrCreateNode(session, "/objects/object_id/file_name", NT_FILE);

        final Node contentNode = jcrTools.findOrCreateChild(ds, JCR_CONTENT,
                NT_RESOURCE);

        Property dataProperty = contentNode.setProperty(JCR_DATA, session
                .getValueFactory().createValue("asdfghjkl"));

        session.save();
        session.logout();
    }

    @After
    public void destroyFixtureObjects() throws RepositoryException {

        final Session session = repo.login();

        final Node obj = session.getNode("/objects/object_id");

        obj.remove();

        session.save();
        session.logout();
    }

    @Test
    public void testGetObjectContent() throws Exception {

        getContentForPath("fedora/object_id");
    }

    @Test
    public void testGetObjectVersionContent() throws Exception {

        getContentForPath("fedora/object_id/0");
    }

    @Test
    public void testGetObjectCurrentVersionFileContent() throws Exception {

        getContentForPath("fedora/object_id/file_name");
    }

    @Test
    public void testGetObjectVersionFileContent() throws Exception {
        getContentForPath("fedora/object_id/0/file_name");

    }
}
