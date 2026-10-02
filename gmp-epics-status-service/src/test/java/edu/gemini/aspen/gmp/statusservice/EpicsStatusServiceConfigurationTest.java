package edu.gemini.aspen.gmp.statusservice;

import edu.gemini.aspen.gmp.statusservice.generated.Channels;
import edu.gemini.aspen.gmp.statusservice.generated.DataType;
import edu.gemini.aspen.gmp.statusservice.generated.SimpleChannelType;
import org.junit.Test;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBException;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Class EpicsStatusServiceConfigurationTest
 *
 * @author Nicolas A. Barriga
 *         Date: 12/28/10
 */
public class EpicsStatusServiceConfigurationTest{
    private static final Logger LOG = Logger.getLogger(EpicsStatusServiceConfigurationTest.class.getName());
    public static final String xmlStr;

    static {
        BufferedReader in = new BufferedReader(new InputStreamReader(EpicsStatusServiceConfigurationTest.class.getResourceAsStream("giapi-epics-status-mapping.xml")));
        String xml = "";
        try {

            String line = in.readLine();
            while (line != null) {
                xml += line;
                line = in.readLine();

            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        xmlStr = xml;

    }

    @Test
    public void testBasic() throws JAXBException, SAXException {
        try {
            File xml = File.createTempFile("EpicsTest", ".xml");

            FileWriter xmlWrt = new FileWriter(xml);

            xmlWrt.write(xmlStr);
            xmlWrt.close();

            EpicsStatusServiceConfiguration ep = new EpicsStatusServiceConfiguration(xml.getPath());
            SimpleChannelType ch = new SimpleChannelType();
            ch.setGiapiname("giapinameint");
            ch.setEpicsname("epicsnameint");
            ch.setType(DataType.INT);
            ch.setInitial("2");

            Channels channels =new Channels();
            channels.getSimpleChannelOrAlarmChannelOrHealthChannel().add(ch);


            Channels channels2 = ep.getSimulatedChannels();
            SimpleChannelType ch2=(SimpleChannelType)channels2.getSimpleChannelOrAlarmChannelOrHealthChannel().get(0);
            assertEquals(ch2.getGiapiname(),ch.getGiapiname());
            assertEquals(ch2.getEpicsname(), ch.getEpicsname());
            assertEquals(ch2.getType(),ch.getType());
            assertEquals(ChannelsHelper.getInitial(ch2),ChannelsHelper.getInitial(ch));

        } catch (IOException ex) {
            fail();
        }
    }
}
