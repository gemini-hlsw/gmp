package edu.gemini.epics;

import gov.aps.jca.event.LatestMonitorOnlyQueuedEventDispatcher;

/**
 * Prepares the properties for CAJ to use `LatestMonitorOnlyQueuedEventDispatcher`. Its parameters are:
 * <p>
 * Integer queueLimit: maximum size of the event queue. Defaults is 100.
 * Integer limit: maximum number of pending events in the queue for a given channel. Default is 5.
 * Integer priority: Thread priority of the thread executing the callbacks. Default is Thread.NORM_PRIORITY
 * String monitorOutput: File name, stdout or stderr. This output will be used to log size of the event queue. Default
 * value is null, and no output is generated.
 * <p>
 * All the parameters are optional. If not set, CAJ will use default values.
 */
public class LatestMonitorOnlyQueuedEventDispatcherSelector implements EventDispatcherSelector {
    Integer queueLimit;
    Integer limit;
    Integer priority;
    String monitorOutput;

    @Override
    public void prepareForBuild() {
        System.setProperty(gov.aps.jca.Context.class.getName() + ".event_dispatcher",
            LatestMonitorOnlyQueuedEventDispatcher.class.getName());
        if (queueLimit != null)
            System.setProperty(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".queue_limit", queueLimit.toString());
        if (limit != null)
            System.setProperty(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".channel_queue_limit", limit.toString());
        if (priority != null)
            System.setProperty(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".priority", priority.toString());
        if (monitorOutput != null)
            System.setProperty(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".monitor_output", monitorOutput);
    }

    LatestMonitorOnlyQueuedEventDispatcherSelector setQueueLimit(Integer queueLimit) {
        this.queueLimit = queueLimit;
        return this;
    }

    LatestMonitorOnlyQueuedEventDispatcherSelector setPerChannelLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    LatestMonitorOnlyQueuedEventDispatcherSelector setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    LatestMonitorOnlyQueuedEventDispatcherSelector setMonitorOutput(String monitorOutput) {
        this.monitorOutput = monitorOutput;
        return this;
    }

}
