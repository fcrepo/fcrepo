package org.fcrepo.integration;

import com.yammer.metrics.Clock;
import com.yammer.metrics.ConsoleReporter;
import com.yammer.metrics.MetricFilter;
import com.yammer.metrics.MetricRegistry;
import org.fcrepo.services.RepositoryService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractIT {


	protected Logger logger;

	@Before
	public void setLogger() {
		logger = LoggerFactory.getLogger(this.getClass());
	}


    @AfterClass
    public static void dumpMetrics() {
        RepositoryService.dumpMetrics(System.out);
    }

}
