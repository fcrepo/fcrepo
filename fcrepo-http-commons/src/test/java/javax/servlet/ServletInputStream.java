package javax.servlet;

import java.io.IOException;
import java.io.InputStream;

public class ServletInputStream extends InputStream {

	@Override
	public int read() throws IOException {
		return 0;
	}

}
