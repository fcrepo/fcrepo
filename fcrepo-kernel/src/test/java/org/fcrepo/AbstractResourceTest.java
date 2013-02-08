package org.fcrepo;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("/spring-test/master.xml")
public abstract class AbstractResourceTest extends AbstractTest {

	protected static final int SERVER_PORT = Integer.parseInt(System.getProperty("test.port", "8080"));
	protected static final String HOSTNAME = "localhost";
	protected static final String serverAddress = "http://" + HOSTNAME + ":"
			+ SERVER_PORT + "/rest/";

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
