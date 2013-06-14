package org.fcrepo.serialization;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
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
 */
@Component
public class SerializerUtil implements ApplicationContextAware {
    private static final Logger LOGGER = getLogger(SerializerUtil.class);
    private ApplicationContext applicationContext;
    private Map<String, FedoraObjectSerializer> serializerMap;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Set<String> keySet() {
        return getFedoraObjectSerializers().keySet();
    }

    public FedoraObjectSerializer getSerializer(final String format) {
        return getFedoraObjectSerializers().get(format);
    }

    public Map<String, FedoraObjectSerializer> getFedoraObjectSerializers() {
        return serializerMap;
    }

    @PostConstruct
    public void buildFedoraObjectSerializersMap() {
        final Map<String, FedoraObjectSerializer> beans = applicationContext.getBeansOfType(FedoraObjectSerializer.class);

        final Map<String, FedoraObjectSerializer> m = new HashMap<>();

        for(Map.Entry<String, FedoraObjectSerializer> e : beans.entrySet()) {
            final FedoraObjectSerializer serializer = e.getValue();
            LOGGER.info("Registering serializer {} for format {}", serializer, serializer.getKey());
            m.put(serializer.getKey(), serializer);
        }

        serializerMap = m;

    }
}
