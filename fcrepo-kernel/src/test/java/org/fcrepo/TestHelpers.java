package org.fcrepo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Date;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.fcrepo.utils.ContentDigest;
import org.fcrepo.utils.DatastreamIterator;
import org.modeshape.jcr.api.Repository;

public class TestHelpers {
    public static DatastreamIterator mockDatastreamIterator(String pid, String dsId, String content) throws RepositoryException, IOException {
    	DatastreamIterator mockIter = mock(DatastreamIterator.class);
    	Datastream mockDs = mockDatastream(pid, dsId, content);
        when(mockIter.hasNext()).thenReturn(true, false);
        when(mockIter.next()).thenReturn(mockDs);
        when(mockIter.nextDatastream()).thenReturn(mockDs);
        return mockIter;
    }
    
    public static Repository mockRepository() throws LoginException, RepositoryException {
    	Repository mockRepo = mock(Repository.class);
    	Session mockSession = mock(Session.class);
    	when(mockRepo.login()).thenReturn(mockSession);
    	return mockRepo;
    }

	public static Datastream mockDatastream(String pid, String dsId, String content) {
		Datastream mockDs = mock(Datastream.class);
		FedoraObject mockObj = mock(FedoraObject.class);
		try{
			when(mockObj.getName()).thenReturn(pid);
			when(mockDs.getObject()).thenReturn(mockObj);
			when(mockDs.getDsId()).thenReturn(dsId);
	    	when(mockDs.getMimeType()).thenReturn("application/octet-stream");
			when(mockDs.getCreatedDate()).thenReturn(new Date());
			when(mockDs.getLastModifiedDate()).thenReturn(new Date());
			if (content != null) {
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				byte [] digest = md.digest(content.getBytes());
				URI cd = ContentDigest.asURI("SHA-1", digest);
				when(mockDs.getContent()).thenReturn(IOUtils.toInputStream(content));
				when(mockDs.getContentDigest()).thenReturn(cd);
				when(mockDs.getContentDigestType()).thenReturn("SHA-1");
			}
		} catch (Throwable t) {}
		return mockDs;
	}
    
}
