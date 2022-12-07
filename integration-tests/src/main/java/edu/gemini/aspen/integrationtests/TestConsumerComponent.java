// package edu.gemini.aspen.integrationtests;
//
// import edu.gemini.aspen.heartbeatdistributor.HeartbeatConsumer;
// import org.apache.felix.ipojo.annotations.Component;
// import org.apache.felix.ipojo.annotations.Provides;
//
// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.TimeUnit;
// import java.util.logging.Logger;
//
// /**
// * Class TestConsumerComponent.
// *
// * @author Nicolas A. Barriga
// *         Date: 3/10/11
// */
// @Component
// @Provides
// public class TestConsumerComponent implements HeartbeatConsumer {
//     private final Logger LOG = Logger.getLogger(TestConsumerComponent.class.getName());
//     private final CountDownLatch latch;
//
//     private int last=-1;
//     private int count=0;
//
//     public TestConsumerComponent(int latchSize){
//         latch=new CountDownLatch(latchSize);
//     }
//
//     public int getLast(){
//         return last;
//     }
//
//     public int getCount(){
//         return count;
//     }
//
//     @Override
//     public void beat(int beatNumber) {
//         LOG.info("Heartbeat: "+beatNumber);
//         last=beatNumber;
//         count++;
//         latch.countDown();
//     }
//
//     /**
//      * Wait until the latch has been activated (by an arriving beat), or a timeout has elapsed.
//      *
//      * @param l timeout length
//      * @param timeUnit unit of the timeout
//      * @throws java.lang.InterruptedException
//      */
//     public void waitOnLatch(long l, TimeUnit timeUnit) throws java.lang.InterruptedException{
//          latch.await(l,timeUnit);
//     }
//
//     /**
//      * Wait until the latch has been activated (by an arriving beat), or a timeout (in milliseconds) has elapsed.
//      *
//      * @param l        timeout length in milliseconds
//      * @throws java.lang.InterruptedException
//      */
//     public void waitOnLatch(long l) throws java.lang.InterruptedException {
//         latch.await(l, TimeUnit.MILLISECONDS);
//     }
//
// }
