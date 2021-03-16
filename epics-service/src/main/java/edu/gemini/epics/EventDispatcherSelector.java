package edu.gemini.epics;

import gov.aps.jca.event.DirectEventDispatcher;
import gov.aps.jca.event.LatestMonitorOnlyQueuedEventDispatcher;
import gov.aps.jca.event.QueuedEventDispatcher;

public interface EventDispatcherSelector {
    void prepareForBuild();
}


