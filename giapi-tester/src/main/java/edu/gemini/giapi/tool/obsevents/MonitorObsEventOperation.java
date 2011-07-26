package edu.gemini.giapi.tool.obsevents;

import edu.gemini.giapi.tool.parser.Operation;
import edu.gemini.giapi.tool.parser.Argument;
import edu.gemini.giapi.tool.arguments.HostArgument;
import edu.gemini.giapi.tool.arguments.MonitorObsEventArgument;
import edu.gemini.aspen.giapi.data.obsevents.jms.JmsObservationEventListener;
import edu.gemini.aspen.giapi.data.ObservationEventHandler;
import edu.gemini.aspen.giapi.data.ObservationEvent;
import edu.gemini.aspen.giapi.data.DataLabel;
import edu.gemini.jms.activemq.provider.ActiveMQJmsProvider;
import edu.gemini.jms.api.JmsProvider;
import edu.gemini.jms.api.BaseMessageConsumer;
import edu.gemini.jms.api.DestinationData;
import edu.gemini.jms.api.DestinationType;

import javax.jms.JMSException;
import java.util.logging.Logger;

/**
 * An operation to receive Observation Events.
 */
public class MonitorObsEventOperation implements Operation {


    private static final Logger LOG = Logger.getLogger(MonitorObsEventOperation.class.getName());

    private String _host = "localhost";

    private boolean _isReady = false;

    public void setArgument(Argument arg) {
        if (arg instanceof MonitorObsEventArgument) {
            _isReady = true;
        } else if (arg instanceof HostArgument) {
            _host = ((HostArgument) arg).getHost();
        }
    }

    public boolean isReady() {
        return _isReady;
    }

    public int execute() throws Exception {

        JmsProvider provider = new ActiveMQJmsProvider("tcp://" + _host + ":61616");

        JmsObservationEventListener listener = new JmsObservationEventListener(new TestObsEventHandler());

        BaseMessageConsumer consumer = new BaseMessageConsumer(
                "Observation Event Test Client",
                new DestinationData(JmsObservationEventListener.TOPIC_NAME,
                        DestinationType.TOPIC),
                listener
        );

        try {
            consumer.startJms(provider);
            Thread.sleep(Long.MAX_VALUE);//wait for events until somebody quits the application
        } catch (JMSException e) {
            LOG.warning("Problem on GIAPI tester: " + e.getMessage());
        }
        return 0;
    }

    public class TestObsEventHandler implements ObservationEventHandler {
        public void onObservationEvent(ObservationEvent event, DataLabel dataLabel) {
            System.out.println("[" + dataLabel.getName() + "/" + event + "]");
        }
    }
}
