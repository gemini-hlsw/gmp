package edu.gemini.aspen.giapi.statusservice;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;

import java.util.logging.Logger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Status Handler Manager acts as a register for status handlers
 * and also as a receiver of updates when status items are updated. It will
 * notify the Status Handlers registered whenever a new status item is
 * received. It assumes all the status handlers registered are interested
 * in all the status items for which the Status Service is registered.
 * Filtering/processing of status item is delegated to the status handlers
 * themselves. 
 */
public class StatusHandlerManager implements StatusHandler, StatusHandlerRegister {

    private static final Logger LOG = Logger.getLogger(StatusHandlerManager.class.getName());

    private final List<StatusHandler> _statusHandlers = new CopyOnWriteArrayList<StatusHandler>();
    private static final String STATUS_HANDLER_NAME = "Status Handler Manager";

    public StatusHandlerManager() {

    }

    public String getName() {
        return STATUS_HANDLER_NAME;
    }

    public void update(StatusItem item) {
        for (StatusHandler handler: _statusHandlers) {
            handler.update(item);
        }
    }

    public void addStatusHandler(StatusHandler handler) {
        _statusHandlers.add(handler);
        LOG.info("Status Handler Registered: " + handler);
    }

    public void removeStatusHandler(StatusHandler handler) {
        _statusHandlers.remove(handler);
        LOG.info("Removed Status Handler: " + handler);

    }
}
