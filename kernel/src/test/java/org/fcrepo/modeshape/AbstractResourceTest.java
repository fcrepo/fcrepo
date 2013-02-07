package org.fcrepo.modeshape;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("/spring-test/jetty.xml")
public abstract class AbstractResourceTest extends AbstractTest {

	protected Logger logger;

	protected static final int SERVER_PORT = 8080;
	protected static final String HOSTNAME = "localhost";
	protected static final String serverAddress = "http://" + HOSTNAME + ":"
			+ SERVER_PORT + "/";

	protected final HttpClient client = new HttpClient();

	protected static PostMethod postObjMethod(final String pid) {
		return new PostMethod(serverAddress + "objects/" + pid);
	}

	protected static PostMethod postDSMethod(final String pid, final String ds) {
		return new PostMethod(serverAddress + "objects/" + pid
				+ "/datastreams/" + ds);
	}

	protected static PutMethod putDSMethod(final String pid, final String ds) {
		return new PutMethod(serverAddress + "objects/" + pid + "/datastreams/"
				+ ds);
	}
}
