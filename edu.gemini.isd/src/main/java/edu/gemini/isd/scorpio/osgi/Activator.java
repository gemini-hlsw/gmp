package edu.gemini.isd.scorpio.osgi;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.isd.scorpio.ConfigurationReader;
import edu.gemini.isd.scorpio.controller.WebsocketController;
import edu.gemini.isd.scorpio.status.StatusCacheHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import java.util.Set;

public class Activator implements BundleActivator {
    private ServiceRegistration<StatusHandler> statusHandlerRegistration;
    private final WebsocketController websocketController = new WebsocketController();

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("scorpio-isd: Hello World - bundle started");

        Set<String> statusItems = ConfigurationReader.loadStatusItems(context);
        StatusCacheHandler statusHandler = new StatusCacheHandler(statusItems);
        statusHandlerRegistration = context.registerService(StatusHandler.class, statusHandler, null);

        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());

        try {
            websocketController.start(statusHandler);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (statusHandlerRegistration != null) {
            statusHandlerRegistration.unregister();
            statusHandlerRegistration = null;
            websocketController.stop();
        }
        System.out.println("scorpio-isd: Goodbye - bundle stopped");
    }
}
