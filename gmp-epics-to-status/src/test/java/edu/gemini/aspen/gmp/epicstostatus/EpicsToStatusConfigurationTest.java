package edu.gemini.aspen.gmp.epicstostatus;

import edu.gemini.aspen.gmp.epicstostatus.generated.Channels;
import edu.gemini.aspen.gmp.epicstostatus.generated.SimpleChannelType;
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
public class EpicsToStatusConfigurationTest {
    private static final Logger LOG = Logger.getLogger(EpicsToStatusConfigurationTest.class.getName());
    public static final String xmlStr;


    static {
        BufferedReader in = new BufferedReader(new InputStreamReader(EpicsToStatusConfigurationTest.class.getResourceAsStream("gmp-epics-to-status-mapping.xml")));
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

            EpicsToStatusConfiguration ep = new EpicsToStatusConfiguration(xml.getPath());


            Channels channels = ep.getSimulatedChannels();
            SimpleChannelType ch = (SimpleChannelType) channels.getSimpleChannelOrAlarmChannelOrHealthChannel().get(0);
            assertEquals("giapitest:statusitemint", ch.getStatusitem());
            assertEquals("giapitest:epicschannelint", ch.getEpicschannel());
            assertEquals(new Integer(0), ch.getIndex());

        } catch (IOException ex) {
            fail();
        }
    }

    @Test
    public void testNoIndex() throws JAXBException, SAXException {
        try {
            File xml = File.createTempFile("EpicsTest", ".xml");

            FileWriter xmlWrt = new FileWriter(xml);

            xmlWrt.write(xmlStr);
            xmlWrt.close();

            EpicsToStatusConfiguration ep = new EpicsToStatusConfiguration(xml.getPath());


            Channels channels = ep.getSimulatedChannels();
            SimpleChannelType ch = (SimpleChannelType) channels.getSimpleChannelOrAlarmChannelOrHealthChannel().get(1);
            assertEquals("giapitest:giapialarmfloat", ch.getStatusitem());
            assertEquals("giapitest:epicsalarmfloat", ch.getEpicschannel());
            assertEquals(null, ch.getIndex());

        } catch (IOException ex) {
            fail();
        }
    }
}
