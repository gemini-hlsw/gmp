package edu.gemini.jms.activemq.broker.osgi;

import com.google.common.collect.Maps;
import edu.gemini.jms.activemq.broker.ActiveMQBroker;
import edu.gemini.jms.activemq.broker.ActiveMQBrokerComponent;
import edu.gemini.jms.activemq.broker.ConfigDefaults;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.logging.Logger;

public class ActiveMQBrokerFactory implements ManagedServiceFactory {
    private static final Logger LOG = Logger.getLogger(ActiveMQBrokerFactory.class.getName());

    private final Map<String, ActiveMQBrokerComponent> existingServices = Maps.newHashMap();

    public String getName() {
        return "ActiveMQ Broker factory";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) {
        if (checkProperties(properties)) {
            ActiveMQBrokerComponent broker = createService(properties);
            broker.startBroker();
        } else {
            LOG.warning("Cannot build " + ActiveMQBroker.class.getName() + " without the required properties");
        }
    }

    private ActiveMQBrokerComponent createService(Dictionary<String, ?> properties) {
        try {
            boolean persistent = "true".equalsIgnoreCase(properties.get(ConfigDefaults.BROKER_PERSISTENT_PROPERTY).toString());
            boolean useJmx = "true".equalsIgnoreCase(properties.get(ConfigDefaults.BROKER_USE_JMX_PROPERTY).toString());
            String name = properties.get(ConfigDefaults.BROKER_NAME_PROPERTY).toString();
            String url = properties.get(ConfigDefaults.BROKER_URL_PROPERTY).toString();
            boolean deleteMessagesOnStartup = "true".equalsIgnoreCase(properties.get(ConfigDefaults.BROKER_DELETE_MESSAGES_ON_STARTUP_PROPERTY).toString());
            boolean useAdvisoryMessages = "true".equalsIgnoreCase(properties.get(ConfigDefaults.BROKER_USE_ADVISORY_MESSAGES_PROPERTY).toString());
            int jmxPort = Integer.parseInt(properties.get(ConfigDefaults.BROKER_JMX_RMI_PORT_PROPERTY).toString());
            int jmxConnectorPort = Integer.parseInt(properties.get(ConfigDefaults.BROKER_JMX_CONNECTOR_PORT_PROPERTY).toString());
            double memoryPercentage = Double.parseDouble(properties.get(ConfigDefaults.BROKER_MEMORY_PERCENTAGE_PROPERTY).toString());
            int maxStorageMB = Integer.parseInt(properties.get(ConfigDefaults.BROKER_MAX_STORAGE_MB_PROPERTY).toString());
            int maxMessagesLimit = Integer.parseInt(properties.get(ConfigDefaults.BROKER_MAX_MESSAGES_LIMIT_PROPERTY).toString());
            LOG.info("Build " + ActiveMQBroker.class.getName() + " with url " + url);
            return new ActiveMQBrokerComponent(useJmx, persistent, name, url, deleteMessagesOnStartup, useAdvisoryMessages, jmxPort, jmxConnectorPort, memoryPercentage, maxStorageMB, maxMessagesLimit);
        } catch (NumberFormatException e) {
            LOG.severe("Cannot start ActiveMQBroker");
            throw e;
        }
    }

    private boolean checkProperties(Dictionary<String, ?> properties) {
        return properties.get(ConfigDefaults.BROKER_PERSISTENT_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_USE_JMX_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_NAME_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_URL_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_DELETE_MESSAGES_ON_STARTUP_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_USE_ADVISORY_MESSAGES_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_JMX_RMI_PORT_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_JMX_CONNECTOR_PORT_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_MEMORY_PERCENTAGE_PROPERTY) != null &&
            properties.get(ConfigDefaults.BROKER_MAX_STORAGE_MB_PROPERTY) != null && 
            properties.get(ConfigDefaults.BROKER_MAX_MESSAGES_LIMIT_PROPERTY) != null;
    }

    @Override
    public void deleted(String pid) {
        if (existingServices.containsKey(pid)) {
            ActiveMQBrokerComponent activeMQBroker = existingServices.get(pid);
            activeMQBroker.stopBroker();
            existingServices.remove(pid);
        }
    }

}
