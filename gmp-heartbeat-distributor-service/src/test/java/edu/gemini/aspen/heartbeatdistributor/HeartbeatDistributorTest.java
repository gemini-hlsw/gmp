// package edu.gemini.aspen.heartbeatdistributor;
//
// import edu.gemini.aspen.giapi.util.jms.JmsKeys;
// import edu.gemini.jms.activemq.provider.ActiveMQJmsProvider;
// import edu.gemini.jms.api.BaseMessageProducer;
// import edu.gemini.jms.api.DestinationData;
// import edu.gemini.jms.api.DestinationType;
// import edu.gemini.jms.api.JmsProvider;
// import org.apache.activemq.broker.BrokerService;
// import org.junit.Test;
//
// import javax.jms.BytesMessage;
// import javax.jms.JMSException;
// import java.util.concurrent.TimeUnit;
// import java.util.logging.Level;
//
// import static org.junit.Assert.assertEquals;
//
// /**
//  * Class HeartbeatDistributorTest
//  *
//  * @author Nicolas A. Barriga
//  *         Date: 3/11/11
//  */
// public class HeartbeatDistributorTest {
//     private class HeartbeatMessageProducer extends BaseMessageProducer{
//         public HeartbeatMessageProducer() {
//             super("Heartbeat", new DestinationData(JmsKeys.GMP_HEARTBEAT_DESTINATION, DestinationType.TOPIC));
//         }
//
//         /**
//          * Create and send a JMS Heartbeat message, with a number 1
//          */
//         public void sendBeat() {
//             try {
//                 BytesMessage m = _session.createBytesMessage();
//                 m.writeInt(1);
//                 _producer.send(m);
//             } catch (JMSException e) {
//                 LOG.log(Level.SEVERE, e.getMessage(), e);
//             }
//         }
//     }
//
//     /**
//      * Sends a beat via JMS, listen for it with a Distributor, and notify a registered client.
//      *
//      * @throws Exception
//      */
//     @Test
//     public void test() throws Exception{
//
//         BrokerService broker = new BrokerService();
//         broker.addConnector("vm://HeartbeatTestBroker");
//         broker.setUseJmx(false);
//         broker.setPersistent(false);
//         broker.start();
//         JmsProvider provider = new ActiveMQJmsProvider("vm://HeartbeatTestBroker");
//
//         HeartbeatMessageProducer hbmp = new HeartbeatMessageProducer();
//         hbmp.startJms(provider);
//
//         HeartbeatDistributor hbDist = new HeartbeatDistributor();
//         hbDist.startJms(provider);
//
//         TestConsumerComponent comp = new TestConsumerComponent(1);
//         hbDist.bindHeartbeatConsumer(comp);
//
//         hbmp.sendBeat();
//         //wait at most 1 second for the beat to arrive
//         comp.waitOnLatch(1, TimeUnit.SECONDS);
//         //check that the beat number 1 arrived. The latch might have been released by the timeout.
//         assertEquals(1,comp.getLast());
//     }
//
//
// }
