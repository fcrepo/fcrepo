package org.fcrepo.merritt.api;

import org.apache.commons.httpclient.methods.GetMethod;
import org.fcrepo.merritt.AbstractMerrittTest;
import org.junit.Test;

import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HelpTest extends AbstractMerrittTest {

    @Test
    public void testHelpService() throws Exception {

        GetMethod getMerritHelpService = new GetMethod(serverAddress
                + "help");

        int status = client.executeMethod(getMerritHelpService);

        assertEquals(200, status);

        final String help_text = getMerritHelpService.getResponseBodyAsString();
        logger.debug("Found the repository help:\n" + help_text);
        assertTrue(
                "Failed to find repository help",
                compile("I am a help service!").matcher(
                        help_text).find());

    }
}
