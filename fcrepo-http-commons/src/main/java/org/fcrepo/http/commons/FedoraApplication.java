/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons;

import org.fcrepo.http.commons.metrics.MicrometerFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.logging.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author cabeer
 * @since 9/22/14
 */
public class FedoraApplication extends ResourceConfig {

    private static final org.slf4j.Logger LOGGER = getLogger(FedoraApplication.class);

    /**
     * THIS IS OUR RESOURCE CONFIG!
     */
    public FedoraApplication() {
        super();
        packages("org.fcrepo");
        register(MultiPartFeature.class);
        register(JacksonFeature.class);
        register(MicrometerFeature.class);

        if (LOGGER.isDebugEnabled()) {
            register(new LoggingFeature(Logger.getLogger(LoggingFeature.class.getName())));
        }
    }
}
