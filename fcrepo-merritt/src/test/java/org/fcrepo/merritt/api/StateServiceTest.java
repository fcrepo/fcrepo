package org.fcrepo.merritt.api;

import org.fcrepo.merritt.AbstractMerrittTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.api.JcrTools;

import javax.jcr.*;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.*;


public class StateServiceTest extends AbstractMerrittTest {

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
    public void testHelpService() throws Exception {

        final String state_text = getStateForPath("");

        assertTrue("Didn't find the service state content!", compile(
                "serviceState", DOTALL).matcher(state_text).find());
    }

    @Test
    public void testNodeStateService() throws Exception {
        final String state_text = getStateForPath("fedora");

        assertTrue("Didn't find the node state content!", compile(
                "nodeState", DOTALL).matcher(state_text).find());

    }

    @Test
    public void testObjectStateService() throws Exception {
        final String state_text = getStateForPath("fedora/object_id");

        assertTrue("Didn't find the object state content!", compile(
                "objectState", DOTALL).matcher(state_text).find());

    }

    @Test
    public void testVersionStateService() throws Exception {
        final String state_text = getStateForPath("fedora/object_id/0");

        assertTrue("Didn't find the version state content!", compile(
                "versionState", DOTALL).matcher(state_text).find());

    }

    @Test
    public void testVersionFileStateService() throws Exception {
        final String state_text = getStateForPath("fedora/object_id/0/file_name");

        assertTrue("Didn't find the file state content!", compile(
                "fileState", DOTALL).matcher(state_text).find());

    }

    @Test
    public void testCurrentVersionFileStateService() throws Exception {
        final String state_text = getStateForPath("fedora/object_id/file_name");

        assertTrue("Didn't find the file state content!", compile(
                "fileState", DOTALL).matcher(state_text).find());

    }
}
