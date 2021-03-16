package edu.gemini.epics;

import gov.aps.jca.event.QueuedEventDispatcher;

/**
 * Prepares the properties for CAJ to use `QueuedEventDispatcher`. Its parameters are:
 * <p>
 * Integer queueLimit: maximum size of the event queue. Defaults is 100.
 * Integer limit: maximum number of pending events in the queue for a given channel. Default is 5.
 * Integer priority: Thread priority of the thread executing the callbacks. Default is Thread.NORM_PRIORITY
 * <p>
 * All the parameters are optional. If not set, CAJ will use default values.
 */
public class QueuedEventDispatcherSelector implements EventDispatcherSelector {
    Integer queueLimit;
    Integer limit;
    Integer priority;

    @Override
    public void prepareForBuild() {
        System.setProperty(gov.aps.jca.Context.class.getName() + ".event_dispatcher",
            QueuedEventDispatcher.class.getName());
        if (queueLimit != null)
            System.setProperty(QueuedEventDispatcher.class.getName() + ".queue_limit", queueLimit.toString());
        if (limit != null)
            System.setProperty(QueuedEventDispatcher.class.getName() + ".channel_queue_limit", limit.toString());
        if (priority != null)
            System.setProperty(QueuedEventDispatcher.class.getName() + ".priority", priority.toString());
    }

    QueuedEventDispatcherSelector setQueueLimit(Integer queueLimit) {
        this.queueLimit = queueLimit;
        return this;
    }

    QueuedEventDispatcherSelector setPerChannelLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    QueuedEventDispatcherSelector setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

}
