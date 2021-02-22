package edu.gemini.aspen.gmp.epicstostatus;


import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;
import edu.gemini.aspen.giapi.statusservice.StatusHandlerAggregate;
import edu.gemini.aspen.giapi.statusservice.StatusService;
import edu.gemini.cas.impl.ChannelAccessServerImpl;
import edu.gemini.epics.EpicsReader;
import edu.gemini.epics.EpicsService;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import edu.gemini.epics.api.Channel;
import edu.gemini.epics.impl.EpicsReaderImpl;
import edu.gemini.epics.impl.EpicsWriterImpl;
import edu.gemini.jms.activemq.provider.ActiveMQJmsProvider;
import edu.gemini.jms.api.JmsProvider;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Class EpicsStatusServiceTest
 *
 * @author Nicolas A. Barriga
 *         Date: Nov 9, 2010
 */
public class EpicsToStatusComponentTest {
    private class StatusMonitor implements StatusHandler {
        private StatusItem lastItem;

        @Override
        public String getName() {
            return "Status Monitor";
        }

        @Override
        public <T> void update(StatusItem<T> item) {
            lastItem = item;
        }

        public StatusItem getLast() {
            return lastItem;
        }
    }

    static {
        System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "127.0.0.1");
        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");
    }

    @Test
    public void testWithCas() throws Exception {
        //create fake config files
        File xml = File.createTempFile("EpicsTest", "xml");

        FileWriter xmlWrt = new FileWriter(xml);

        xmlWrt.write(EpicsToStatusConfigurationTest.xmlStr);
        xmlWrt.close();


        //read config
        EpicsToStatusConfiguration essc = new EpicsToStatusConfiguration(xml.getPath());

        //create EPICS channels
        ChannelAccessServerImpl cas = new ChannelAccessServerImpl();
        cas.start();
        Channel<Integer> casCh = cas.createChannel("giapitest:epicschannelint", 1);
        cas.createAlarmChannel("giapitest:epicsalarmfloat", 1.0f);
        cas.createChannel("giapitest:epicshealth1", "BAD");
        Thread.sleep(100);


        //Create EPICS reader and writer
        JmsProvider provider = new ActiveMQJmsProvider("vm://EpicsToStatusComponentTestBroker");
        EpicsService epicsServ = new EpicsService("127.0.0.1", 1.0, 0);
        epicsServ.startService();
        EpicsReader reader = new EpicsReaderImpl(epicsServ);
        EpicsWriter writer = new EpicsWriterImpl(epicsServ);


        //setup a status service and a status handler to check updates are getting published
        StatusMonitor monitor = new StatusMonitor();
        StatusHandlerAggregate aggregate = new StatusHandlerAggregate();
        aggregate.bindStatusHandler(monitor);
        StatusService service = new StatusService(aggregate, "Status Monitor Service Client", "giapitest:statusitemint");
        service.startJms(provider);

        //initialize client EPICS channels, channel listeners and StatusSetters
        EpicsToStatusComponent esc = new EpicsToStatusComponent(reader, provider, xml.getPath());
        esc.initialize();

        //write the channel to trigger an update
        ReadWriteClientEpicsChannel<Integer> writeCh = writer.getIntegerChannel("giapitest:epicschannelint");
        assertTrue(writeCh.isValid());
        writeCh.setValue(2);

        //wait a bit and check that the update got published
        Thread.sleep(100);
        assertEquals("giapitest:statusitemint", monitor.getLast().getName());
        assertEquals(2, monitor.getLast().getValue());


        //shutdown everything
        esc.shutdown();
        epicsServ.stopService();
        service.stopJms();
        cas.stop();
    }
}
