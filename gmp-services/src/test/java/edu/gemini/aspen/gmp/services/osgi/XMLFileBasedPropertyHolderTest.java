package edu.gemini.aspen.gmp.services.osgi;

import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XMLFileBasedPropertyHolderTest {
    @Test
    public void readProperties() {
        XMLFileBasedPropertyHolder propertyHolder = new XMLFileBasedPropertyHolder(System.getProperty("user.dir") + "/src/test/resources/gmp-properties.xml");

        assertEquals(propertyHolder.getProperty("GMP_HOST_NAME"), "localhost");
        assertEquals(propertyHolder.getProperty("DHS_ANCILLARY_DATA_PATH"), "/home/anunez/tmp");
        assertEquals(propertyHolder.getProperty("DHS_SCIENCE_DATA_PATH"), "/home/anunez/tmp");
        assertEquals(propertyHolder.getProperty("DHS_INTERMEDIATE_DATA_PATH"), "/home/anunez/tmp");
        assertEquals(propertyHolder.getProperty("DEFAULT"), "");
    }

}
