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

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.WriterConfig;
import org.openrdf.rio.helpers.JSONLDMode;
import org.openrdf.rio.helpers.JSONLDSettings;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class creates {@link org.openrdf.rio.WriterConfig}s based on provided {@link javax.ws.rs.core.MediaType}s
 *
 * @author awoods
 * @since 7/7/2015.
 */
public class WriterConfigHelper {

    private static final Logger LOGGER = getLogger(WriterConfigHelper.class);

    public static final String PROFILE_KEY = "profile";
    public static final String PROFILE_COMPACT = "http://www.w3.org/ns/json-ld#compacted";
    public static final String PROFILE_FLATTEN = "http://www.w3.org/ns/json-ld#flattened";
    public static final String PROFILE_EXPAND = "http://www.w3.org/ns/json-ld#expanded";

    private WriterConfigHelper() {
        // no public constructor
    }

    /**
     * This method returns a {@link org.openrdf.rio.WriterConfig} based on the arg {@link javax.ws.rs.core.MediaType}
     *
     * @param mediaType that will serve as the basis for the returned WriterConfig
     * @return default or configured WriterConfig
     */
    public static WriterConfig apply(final MediaType mediaType) {
        final WriterConfig config = new WriterConfig();
        if (!mediaType.isCompatible(MediaType.valueOf(RDFFormat.JSONLD.getDefaultMIMEType()))) {
            return config;
        }

        final Map<String, String> params = mediaType.getParameters();
        if (null != params && params.containsKey(PROFILE_KEY)) {
            final String profile = params.get(PROFILE_KEY);
            switch (profile) {
                case PROFILE_COMPACT:
                    config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
                    break;
                case PROFILE_FLATTEN:
                    config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.FLATTEN);
                    break;
                case PROFILE_EXPAND:
                    config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.EXPAND);
                    break;
                default:
                    LOGGER.debug("No profile found in {}", mediaType);
            }
        }

        return config;
    }
}
