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
package org.fcrepo.kernel.impl.services.functions;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import javax.jcr.Repository;

import org.infinispan.manager.DefaultCacheManager;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.value.binary.infinispan.InfinispanBinaryStore;
import org.slf4j.Logger;

/**
 * Extract the Infinispan cluster configuration and state
 * from a running Modeshape repository
 * @author Gregory Jansen
 * @since Apr 26, 2013
 */
public class GetClusterConfiguration implements Function<Repository, Map<String, String>> {

    private static final Logger LOGGER = getLogger(GetClusterConfiguration.class);

    public static final String CLUSTER_NAME = "clusterName";
    public static final String CACHE_MODE = "clusterCacheMode";
    public static final String NODE_ADDRESS = "clusterNodeAddress";
    public static final String PHYSICAL_ADDRESS = "clusterPhysicalAddress";
    public static final String NODE_VIEW = "clusterNodeView";
    public static final String CLUSTER_SIZE = "clusterSize";
    public static final String CLUSTER_MEMBERS = "clusterMembers";
    public static final int UNKNOWN_NODE_VIEW = -1;

    private final GetCacheManager getCacheManager = new GetCacheManager();

    /**
     * Extract the BinaryStore out of Modeshape
     * (infinspan, jdbc, file, transient, etc)
     * @return the binary store configuration as a map
     */
    @Override
    public Map<String, String> apply(final Repository input) {
        requireNonNull(input, "null cannot have a BinaryStore!");

        final Map<String, String> result = new LinkedHashMap<>();

        final RepositoryConfiguration config = ((JcrRepository)input).getConfiguration();

        try {
            if (!(config.getBinaryStorage().getBinaryStore() instanceof InfinispanBinaryStore)) {
                return result;
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not extract BinaryStore configuration", ex);
        }

        final DefaultCacheManager cm = (DefaultCacheManager)getCacheManager.apply(input);

        if (cm == null) {
            LOGGER.debug("Could not access infinispan configuration");
            return result;
        }

        final int nodeView;

        if (cm.getTransport() != null) {
            nodeView = cm.getTransport().getViewId() + 1;
        } else {
            nodeView = UNKNOWN_NODE_VIEW;
        }

        result.put(CLUSTER_NAME, cm.getClusterName());
        result.put(CACHE_MODE, cm.getCache().getCacheConfiguration().clustering().cacheMode().toString());
        result.put(NODE_ADDRESS, cm.getNodeAddress());
        result.put(PHYSICAL_ADDRESS, cm.getPhysicalAddresses());
        result.put(NODE_VIEW, nodeView == UNKNOWN_NODE_VIEW ? "Unknown" : String.valueOf(nodeView));
        result.put(CLUSTER_SIZE, String.valueOf(cm.getClusterSize()));
        result.put(CLUSTER_MEMBERS, cm.getClusterMembers());
        return result;
    }
}
