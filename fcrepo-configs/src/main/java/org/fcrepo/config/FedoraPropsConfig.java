/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.config;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * General Fedora properties
 *
 * @author pwinckles
 * @since 6.0.0
 */
@Configuration
public class FedoraPropsConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FedoraPropsConfig.class);

    public static final String FCREPO_JMS_HOST = "fcrepo.jms.host";
    public static final String FCREPO_DYNAMIC_JMS_PORT = "fcrepo.dynamic.jms.port";
    public static final String FCREPO_DYNAMIC_STOMP_PORT = "fcrepo.dynamic.stomp.port";
    public static final String FCREPO_ACTIVEMQ_CONFIGURATION = "fcrepo.activemq.configuration";
    public static final String FCREPO_NAMESPACE_REGISTRY = "fcrepo.namespace.registry";
    public static final String FCREPO_EXTERNAL_CONTENT_ALLOWED = "fcrepo.external.content.allowed";
    private static final String FCREPO_ACTIVEMQ_DIRECTORY = "fcrepo.activemq.directory";
    private static final String FCREPO_SESSION_TIMEOUT = "fcrepo.session.timeout";
    private static final String FCREPO_VELOCITY_RUNTIME_LOG = "fcrepo.velocity.runtime.log";
    private static final String FCREPO_REBUILD_VALIDATION = "fcrepo.rebuild.validation";
    private static final String FCREPO_REBUILD_VALIDATION_FIXITY = "fcrepo.rebuild.validation.fixity";
    private static final String FCREPO_REBUILD_ON_START = "fcrepo.rebuild.on.start";
    private static final String FCREPO_REBUILD_CONTINUE = "fcrepo.rebuild.continue";
    private static final String FCREPO_REBUILD = "fcrepo.rebuild";
    private static final String FCREPO_JMS_BASEURL = "fcrepo.jms.baseUrl";
    private static final String FCREPO_SERVER_MANAGED_PROPS_MODE = "fcrepo.properties.management";
    private static final String FCREPO_JMS_DESTINATION_TYPE = "fcrepo.jms.destination.type";
    private static final String FCREPO_JMS_DESTINATION_NAME = "fcrepo.jms.destination.name";
    public static final String FCREPO_JMS_ENABLED = "fcrepo.jms.enabled";
    private static final String FCREPO_EVENT_THREADS = "fcrepo.event.threads";
    public static final String FCREPO_TRANSACTION_ON_CONFLICT = "fcrepo.response.include.transaction";

    private static final String DATA_DIR_DEFAULT_VALUE = "data";
    private static final String LOG_DIR_DEFAULT_VALUE = "logs";
    private static final String ACTIVE_MQ_DIR_DEFAULT_VALUE = "ActiveMQ/kahadb";

    @Value("${" + FCREPO_HOME_PROPERTY + ":" + DEFAULT_FCREPO_HOME_VALUE + "}")
    protected Path fedoraHome;

    @Value("#{fedoraPropsConfig.fedoraHome.resolve('" + DATA_DIR_DEFAULT_VALUE + "')}")
    private Path fedoraData;

    @Value("#{fedoraPropsConfig.fedoraHome.resolve('" + LOG_DIR_DEFAULT_VALUE + "')}")
    private Path fedoraLogs;

    @Value("${" + FCREPO_JMS_HOST + ":localhost}")
    private String jmsHost;

    @Value("${" + FCREPO_DYNAMIC_JMS_PORT + ":61616}")
    private String jmsPort;

    @Value("${" + FCREPO_DYNAMIC_STOMP_PORT + ":61613}")
    private String stompPort;

    @Value("${" + FCREPO_ACTIVEMQ_CONFIGURATION + ":classpath:/config/activemq.xml}")
    private Resource activeMQConfiguration;

    @Value("${" + FCREPO_ACTIVEMQ_DIRECTORY + ":#{fedoraPropsConfig.fedoraData.resolve('" +
            ACTIVE_MQ_DIR_DEFAULT_VALUE + "').toAbsolutePath().toString()}}")
    private String activeMqDirectory;

    @Value("${" + FCREPO_NAMESPACE_REGISTRY + ":classpath:/namespaces.yml}")
    private String namespaceRegistry;

    @Value("${" + FCREPO_EXTERNAL_CONTENT_ALLOWED + ":#{null}}")
    private String externalContentAllowed;

    @Value("${" + FCREPO_SESSION_TIMEOUT + ":180000}")
    private Long sessionTimeoutLong;
    private Duration sessionTimeout;

    @Value("${" + FCREPO_VELOCITY_RUNTIME_LOG + ":" +
            "#{fedoraPropsConfig.fedoraLogs.resolve('velocity.log').toString()}}")
    private Path velocityLog;

    @Value("${" + FCREPO_REBUILD_VALIDATION + ":true}")
    private boolean rebuildValidation;

    @Value("${" + FCREPO_REBUILD_VALIDATION_FIXITY + ":false}")
    private boolean rebuildFixityCheck;

    @Deprecated
    @Value("${" + FCREPO_REBUILD_ON_START + ":false}")
    private boolean rebuildOnStart;

    @Deprecated
    @Value("${" + FCREPO_REBUILD_CONTINUE + ":false}")
    private boolean rebuildContinue;

    @Value("${" + FCREPO_REBUILD + ":false}")
    private boolean rebuildEnabled;

    @Value("${" + FCREPO_JMS_BASEURL + ":#{null}}")
    private String jmsBaseUrl;

    @Value("${" + FCREPO_SERVER_MANAGED_PROPS_MODE + ":strict}")
    private String serverManagedPropsModeStr;
    private ServerManagedPropsMode serverManagedPropsMode;

    @Value("${" + FCREPO_JMS_DESTINATION_TYPE + ":topic}")
    private String jmsDestinationTypeStr;
    private JmsDestination jmsDestinationType;

    @Value("${" + FCREPO_JMS_DESTINATION_NAME + ":fedora}")
    private String jmsDestinationName;

    @Value("${" + FCREPO_EVENT_THREADS + ":1}")
    private int eventBusThreads;

    @Value("${fcrepo.cache.db.containment.size.entries:1024}")
    private long containmentCacheSize;

    @Value("${fcrepo.cache.db.containment.timeout.minutes:10}")
    private long containmentCacheTimeout;

    @Value("${fcrepo.cache.types.size.entries:1024}")
    private long userTypesCacheSize;

    @Value("${fcrepo.cache.types.timeout.minutes:10}")
    private long userTypesCacheTimeout;

    @Value("${fcrepo.cache.webac.acl.size.entries:1024}")
    private long webacCacheSize;

    @Value("${fcrepo.cache.webac.acl.timeout.minutes:10}")
    private long webacCacheTimeout;

    @Value("${fcrepo.banner.enabled:true}")
    private boolean bannerEnabled;

    @Value("${fcrepo.pid.minter.length:0}")
    private int fcrepoPidMinterLength;

    @Value("${fcrepo.pid.minter.count:0}")
    private int fcrepoPidMinterCount;

    @Value("${" + FCREPO_TRANSACTION_ON_CONFLICT + ":false}")
    private boolean includeTransactionOnConflict;



    @PostConstruct
    private void postConstruct() throws IOException {
        LOGGER.info("Fedora is an open source project supported entirely by its users. To continue the " +
                "on-going maintenance of the software and ensure Fedora is meeting the needs of the " +
                "community, we are working to gather information on current installations. We strongly " +
                "encourage all users to register their instance in the DuraSpace Community Supported " +
                "Program Registry hosted by LYRASIS at https://fedora.lyrasis.org/register-your-site/");
        LOGGER.info("Fedora home: {}", fedoraHome);
        LOGGER.debug("Fedora home data: {}", fedoraData);
        try {
            createDirectories(fedoraHome);
        } catch (final IOException e) {
            throw new IOException(String.format("Failed to create Fedora home directory at %s." +
                    " Fedora home can be configured by setting the %s property.", fedoraHome, FCREPO_HOME_PROPERTY), e);
        }
        createDirectories(fedoraData);
        serverManagedPropsMode = ServerManagedPropsMode.fromString(serverManagedPropsModeStr);
        sessionTimeout = Duration.ofMillis(sessionTimeoutLong);
        jmsDestinationType = JmsDestination.fromString(jmsDestinationTypeStr);

        checkRebuildProps();
        checkDeprecatedProperties();
    }

    /**
     * Check if the rebuild fixity check prop was set without the rebuild validation being enabled
     */
    private void checkRebuildProps() {
        if (rebuildFixityCheck && !rebuildValidation) {
            throw new IllegalStateException(FCREPO_REBUILD_VALIDATION_FIXITY + " must be false when " +
                                            FCREPO_REBUILD_VALIDATION + " is false.");
        }
    }

    /**
     * Check for deprecated properties and log warnings if they are used
     */
    private void checkDeprecatedProperties() {
        if (System.getProperty(FCREPO_REBUILD_CONTINUE) != null) {
            LOGGER.warn("The property '{}' is deprecated and will be removed in a future version. " +
                    "Use {} instead.", FCREPO_REBUILD_CONTINUE, FCREPO_REBUILD);
        }
        if (System.getProperty(FCREPO_REBUILD_ON_START) != null) {
            LOGGER.warn("The property '{}' is deprecated and will be removed in a future version. " +
                    "It now behaves the same as {}.", FCREPO_REBUILD_CONTINUE, FCREPO_REBUILD);
        }
    }

    /**
     * @return Path to Fedora home directory
     */
    public Path getFedoraHome() {
        return fedoraHome;
    }

    /**
     * Sets the path to the Fedora home directory -- should only be used for testing purposes.
     *
     * @param fedoraHome Path to Fedora home directory
     */
    public void setFedoraHome(final Path fedoraHome) {
        this.fedoraHome = fedoraHome;
    }

    /**
     * @return Path to Fedora home data directory
     */
    public Path getFedoraData() {
        return fedoraData;
    }

    /**
     * @return Path to Fedora home logs directory
     */
    public Path getFedoraLogs() {
        return fedoraLogs;
    }

    /**
     * Sets the path to the Fedora home data directory -- should only be used for testing purposes.
     *
     * @param fedoraData Path to Fedora home data directory
     */
    public void setFedoraData(final Path fedoraData) {
        this.fedoraData = fedoraData;
    }

    /**
     * @return The JMS host
     */
    public String getJmsHost() {
        return jmsHost;
    }

    /**
     * @return The JMS/Open Wire port
     */
    public String getJmsPort() {
        return jmsPort;
    }

    /**
     * @return The STOMP protocol port
     */
    public String getStompPort() {
        return stompPort;
    }

    /**
     * @return The ActiveMQ data directory
     */
    public String getActiveMqDirectory() {
        return activeMqDirectory;
    }

    /**
     * @return The path to the ActiveMQ xml spring configuration.
     */
    public Resource getActiveMQConfiguration() {
        return activeMQConfiguration;
    }

    /**
     * @return The path to the allowed external content pattern definitions.
     */
    public String getExternalContentAllowed() {
        return externalContentAllowed;
    }

    /**
     * @return The path to the namespace registry file.
     */
    public String getNamespaceRegistry() {
        return namespaceRegistry;
    }

    /**
     * @return The timeout in milliseconds of the persistence session
     */
    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * @param sessionTimeout the session timeout duration
     */
    public void setSessionTimeout(final Duration sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    /**
     * @return The path to the velocity log.
     */
    public Path getVelocityLog() {
        return velocityLog;
    }

    /**
     * @return true if the rebuild object validation should run
     */
    public boolean isRebuildValidation() {
        return rebuildValidation;
    }

    /**
     * @return true if the rebuild validation should also check file fixity
     */
    public boolean isRebuildFixityCheck() {
        return rebuildFixityCheck;
    }

    /**
     * @return true if the internal indices should be rebuilt when Fedora starts up.
     */
    public boolean isRebuildOnStart() {
        return rebuildOnStart;
    }

    /**
     * @param rebuildOnStart A boolean flag indicating whether or not to rebuild on start
     */
    public void setRebuildOnStart(final boolean rebuildOnStart) {
        this.rebuildOnStart = rebuildOnStart;
    }

    /**
     * @return true if we should run a rebuild to add those resources missing from the indexes.
     */
    public boolean isRebuildContinue() {
        return rebuildContinue;
    }

    /**
     * @param rebuildContinue A boolean flag indicating whether or not to continue a rebuild on start
     */
    public void setRebuildContinue(final boolean rebuildContinue) {
        this.rebuildContinue = rebuildContinue;
    }

    /**
     * @return true if the internal indices should be rebuilt when Fedora starts up.
     */
    public boolean isRebuildEnabled() {
        return rebuildEnabled;
    }

    /**
     * @param rebuildEnabled A boolean flag indicating whether to rebuild on start
     */
    public void setRebuildEnabled(boolean rebuildEnabled) {
        this.rebuildEnabled = rebuildEnabled;
    }

    /**
     * @return the JMS base url, if specified
     */
    public String getJmsBaseUrl() {
        return jmsBaseUrl;
    }

    /**
     * @return the server managed properties mode, default strict
     */
    public ServerManagedPropsMode getServerManagedPropsMode() {
        return serverManagedPropsMode;
    }

    /**
     * @param serverManagedPropsMode the server managed props mode
     */
    public void setServerManagedPropsMode(final ServerManagedPropsMode serverManagedPropsMode) {
        this.serverManagedPropsMode = serverManagedPropsMode;
    }

    /**
     * @return the jms destination type
     */
    public JmsDestination getJmsDestinationType() {
        return jmsDestinationType;
    }

    /**
     * @return the jms destination name
     */
    public String getJmsDestinationName() {
        return jmsDestinationName;
    }

    /**
     * @return the number of threads to allocate in the event bus thread pool
     *         if this number is less than 1, 1 is returned
     */
    public int getEventBusThreads() {
        if (eventBusThreads < 1) {
            return 1;
        }
        return eventBusThreads;
    }

    /**
     * @return The number of entries in the containment cache.
     */
    public long getContainmentCacheSize() {
        return containmentCacheSize;
    }

    /**
     * @return The number of minutes before items in the containment cache expire.
     */
    public long getContainmentCacheTimeout() {
        return containmentCacheTimeout;
    }

    /**
     * @return The number of entries in the user types cache.
     */
    public long getUserTypesCacheSize() {
        return userTypesCacheSize;
    }

    /**
     * @param userTypesCacheSize user types cache size
     */
    public void setUserTypesCacheSize(final long userTypesCacheSize) {
        this.userTypesCacheSize = userTypesCacheSize;
    }

    /**
     * @return The number of minutes before items in the user types cache expire.
     */
    public long getUserTypesCacheTimeout() {
        return userTypesCacheTimeout;
    }

    /**
     * @param userTypesCacheTimeout user types cache timeout
     */
    public void setUserTypesCacheTimeout(final long userTypesCacheTimeout) {
        this.userTypesCacheTimeout = userTypesCacheTimeout;
    }

    /**
     * @return The number of entries in the WebAC effective ACL cache.
     */
    public long getWebacCacheSize() {
        return webacCacheSize;
    }

    /**
     * @return The number of minutes before items in the WebAC ACL cache expire.
     */
    public long getWebacCacheTimeout() {
        return webacCacheTimeout;
    }

    /**
     * @return whether the repository registration banner should be displayed
     */
    public boolean getBannerEnabled() {
        return bannerEnabled;
    }

    /**
     * @return length of pid minter
     */
    public int getFcrepoPidMinterLength() {
        return fcrepoPidMinterLength;
    }

    /**
     * @param length PID Minter length
     */
    public void setFcrepoPidMinterLength(final int length) {
        this.fcrepoPidMinterLength = length;
    }

    /**
     * @return count of separaters for pid minter
     */
    public int getFcrepoPidMinterCount() {
        return fcrepoPidMinterCount;
    }

    /**
     * @param count PID Minter count
     */
    public void setFcrepoPidMinterCount(final int count) {
        this.fcrepoPidMinterCount = count;
    }


    /**
     * @return if transaction ids should be included in conflict error responses
     */
    public boolean includeTransactionOnConflict() {
        return includeTransactionOnConflict;
    }

    /**
     * @param includeTransactionOnConflict if transaction ids should be included in conflict error responses
     */
    public void setIncludeTransactionOnConflict(final boolean includeTransactionOnConflict) {
        this.includeTransactionOnConflict = includeTransactionOnConflict;
    }

}
