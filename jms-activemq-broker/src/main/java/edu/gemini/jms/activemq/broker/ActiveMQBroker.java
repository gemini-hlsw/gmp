package edu.gemini.jms.activemq.broker;

import edu.gemini.jms.api.Broker;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.broker.region.policy.ConstantPendingMessageLimitStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A JMS Broker implementation, based on the ActiveMQ JMS service
 */
public class ActiveMQBroker implements Broker {

    private static final Logger LOG = Logger.getLogger(ActiveMQBroker.class.getName());

    private final BrokerService _broker;

    private ActiveMQBroker(Builder builder) {
        _broker = new BrokerService();
        _broker.setUseJmx(builder.useJmx);
        _broker.setPersistent(builder.isPersistent);
        _broker.setBrokerName(builder.brokerName);
        _broker.setDeleteAllMessagesOnStartup(builder.deleteMsgOnStartup);
        _broker.setAdvisorySupport(builder.useAdvisoryMessages);

        // Define some limits for resource usage
        _broker.getSystemUsage().getMemoryUsage().setLimit((int)(Runtime.getRuntime().totalMemory() * Math.min(0.9, builder.memoryPercentage)));
        _broker.getSystemUsage().getStoreUsage().setLimit(builder.maxStorageMB * 1024 * 1024);
        _broker.getSystemUsage().getTempUsage().setLimit(builder.maxStorageMB * 1024 * 1024);
        //Set the Destination policies
        PolicyEntry policy = new PolicyEntry();
        //set a memory limit of 4mb for each destination
        policy.setMemoryLimit(4 * 1024 *1024);
        //disable flow control (Important, this removes the slowing down of producers if consumers are slow)
        policy.setProducerFlowControl(false);

        // This makes the broker not to hold too many messages in memory
        ConstantPendingMessageLimitStrategy limitStrategy = new ConstantPendingMessageLimitStrategy();
        limitStrategy.setLimit(builder.maxMessagesLimit);
        policy.setPendingMessageLimitStrategy(limitStrategy);

        PolicyMap pMap = new PolicyMap();
        //configure the policy
        pMap.setDefaultEntry(policy);
        _broker.setDestinationPolicy(pMap);
        if (builder.useJmx) {
            ManagementContext managementContext = new ManagementContext();
            managementContext.setCreateMBeanServer(false);
            managementContext.setFindTigerMbeanServer(true);

            managementContext.setRmiServerPort(builder.jmxRmiServerPort);
            managementContext.setConnectorPort(builder.jmxConnectorPort);
            _broker.setManagementContext(managementContext);
        }
        // The OpenWire connector is mandatory, GIAPI clients (giapi-tester, C++/Java glue) use it
        if (builder.url == null || builder.url.isEmpty()) {
            throw new IllegalArgumentException("brokerUrl is required, the OpenWire connector cannot be disabled");
        }
        addConnector(builder.url);
        if (builder.stompUrl != null && !builder.stompUrl.isEmpty() && !builder.stompUrl.equals(builder.url)) {
            addConnector(builder.stompUrl);
        }
    }

    private void addConnector(String url) {
        try {
            _broker.addConnector(url);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "URL for ActiveMQ Broker not valid: " + url + " aborting...", e);
            throw new IllegalArgumentException("Cannot start a broker with a url " + url);
        }
    }

    /**
     * Utility static method to create a Builder object
     *
     * @return a new Builder object
     */
    public static Builder activemq() {
        return new Builder();
    }

    /**
     * A Builder class for the ActiveMQ Broker
     */
    public static class Builder {
        private boolean useJmx = ConfigDefaults.BROKER_USE_JMX;
        private boolean isPersistent = ConfigDefaults.BROKER_PERSISTENT;
        private String brokerName = ConfigDefaults.BROKER_NAME;
        private String url = ConfigDefaults.BROKER_URL;
        private boolean deleteMsgOnStartup = ConfigDefaults.BROKER_DELETE_MESSAGES_ON_STARTUP;
        private boolean useAdvisoryMessages = ConfigDefaults.BROKER_USE_ADVISORY_MESSAGES;
        private int jmxRmiServerPort = ConfigDefaults.BROKER_JMX_RMI_PORT;  
        private int jmxConnectorPort = ConfigDefaults.BROKER_JMX_CONNECTOR_PORT;
        private double memoryPercentage = ConfigDefaults.BROKER_MEMORY_PERCENTAGE;
        private int maxStorageMB = ConfigDefaults.BROKER_MAX_STORAGE_MB;
        private int maxMessagesLimit = ConfigDefaults.BROKER_MAX_MESSAGES_LIMIT;
        // No STOMP connector unless a stompUrl is set
        private String stompUrl = null;

        public Builder useJmx(boolean useJmx) {
            this.useJmx = useJmx;
            return this;
        }

        public Builder useAdvisoryMessages(boolean useAdvisoryMessages) {
            this.useAdvisoryMessages = useAdvisoryMessages;
            return this;
        }

        Builder jmxRrmiServerPort(int jmxRmiServerPort) {
            this.jmxRmiServerPort = jmxRmiServerPort;
            return this;
        }

        Builder memoryPercentage(double memoryPercentage) {
            this.memoryPercentage = memoryPercentage;
            return this;
        }

        Builder maxStorageMB(int maxStorageMB) {
            this.maxStorageMB = maxStorageMB;
            return this;
        }
        Builder maxMessagesLimit(int maxMessagesLimit) {
            this.maxMessagesLimit = maxMessagesLimit;
            return this;
        }

        public Builder jmxConnectorPort(int jmxConnectorPort) {
            this.jmxConnectorPort = jmxConnectorPort;
            return this;
        }

        public Builder name(String name) {
            brokerName = name;
            return this;
        }

        public Builder persistent(boolean persistent) {
            isPersistent = persistent;
            return this;
        }

        public Builder deleteMsgOnStartup(boolean delete) {
            deleteMsgOnStartup = delete;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder stompUrl(String stompUrl) {
            this.stompUrl = stompUrl;
            return this;
        }

        public ActiveMQBroker build() {
            return new ActiveMQBroker(this);
        }
    }


    public void start() {
        LOG.info("Starting up ActiveMQ AMQ Broker");
        try {
            _broker.start();
            if (_broker.waitUntilStarted()) {
                LOG.info("ActiveMQ Broker Component Started");
            } else {
                LOG.severe("ActiveMQ didn't start properly");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while starting broker", e);
        }
    }

    public void shutdown() {
        LOG.info("Shutting down ActiveMQ Broker");
        try {
            _broker.stop();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while stopping broker", e);
        }
    }
}
