package edu.gemini.jms.activemq.provider;

import edu.gemini.jms.api.JmsArtifact;
import edu.gemini.jms.api.JmsProvider;
import edu.gemini.jms.api.JmsProviderStatusListener;
import org.junit.Test;

import javax.jms.JMSException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ActiveMQJMSProviderTest {
    @Test
    public void testConstruction() {
        String brokerUrl = "vm:testBroker?broker.persistent=false";
        ActiveMQJmsProvider provider = new ActiveMQJmsProvider(brokerUrl, "1000");
        provider.startConnection();

        assertNotNull(provider.getConnectionFactory());
    }

    @Test
    public void testConstructionWithPropertySubstitution() {
        String brokerUrl = "${address}?broker.persistent=false";
        System.setProperty("address", "vm:testBroker");
        ActiveMQJmsProvider provider = new ActiveMQJmsProvider(brokerUrl, "1000");
        provider.startConnection();

        assertNotNull(provider.getConnectionFactory());
    }

    @Test
    public void addStatusListener() throws InterruptedException, JMSException {
        String brokerUrl = "failover:(vm:testBroker?broker.persistent=false)";
        ActiveMQJmsProvider provider = new ActiveMQJmsProvider(brokerUrl, "1000");

        final AtomicBoolean resumed = new AtomicBoolean(false);

        provider.bindJmsStatusListener(new JmsProviderStatusListener() {
            @Override
            public void transportResumed() {
                resumed.set(true);
            }

            @Override
            public void transportInterrupted() {
            }
        });

        assertFalse(resumed.get());

        provider.startConnection();

        TimeUnit.SECONDS.sleep(2);

        assertTrue(resumed.get());
    }

    @Test
    public void addJmsArtifact() throws InterruptedException, JMSException {
        String brokerUrl = "failover:(vm:testBroker?broker.persistent=false)";
        ActiveMQJmsProvider provider = new ActiveMQJmsProvider(brokerUrl, "1000");

        final AtomicBoolean started = new AtomicBoolean(false);

        JmsArtifact jmsArtifact = new JmsArtifact() {
            @Override
            public void startJms(JmsProvider provider) throws JMSException {
                started.set(true);
            }

            @Override
            public void stopJms() {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        provider.bindJmsArtifact(jmsArtifact);

        assertFalse(started.get());

        provider.startConnection();

        TimeUnit.SECONDS.sleep(2);

        assertTrue(started.get());
    }
}
