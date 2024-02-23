package edu.gemini.aspen.gmp.tcsoffset.model;

import edu.gemini.epics.EpicsException;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;

import java.util.logging.Logger;

import static edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetException.Error.CONFIGURATION_FILE;

public class ChannelAccessFactory {
    private static final Logger LOG = Logger.getLogger(ChannelAccessFactory.class.getName());

    public static  ChannelAccess createChannelAccess(String caname, EpicsWriter ew1, int type) {
        try {
            switch (type) {
                case 0:
                    return new ChannelAcessShort(caname, ew1, type);
                case 1:
                    return new ChannelAcessInt(caname, ew1, type);
                case 2:
                    return new ChannelAccessFloat(caname, ew1, type);
                case 3:
                    return new ChannelAcessDouble(caname, ew1, type);
                case 4:
                    return new ChannelAccessString(caname, ew1, type);
                case 5: // enum short
                    return new ChannelAcessEnum(caname, ew1, type);
                default:
                    // This Exception will stop the service because there is an error at configuration level file.
                    throw new TcsOffsetException(CONFIGURATION_FILE,
                            "Bad file configuration. The " + caname +
                                    " channel access has not a correct type. The allowed types are: " +
                                    "0 (short), 1(enum), 2(float), 3(double), 4(int), 5(string)");
            }
        } catch (IllegalArgumentException | EpicsException e) {
            // This exception shouldn't affect the service to be deployed.
            LOG.warning("Epics Channel Access was not created. Name: "+ caname +
                    ". Please check if the IOC is running or the EPICS server list");
        }
        return null;
    }
}
