/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration;

import static org.slf4j.LoggerFactory.getLogger;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @todo Add Documentation.
 * @author fasseg
 * @date Mar 20, 2013
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractIT {

    protected Logger logger;

    /**
     * @todo Add Documentation.
     */
    @Before
    public void setLogger() {
        logger = getLogger(this.getClass());
    }

}
