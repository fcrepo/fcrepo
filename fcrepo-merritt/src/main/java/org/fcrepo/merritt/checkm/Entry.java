package org.fcrepo.merritt.checkm;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class Entry {
        final public String fileUrl;
        final public String hashAlgorithm;
        final public String hashValue;
        final public String fileSize;
        final public String fileLastModified;
        final public String fileName;


        protected static HttpClient client;
        protected static final PoolingClientConnectionManager connectionManager =
                new PoolingClientConnectionManager();

        static {
            connectionManager.setMaxTotal(Integer.MAX_VALUE);
            connectionManager.setDefaultMaxPerRoute(5);
            connectionManager.closeIdleConnections(3, TimeUnit.SECONDS);
            client = new DefaultHttpClient(connectionManager);
        }


        public Entry(final String fileUrl, final String hashAlgorithm, final String hashValue, final String fileSize, final String _, final String fileName) {
            this.fileUrl = fileUrl;
            this.hashAlgorithm = hashAlgorithm;
            this.hashValue = hashValue;
            this.fileSize = fileSize;
            this.fileLastModified = null;
            this.fileName = fileName;
        }

        public Entry(String[] row) {
            this(row[0].trim(), row[1].trim(), row[2].trim(), row[3].trim(), row[4].trim(), row[5].trim());
        }

        public InputStream getSourceInputStream() throws IOException {
            final HttpGet manifestRequest =
                    new HttpGet(fileUrl);

            HttpResponse response = client.execute(manifestRequest);

            return response.getEntity().getContent();
        }

}