/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.http.commons;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContextEvent;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class wraps the standard Spring ContextLoaderListener in order to catch initialization errors.
 * 
 * @author awoods
 * @since 2016-06-09
 */
public class FedoraContextLoaderListener extends ContextLoaderListener {

    private static final org.slf4j.Logger LOGGER = getLogger(FedoraContextLoaderListener.class);

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        try {
            super.contextInitialized(event);
        } catch (final BeanDefinitionStoreException e) {
            final String msg = "\n" +
                    "=====================================================================\n" +
                    "=====================================================================\n" +
                    "---------- FEDORA CONFIGURATION ERROR ----------\n" +
                    "\n" +
                    "See documentation specific to your version of Fedora\n" +
                    "https://wiki.lyrasis.org/display/FEDORA6x/Application+Configuration\n" +
                    "\n" +
                    "=====================================================================\n" +
                    "=====================================================================\n";
            LOGGER.error(msg);
        }

    }

}
