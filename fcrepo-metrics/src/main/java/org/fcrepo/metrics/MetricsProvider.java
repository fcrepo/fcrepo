
package org.fcrepo.metrics;

import javax.ws.rs.ext.Provider;

import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;

@Provider
public class MetricsProvider extends InstrumentedResourceMethodDispatchAdapter {

    /**
     * Default constructor that provides a MetricsRegistry
     */
    public MetricsProvider() {
        super(RegistryService.getMetrics());
    }
}
