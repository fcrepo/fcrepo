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
package org.fcrepo.serialization;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Helper service that aggregates serializers and makes them accessible by key
 *
 * @author cbeer
 */
@Component
public class SerializerUtil implements ApplicationContextAware {

    private static final Logger LOGGER = getLogger(SerializerUtil.class);

    private ApplicationContext applicationContext;

    private Map<String, FedoraObjectSerializer> serializerMap;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Get the list of Fedora serializer keys
     * @return the list of Fedora serializer keys
     */
    public Set<String> keySet() {
        return getFedoraObjectSerializers().keySet();
    }

    /**
     * Get a Fedora Object Serializer by its key
     * @param format
     * @return FedoraObjectSerializer for the given format
     */
    public FedoraObjectSerializer getSerializer(final String format) {
        return getFedoraObjectSerializers().get(format);
    }

    /**
     * Get the whole list of FedoraObjectSerializers
     * @return map of all serializers with format as the key
     */
    public Map<String, FedoraObjectSerializer> getFedoraObjectSerializers() {
        return serializerMap;
    }

    /**
     * Hook into Spring to get the list of all FedoraObjectSerializers that
     * were (supposedly) component scanned, and register them in our own
     * map.
     */
    @PostConstruct
    public void buildFedoraObjectSerializersMap() {
        final Map<String, FedoraObjectSerializer> beans =
                applicationContext.getBeansOfType(FedoraObjectSerializer.class);

        final Map<String, FedoraObjectSerializer> m = new HashMap<>();

        for (Map.Entry<String, FedoraObjectSerializer> e : beans.entrySet()) {
            final FedoraObjectSerializer serializer = e.getValue();
            LOGGER.info("Registering serializer {} for format {}", serializer,
                    serializer.getKey());
            m.put(serializer.getKey(), serializer);
        }

        serializerMap = m;

    }
}
