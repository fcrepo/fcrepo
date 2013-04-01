package org.fcrepo.provider;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.fcrepo.jaxb.responses.access.DescribeRepository;
import org.fcrepo.jaxb.search.FieldSearchResult;
import org.slf4j.Logger;

/**
 * Resolves the view to be used
 * @author Vincent Nguyen
 *
 */
public class VelocityViewer {
	
	private VelocityEngine velocityEngine;

    private static final Logger logger = getLogger(VelocityViewer.class);


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
            logger.warn("Exception rendering Velocity template: {}", e);
        }
	}
	
	public String getRepoInfo(DescribeRepository repoinfo) {
		try {
			// Build a context to hold the model
	        VelocityContext velocityContext = new VelocityContext();
	        velocityContext.put("repo", repoinfo);
	        
	        // Execute the template
	        StringWriter writer = new StringWriter();
	        velocityEngine.mergeTemplate( "views/repo-info.vm", "utf-8", velocityContext, writer );
	
	        // Return the result
	        return writer.toString();
		}
	    catch( Exception e )
	    {
            logger.warn("Exception rendering Velocity template: {}", e);
	    }
		return null;
	}

    public String getFieldSearch(FieldSearchResult results) {
        try {
            // Build a context to hold the model
            VelocityContext velocityContext = new VelocityContext();
            velocityContext.put("results", results);

            // Execute the template
            StringWriter writer = new StringWriter();
            velocityEngine.mergeTemplate( "views/search-results-form.vm", "utf-8", velocityContext, writer );

            // Return the result
            return writer.toString();
        }
        catch( Exception e )
        {
            logger.warn("Exception rendering Velocity template: {}", e);
        }
        return null;
    }


}
