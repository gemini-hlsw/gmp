package edu.gemini.aspen.heartbeatdistributor;

import org.junit.Ignore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
* Class TestConsumerComponent.
*
* @author Nicolas A. Barriga
*         Date: 3/10/11
*/
@Ignore
public class TestConsumerComponent implements HeartbeatConsumer {
    private final Logger LOG = Logger.getLogger(TestConsumerComponent.class.getName());

    private final CountDownLatch latch;

    private int last=-1;
    private int count=0;

    public TestConsumerComponent(int latchSize){
        latch=new CountDownLatch(latchSize);
    }

    public int getLast(){
        return last;
    }

    public int getCount(){
        return count;
    }

    @Override
    public void beat(int beatNumber) {
        LOG.info("Heartbeat: "+beatNumber);
        last=beatNumber;
        count++;
        latch.countDown();
    }

    /**
     * Wait until the latch has been activated (by an arriving beat), or a timeout has elapsed.
     *
     * @param l timeout length
     * @param timeUnit unit of the timeout
     * @throws InterruptedException
     */
    public void waitOnLatch(long l, TimeUnit timeUnit) throws InterruptedException{
         latch.await(l,timeUnit);
    }

    /**
     * Wait until the latch has been activated (by an arriving beat), or a timeout (in milliseconds) has elapsed.
     *
     * @param l        timeout length in milliseconds
     * @throws InterruptedException
     */
    public void waitOnLatch(long l) throws InterruptedException {
        latch.await(l, TimeUnit.MILLISECONDS);
    }

}
