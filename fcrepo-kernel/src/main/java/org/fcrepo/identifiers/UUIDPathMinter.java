package org.fcrepo.identifiers;

import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.UUID.randomUUID;
import static org.fcrepo.metrics.RegistryService.getMetrics;

/**
 * PID minter that creates hierarchical IDs for a UUID
 */
public class UUIDPathMinter extends BasePidMinter {

    static final Timer timer = getMetrics().timer(name(UUIDPathMinter.class, "mint"));


    /**
     * Mint a unique identifier as a UUID
     * @return
     */
    @Override
    public String mintPid() {

        final Timer.Context context = timer.time();

        try {
            final String s = randomUUID().toString();
            final Iterable<String> split = Splitter.fixedLength(2).split(s.substring(0, 8));

            return Joiner.on("/").join(split) + "/" + s;
        } finally {
            context.stop();
        }
    }
}
