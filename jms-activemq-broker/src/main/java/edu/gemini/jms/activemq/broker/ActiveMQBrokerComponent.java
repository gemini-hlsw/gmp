package edu.gemini.jms.activemq.broker;

import edu.gemini.jms.api.Broker;

import java.io.Serializable;
import java.util.logging.Logger;

import static edu.gemini.jms.activemq.broker.ActiveMQBroker.activemq;

/**
 * Thin wrapper of ActiveMQBroker to expose the configuration to the ConfigAdmin service
 *
 * @author cquiroz
 */
public class ActiveMQBrokerComponent implements Serializable {
    private static final Logger LOG = Logger.getLogger(ActiveMQBrokerComponent.class.getName());

    private final Broker _broker;

    private final String url;
    private final String stompUrl;

    public ActiveMQBrokerComponent(boolean useJmx,
                                   boolean persistent,
                                   String brokerName,
                                   String url,
                                   String stompUrl,
                                   boolean deleteMsgOnStartup,
                                   boolean useAdvisoryMessages,
                                   int jmxRmiServerPort,
                                   int jmxConnectorPort,
                                   double memoryPercentage,
                                   int maxMessagesLimit,
                                   int maxStorageMB) {
        this.url = url;
        // A null or empty stompUrl disables the STOMP connector
        this.stompUrl = stompUrl;

        _broker = activemq()
                .name(brokerName)
                .url(url)
                .stompUrl(stompUrl)
                .useJmx(useJmx)
                .persistent(persistent)
                .useAdvisoryMessages(useAdvisoryMessages)
                .deleteMsgOnStartup(deleteMsgOnStartup)
                .jmxConnectorPort(jmxConnectorPort)
                .jmxRrmiServerPort(jmxRmiServerPort)
                .memoryPercentage(memoryPercentage)
                .maxStorageMB(maxStorageMB)
                .maxMessagesLimit(maxMessagesLimit)
                .build();
    }

    public synchronized void startBroker() {
        if (stompUrl == null || stompUrl.isEmpty()) {
            LOG.info("Starting ActiveMQBroker broker with URL:" + url + ", STOMP connector disabled");
        } else {
            LOG.info("Starting ActiveMQBroker broker with URL:" + url + " and STOMP URL:" + stompUrl);
        }
        // Start in a separate thread, otherwise there is a risk of a race condition
        new Thread(new Runnable() {
            @Override
            public void run() {
                _broker.start();
            }
        }).start();

    }

    public synchronized void stopBroker() {
        _broker.shutdown();
    }
}
