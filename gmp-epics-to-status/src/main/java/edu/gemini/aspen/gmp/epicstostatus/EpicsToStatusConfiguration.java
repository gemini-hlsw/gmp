package edu.gemini.aspen.gmp.epicstostatus;

import edu.gemini.aspen.gmp.epicstostatus.generated.Channels;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

/**
 * An Epics Configuration built on top of the OSGI properties infraestructure
 */
public class EpicsToStatusConfiguration {
    private final Channels _simChannels;

    public EpicsToStatusConfiguration(String confFileName) throws JAXBException, SAXException {
        JAXBContext jc = JAXBContext.newInstance(Channels.class);
        Unmarshaller um = jc.createUnmarshaller();
        SchemaFactory factory =
                SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema schema = factory.newSchema(this.getClass().getResource("gmp-epics-to-status-mapping.xsd"));
        um.setSchema(schema); //to enable validation
        _simChannels = (Channels) um.unmarshal(new File(confFileName));
    }

    public Channels getSimulatedChannels() {
        return _simChannels;
    }

}