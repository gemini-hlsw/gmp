package edu.gemini.aspen.gmp.commands.model.executors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.aspen.gmp.commands.model.*;

import java.util.HashMap;
import java.util.Map;

class MockActionMessageBuilder implements ActionMessageBuilder {
    private class MockActionMessage implements ActionMessage {
        private ImmutableMap<String, Object> props;
        private HashMap<String, Object> configurationElements;

        public MockActionMessage(Action action, ConfigPath path) {
            props = ImmutableMap.<String, Object>of(
                    JmsKeys.GMP_ACTIVITY_PROP, action.getCommand().getActivity().getName(),
                    JmsKeys.GMP_ACTIONID_PROP, action.getId());

            configurationElements = Maps.newHashMap();

            //Store the configuration elements that
            //matches this config path.
            Configuration c = action.getCommand().getConfiguration();
            c = c.getSubConfiguration(path);

            for (ConfigPath cp : c.getKeys()) {
                configurationElements.put(cp.getName(), c.getValue(cp));
            }
        }


        @Override
        public String getDestinationName() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Map<String, Object> getProperties() {
            return props;
        }

        @Override
        public Map<String, Object> getDataElements() {
            return configurationElements;
        }
    }

    @Override
    public ActionMessage buildActionMessage(Action action) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ActionMessage buildActionMessage(Action action, ConfigPath path) {
        return new MockActionMessage(action, path);
    }
}

