package edu.gemini.aspen.gmp.tcsoffset.model;

import edu.gemini.epics.api.EpicsClient;
import java.util.List;
import java.util.logging.Logger;

public class ChannelAccessSubscribe implements EpicsClient{

    private PtrMethod _ptrMethod;
    private String _caName;
    private static final Logger LOG = Logger.getLogger(ChannelAccessSubscribe.class.getName());

    public ChannelAccessSubscribe(PtrMethod ptr, String CA_name) {
        _ptrMethod = ptr;
	_caName = CA_name;
    }

    public <T> void valueChanged(String channel, List<T> values) {
        _ptrMethod.func(values);

    }

    public void connected() {
        LOG.fine("Subscribed to the " + _caName + " epics channel access");
    }

    public void disconnected() {
        LOG.fine("Disconnected to the " + _caName + " epics channel access" );
    }

}
