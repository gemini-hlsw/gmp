package edu.gemini.jms.activemq.broker;

import org.junit.Test;

import javax.management.*;
import java.util.List;
import java.util.Set;

import static edu.gemini.jms.activemq.broker.ActiveMQBroker.activemq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test on how to build the ActiveMQBroker
 * Since ActiveMQBroker is not meant to expose the configuration publicly
 * we can only check some of those value using JMX
 */
public class ActiveMQBrokerBuilderTest {

    private MBeanServer mBeanServer;
    private ObjectName brokerObjectName;
    private final String connectionUrl = "vm://gmp";
    private final String activeMQName = "ActiveMQ";
    private static int startPort = 9100;
    private final ObjectName amqObjectNamePattern;

    public ActiveMQBrokerBuilderTest() throws MalformedObjectNameException {
        amqObjectNamePattern = new ObjectName("org.apache.activemq:BrokerName=*,Type=Broker");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadUrl() {
        ActiveMQBroker broker = activemq().url("nonvalid").build();
    }

    @Test
    public void testBuilder() throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException, IntrospectionException {
        ActiveMQBroker broker = startBuildingBroker().build();
        assertNotNull(broker);
        broker.start();

        findBrokerMBean();
        assertEquals(ConfigDefaults.BROKER_NAME, getMBeanAttribute("BrokerName"));
        assertEquals(ConfigDefaults.BROKER_PERSISTENT, getMBeanAttribute("Persistent"));
        assertEquals(connectionUrl, getMBeanAttribute("VMURL"));
        broker.shutdown();
    }

    private ActiveMQBroker.Builder startBuildingBroker() {
        // Use increasing ports to avoid the usual rmi port conflict
        return activemq().url(connectionUrl).useJmx(true).jmxConnectorPort(startPort++).jmxRrmiServerPort(startPort++);
    }

    @Test
    public void testBuilderWithName() throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException, IntrospectionException {
        ActiveMQBroker broker = startBuildingBroker().name(activeMQName).build();
        assertNotNull(broker);
        broker.start();

        findBrokerMBean();
        assertEquals(activeMQName, getMBeanAttribute("BrokerName"));
        broker.shutdown();
    }

    @Test
    public void testPersistentBuilder() throws InstanceNotFoundException, AttributeNotFoundException, ReflectionException, MBeanException, IntrospectionException {
        ActiveMQBroker broker = startBuildingBroker().persistent(true).build();
        assertNotNull(broker);
        broker.start();

        findBrokerMBean();
        assertEquals(true, getMBeanAttribute("Persistent"));
        broker.shutdown();
    }

    private void findBrokerMBean() throws IntrospectionException, InstanceNotFoundException, ReflectionException {
        List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        // There should be only one broker
        assertEquals(1, servers.size());
        mBeanServer = servers.get(0);
        Set<ObjectName> instances = mBeanServer.queryNames(amqObjectNamePattern, null);
        // There should be only one broker
        assertEquals(1, instances.size());
        brokerObjectName = instances.iterator().next();
        assertNotNull(brokerObjectName);
    }

    private Object getMBeanAttribute(String attribute) throws InstanceNotFoundException, ReflectionException, AttributeNotFoundException, MBeanException {
        return mBeanServer.getAttribute(brokerObjectName, attribute);
    }
}
