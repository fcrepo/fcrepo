package org.fcrepo.api;


import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.tika.io.IOUtils;

import org.jboss.resteasy.specimpl.PathSegmentImpl;
import org.jboss.resteasy.specimpl.UriInfoImpl;

public abstract class TestHelpers {
    public static UriInfo getUriInfoImpl() {
    	URI baseURI = URI.create("/fcrepo");
    	URI absoluteURI = URI.create("http://localhost/fcrepo");
        URI absolutePath = UriBuilder.fromUri(absoluteURI).replaceQuery(null).build();
        // path must be relative to the application's base uri
  	    URI relativeUri = baseURI.relativize(absoluteURI);
  		
        List<PathSegment> encodedPathSegments = PathSegmentImpl.parseSegments(relativeUri.getRawPath(), false);
        return new UriInfoImpl(absolutePath, baseURI, "/" + relativeUri.getRawPath(), absoluteURI.getRawQuery(), encodedPathSegments);
    }
    
    public static List<Attachment> getStringsAsAttachments(Map<String, String> contents) {
    	List<Attachment> results = new ArrayList<Attachment>(contents.size());
    	for (String id:contents.keySet()) {
    		String content = contents.get(id);
    		InputStream contentStream = IOUtils.toInputStream(content);
    		ContentDisposition cd =
    				new ContentDisposition("form-data;name=" + id + ";filename=" + id + ".txt");
    		Attachment a = new Attachment(id, contentStream, cd);
    		results.add(a);
    	}
    	return results;
    }
}
