package org.fcrepo.integration;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.reporting.ConsoleReporter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractIT {


	protected Logger logger;

	@Before
	public void setLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}


    @AfterClass
    public static void dumpMetrics() {
        final ConsoleReporter reporter = new ConsoleReporter(Metrics.defaultRegistry(),
                System.out,
                MetricPredicate.ALL);

        reporter.run();
    }

}
