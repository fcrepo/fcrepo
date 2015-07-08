/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.http.commons.responses;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openrdf.rio.WriterConfig;
import org.openrdf.rio.helpers.JSONLDMode;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;

import static java.util.Collections.singletonMap;
import static org.fcrepo.http.commons.responses.WriterConfigHelper.PROFILE_COMPACT;
import static org.fcrepo.http.commons.responses.WriterConfigHelper.PROFILE_EXPAND;
import static org.fcrepo.http.commons.responses.WriterConfigHelper.PROFILE_FLATTEN;
import static org.fcrepo.http.commons.responses.WriterConfigHelper.PROFILE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.openrdf.rio.helpers.JSONLDMode.COMPACT;
import static org.openrdf.rio.helpers.JSONLDMode.EXPAND;
import static org.openrdf.rio.helpers.JSONLDMode.FLATTEN;
import static org.openrdf.rio.helpers.JSONLDSettings.JSONLD_MODE;

/**
 * @author awoods
 * @since 7/7/2015.
 */
@RunWith(Parameterized.class)
public class WriterConfigHelperTest {

    @Parameter(value = 0)
    public MediaType mediaType;

    @Parameter(value = 1)
    public JSONLDMode profile;

    @Parameters(name = "{index}: mediaType:{0} => {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new MediaType(null, null), null},
                {new MediaType(null, "ld+json"), null},
                {new MediaType("application", null), null},
                {new MediaType("", ""), null},
                {new MediaType("*", "*"), null},
                {new MediaType("*", "pdf"), null},
                {new MediaType("*", "ld+json"), null},
                {new MediaType("application", "*"), null},
                {new MediaType("application", "pdf"), null},
                {new MediaType("application", "ld+json"), null},
                {new MediaType("application", "ld+json", singletonMap(PROFILE_KEY, PROFILE_COMPACT)), COMPACT},
                {new MediaType("application", "ld+json", singletonMap(PROFILE_KEY, PROFILE_FLATTEN)), FLATTEN},
                {new MediaType("application", "ld+json", singletonMap(PROFILE_KEY, PROFILE_EXPAND)), EXPAND}
        });
    }

    @Test
    public void testApply() throws Exception {
        final WriterConfig config = WriterConfigHelper.apply(mediaType);
        if (null == profile) {
            assertFalse("JSONLD_MODE should not be set!", config.isSet(JSONLD_MODE));
        } else {
            assertEquals("JSONLD_MODE should have been: " + profile, config.get(JSONLD_MODE), profile);
        }
    }
}
