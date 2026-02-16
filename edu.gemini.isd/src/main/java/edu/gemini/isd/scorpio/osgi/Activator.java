package edu.gemini.isd.scorpio.osgi;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.isd.scorpio.config.ConfigurationReader;
import edu.gemini.isd.scorpio.controller.WebsocketController;
import edu.gemini.isd.scorpio.handler.StatusCacheHandler;
import edu.gemini.isd.scorpio.repository.StatusRepository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import java.util.Set;

public class Activator implements BundleActivator {
    private ServiceRegistration<StatusHandler> statusHandlerRegistration;
    private WebsocketController websocketController;
    private final StatusRepository statusRepository = new StatusRepository();

    @Override
    public void start(BundleContext context) throws Exception {
        // Bring the configuration and register as a service the handler
        Set<String> statusItems = ConfigurationReader.loadStatusItems(context);
        StatusCacheHandler statusHandler = new StatusCacheHandler(statusItems, statusRepository);
        statusHandlerRegistration = context.registerService(StatusHandler.class, statusHandler, null);

        // Save the original ClassLoader and forces the thread to use the Activator's class loader to load the dependencies of this module
        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());

        try {
            websocketController = new WebsocketController(statusHandler);
            websocketController.start(7000);
        } finally {
            // Restore the original ClassLoader
            Thread.currentThread().setContextClassLoader(originalCl);
        }
        System.out.println("scorpio-isd: bundle started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (statusHandlerRegistration != null) {
            statusHandlerRegistration.unregister();
            statusHandlerRegistration = null;
        }

        websocketController.stop();
        System.out.println("scorpio-isd: bundle stopped");
    }
}
