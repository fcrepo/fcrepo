package org.fcrepo.provider;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.fcrepo.jaxb.responses.access.DescribeRepository;

/**
 * Resolves the view to be used
 * @author Vincent Nguyen
 *
 */
public class VelocityViewer {
	
	private VelocityEngine velocityEngine;
	
	public VelocityViewer() {
		try
        {
            // Load the velocity properties from the class path
            Properties properties = new Properties();
            properties.load( getClass().getClassLoader().getResourceAsStream( "velocity.properties" ) );

            // Create and initialize the template engine
            velocityEngine = new VelocityEngine( properties );
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
	}
	
	public String getRepoInfo(DescribeRepository repoinfo) {
		return getViewer("repo-info.vm", "repo", repoinfo);
	}
	
	public String getViewer(String template, String key, Object value) {
		try {
			// Build a context to hold the model
	        VelocityContext velocityContext = new VelocityContext();
	        velocityContext.put(key, value);
	        
	        // Execute the template
	        StringWriter writer = new StringWriter();
	        velocityEngine.mergeTemplate( "views/" + template, "utf-8", velocityContext, writer );
	
	        // Return the result
	        return writer.toString();
		}
	    catch( Exception e )
	    {
	        e.printStackTrace();
	    }
		return null;
	}
}
