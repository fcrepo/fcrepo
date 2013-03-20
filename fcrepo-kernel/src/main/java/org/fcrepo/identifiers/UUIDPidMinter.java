package org.fcrepo.identifiers;

import com.yammer.metrics.MetricRegistry;
import com.yammer.metrics.Timer;
import org.fcrepo.services.RepositoryService;

import static java.util.UUID.randomUUID;

/**
 * Simple PidMinter that replies on Java's inbuilt UUID minting.
 * 
 * @author ajs6f
 *
 */
public class UUIDPidMinter implements PidMinter {

    final static Timer timer = RepositoryService.metrics.timer(MetricRegistry.name(UUIDPidMinter.class, "mint"));

	@Override
	public String mintPid() {


        final Timer.Context context = timer.time();

        try {
            return randomUUID().toString();
        } finally {
            context.stop();
        }
	}

}
