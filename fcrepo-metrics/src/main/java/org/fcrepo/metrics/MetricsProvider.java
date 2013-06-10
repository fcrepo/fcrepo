package org.fcrepo.metrics;

import javax.ws.rs.ext.Provider;

import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;

@Provider
public class MetricsProvider extends InstrumentedResourceMethodDispatchAdapter {

    public MetricsProvider() {
        super(RegistryService.getMetrics());
    }
}
