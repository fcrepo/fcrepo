package org.fcrepo.integration;

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

}
