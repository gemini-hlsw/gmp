package edu.gemini.epics;

import gov.aps.jca.event.DirectEventDispatcher;

/**
 * Prepares the properties for CAJ to use `DirectEventDispatcher`. This is the default
 * EventDispatcher
 */
public class DirectEventDispatcherSelector implements EventDispatcherSelector {
    @Override
    public void prepareForBuild() {
        System.setProperty(gov.aps.jca.Context.class.getName() + ".event_dispatcher",
            DirectEventDispatcher.class.getName());
    }
}
