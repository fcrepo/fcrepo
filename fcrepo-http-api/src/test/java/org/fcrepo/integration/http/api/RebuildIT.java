/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.integration.http.api;


import edu.wisc.library.ocfl.api.OcflRepository;
import org.fcrepo.kernel.impl.operations.RdfSourceOperationFactoryImpl;
import org.fcrepo.persistence.ocfl.RepositoryInitializer;
import org.fcrepo.persistence.ocfl.impl.DefaultOCFLObjectSessionFactory;
import org.fcrepo.persistence.ocfl.impl.FedoraToOCFLObjectIndexImpl;
import org.fcrepo.persistence.ocfl.impl.FedoraToOCFLObjectIndexUtilImpl;
import org.fcrepo.persistence.ocfl.impl.OCFLPersistenceConfig;
import org.fcrepo.persistence.ocfl.impl.OCFLPersistentSessionManager;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static org.fcrepo.persistence.ocfl.impl.OCFLConstants.OCFL_STORAGE_ROOT_DIR_KEY;
import static org.fcrepo.persistence.ocfl.impl.OCFLConstants.OCFL_WORK_DIR_KEY;

/**
 * @author awooods
 * @since 2020-03-04
 */
public class RebuildIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildIT.class);

    private OcflRepository ocflRepository;

    private static String origStorageRootDir;
    private static String origWorkDir;

    @BeforeClass
    public static void beforeClass() {
        // Save the pre-test System Property values
        origStorageRootDir = getProperty(OCFL_STORAGE_ROOT_DIR_KEY);
        origWorkDir = getProperty(OCFL_WORK_DIR_KEY);

        setProperty(OCFL_STORAGE_ROOT_DIR_KEY, "target/test-classes/test-rebuild-ocfl/ocfl-root");
        setProperty(OCFL_WORK_DIR_KEY, "target/test-classes/test-rebuild-ocfl/ocfl-work");
    }

    @AfterClass
    public static void afterClass() {
        // Restore pre-test System Property values
        setProperty(OCFL_STORAGE_ROOT_DIR_KEY, origStorageRootDir);
        setProperty(OCFL_WORK_DIR_KEY, origWorkDir);
    }

    @Before
    public void setUp() {
        final AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(OCFLPersistenceConfig.class);

        // RepositoryInitializer.initialize() happens as a part of the object's construction.
        ctx.register(RepositoryInitializer.class,
                OCFLPersistentSessionManager.class,
                RdfSourceOperationFactoryImpl.class,
                FedoraToOCFLObjectIndexUtilImpl.class,
                DefaultOCFLObjectSessionFactory.class,
                OCFLPersistenceConfig.class,
                FedoraToOCFLObjectIndexImpl.class);

        ocflRepository = ctx.getBean(OcflRepository.class);
    }

    /**
     * This test rebuilds from a knows set of OCFL content.
     * The OCFL storage root contains the following four resources:
     * - binary
     * - test
     * - test_child
     *
     * The test verifies that these objects exist in the rebuilt repository.
     */
    @Test
    public void testRebuild() {

        // Optional debugging
        if (LOGGER.isDebugEnabled()) {
            ocflRepository.listObjectIds().forEach(id -> LOGGER.debug("Object id: {}", id));
        }

        Assert.assertEquals(4, ocflRepository.listObjectIds().count());
        Assert.assertTrue("Should contain object with id: binary", ocflRepository.containsObject("binary"));
        Assert.assertTrue("Should contain object with id: test", ocflRepository.containsObject("test"));
        Assert.assertTrue("Should contain object with id: test_child", ocflRepository.containsObject("test_child"));
        Assert.assertFalse("Should NOT contain object with id: junk", ocflRepository.containsObject("junk"));
    }

}
