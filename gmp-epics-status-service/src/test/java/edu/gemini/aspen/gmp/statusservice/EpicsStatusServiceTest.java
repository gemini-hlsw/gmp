package edu.gemini.aspen.gmp.statusservice;

import edu.gemini.aspen.giapi.status.AlarmCause;
import edu.gemini.aspen.giapi.status.AlarmSeverity;
import edu.gemini.aspen.giapi.status.Health;
import edu.gemini.aspen.giapi.status.StatusItem;
import edu.gemini.aspen.giapi.status.impl.AlarmStatus;
import edu.gemini.aspen.giapi.status.impl.BasicStatus;
import edu.gemini.aspen.giapi.status.impl.HealthStatus;
import edu.gemini.aspen.gmp.statusservice.generated.*;
import edu.gemini.gmp.top.Top;
import edu.gemini.gmp.top.TopImpl;
import edu.gemini.cas.AlarmChannel;
import edu.gemini.cas.ChannelAccessServer;
import edu.gemini.cas.impl.ChannelAccessServerImpl;
import edu.gemini.epics.api.Channel;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Class EpicsStatusServiceTest
 *
 * @author Nicolas A. Barriga
 *         Date: Nov 9, 2010
 */
public class EpicsStatusServiceTest{
    private AlarmChannelType buildAlarmChannel(String giapiname, String epicsname, DataType type, String initial) {
        AlarmChannelType ch = new AlarmChannelType();
        ch.setGiapiname(giapiname);
        ch.setEpicsname(epicsname);
        ch.setType(type);
        ch.setInitial(initial);
        return ch;
    }

    private ChannelAccessServer cas;
    private File xml = null;
    private Top epicsTop;

    @Before
    public void setUp() throws Exception {
        cas = mock(ChannelAccessServerImpl.class);
        AlarmChannel iach = mock(AlarmChannel.class);
        when(cas.createAlarmChannel(anyString(), anyInt())).thenReturn(iach);
        when(cas.createAlarmChannel(anyString(), anyFloat())).thenReturn(iach);
        when(cas.createAlarmChannel(anyString(), anyDouble())).thenReturn(iach);
        when(cas.createAlarmChannel(anyString(), anyString())).thenReturn(iach);
        Channel ich = mock(Channel.class);
        when(cas.createChannel(anyString(), anyInt())).thenReturn(ich);
        when(cas.createChannel(anyString(), anyFloat())).thenReturn(ich);
        when(cas.createChannel(anyString(), anyDouble())).thenReturn(ich);
        when(cas.createChannel(anyString(), anyString())).thenReturn(ich);


        xml = File.createTempFile("EpicsTest", "xml");


        FileWriter xmlWrt = new FileWriter(xml);

        xmlWrt.write(EpicsStatusServiceConfigurationTest.xmlStr);
        xmlWrt.close();

        epicsTop = new TopImpl("gpi", "gpi");
    }

    @Test
    public void testBasic() {
        EpicsStatusService ess = new EpicsStatusService(cas, epicsTop, xml.getPath());

        //LOG.info("Service name: "+ess.getName());

        Channels lst = new Channels();
        lst.getSimpleChannelOrAlarmChannelOrHealthChannel().add(buildAlarmChannel("name1", "other name1", DataType.INT, "3"));
        lst.getSimpleChannelOrAlarmChannelOrHealthChannel().add(buildAlarmChannel("name2", "other name2", DataType.DOUBLE, "3.0"));
        lst.getSimpleChannelOrAlarmChannelOrHealthChannel().add(buildAlarmChannel("name3", "other name3", DataType.FLOAT, "3.0"));
        lst.getSimpleChannelOrAlarmChannelOrHealthChannel().add(buildAlarmChannel("name4", "other name4", DataType.STRING, "three"));


        ess.initialize(lst);

        ess.update(new AlarmStatus<Integer>("name1", 4, AlarmSeverity.ALARM_OK, AlarmCause.ALARM_CAUSE_OK));

        Set<String> testSet = new HashSet<String>();
        testSet.add("gpi:name1");
        testSet.add("gpi:name2");
        testSet.add("gpi:name3");
        testSet.add("gpi:name4");
        assertEquals(testSet, ess.getAlarmChannels().keySet());


        ess.shutdown();

        assertEquals(ess.getChannels().keySet(), new HashSet<String>());
    }

    @Test
    public void testFull() throws Exception {

        //initialize and check channels are created
        EpicsStatusServiceConfiguration essc = new EpicsStatusServiceConfiguration(xml.getPath());

        EpicsStatusService ess = new EpicsStatusService(cas, epicsTop, xml.getPath());
        ess.initialize();
        Map<String, AlarmChannel<?>> ac = ess.getAlarmChannels();

        Map<String, Channel<?>> nc = ess.getChannels();

        Map<String, Channel<String>> hc = ess.getHealthChannels();

        for (BaseChannelType cc : essc.getSimulatedChannels().getSimpleChannelOrAlarmChannelOrHealthChannel()) {
            if (cc instanceof HealthChannelType) {
                assertTrue(hc.keySet().contains(epicsTop.buildEpicsChannelName(cc.getGiapiname())));
            } else if (cc instanceof AlarmChannelType) {
                assertTrue(ac.keySet().contains(epicsTop.buildEpicsChannelName(cc.getGiapiname())));
            } else if (cc instanceof SimpleChannelType) {
                assertTrue(nc.keySet().contains(epicsTop.buildEpicsChannelName(cc.getGiapiname())));
            } else {
                fail();
            }
        }

        //update channels
        StatusItem<Integer> si = new BasicStatus<Integer>("giapinameint", 1);
        ess.update(si);

        StatusItem<Double> asi = new AlarmStatus<Double>("giapialarmdouble", 2.0, AlarmSeverity.ALARM_FAILURE, AlarmCause.ALARM_CAUSE_HI);
        ess.update(asi);

        StatusItem<Health> hsi = new HealthStatus("giapihealth1", Health.GOOD);
        ess.update(hsi);
        hsi = new HealthStatus("giapihealth1", Health.WARNING);
        ess.update(hsi);

        //shutdown and check channels get deleted
        ess.shutdown();

        assertEquals(ess.getAlarmChannels().keySet(), new HashSet<String>());
        assertEquals(ess.getChannels().keySet(), new HashSet<String>());

    }
}
