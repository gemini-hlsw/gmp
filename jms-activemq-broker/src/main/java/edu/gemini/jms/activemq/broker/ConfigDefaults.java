package edu.gemini.jms.activemq.broker;

/**
 * Default configuration options for the broker
 */
public class ConfigDefaults {

    public static final String BROKER_NAME_PROPERTY = "brokerName";
    public static final String BROKER_URL_PROPERTY = "brokerUrl";
    public static final String BROKER_STOMP_URL_PROPERTY = "stompUrl";
    public static final String BROKER_PERSISTENT_PROPERTY = "persistent";
    public static final String BROKER_DELETE_MESSAGES_ON_STARTUP_PROPERTY = "deleteMsgOnStartup";
    public static final String BROKER_USE_ADVISORY_MESSAGES_PROPERTY = "useAdvisoryMessages";
    public static final String BROKER_USE_JMX_PROPERTY = "useJmx";
    public static final String BROKER_JMX_RMI_PORT_PROPERTY = "jmxRmiServerPort";
    public static final String BROKER_JMX_CONNECTOR_PORT_PROPERTY = "jmxConnectorPort";
    public static final String BROKER_MEMORY_PERCENTAGE_PROPERTY = "memoryPercentage";
    public static final String BROKER_MAX_STORAGE_MB_PROPERTY = "maxStorageMB";
    public static final String BROKER_MAX_MESSAGES_LIMIT_PROPERTY = "maxMessagesLimit";

    private static final int BROKER_PORT = 61616;
    private static final int BROKER_STOMP_PORT = 61613;

    static final String BROKER_URL = "tcp://0.0.0.0:" + BROKER_PORT;
    static final String BROKER_STOMP_URL = "stomp+nio://0.0.0.0:" + BROKER_STOMP_PORT;
    static final String BROKER_NAME = "gmp";
    // Set to true to enable the broker's JMX usage
    static final boolean BROKER_USE_JMX = true;
    // Set to true if broker should use persistent storage
    static final boolean BROKER_PERSISTENT = false;
    // Should all persistent messages be deleted on broker startup
    static final boolean BROKER_DELETE_MESSAGES_ON_STARTUP = true;
    // Should use advisory messages
    static final boolean BROKER_USE_ADVISORY_MESSAGES = false;
    // Allow setting the JMX ports
    static final int BROKER_JMX_RMI_PORT = 1616;
    // Allow setting the JMX ports
    static final int BROKER_JMX_CONNECTOR_PORT = 1099;
    // Indicates the percentage of memory allocated to the server
    static final double BROKER_MEMORY_PERCENTAGE = 0.25;
    // Indicates how many MB are allowed for temp storage
    static final int BROKER_MAX_STORAGE_MB = 500;
    // Indicates how many messages are in memory per topic
    static final int BROKER_MAX_MESSAGES_LIMIT = 50;

}
