package edu.gemini.epics.osgi;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.gemini.epics.*;
import edu.gemini.epics.api.EpicsClient;
import edu.gemini.epics.impl.EpicsClientSubscriber;
import edu.gemini.epics.impl.EpicsObserverImpl;
import edu.gemini.epics.impl.EpicsReaderImpl;
import edu.gemini.epics.impl.EpicsWriterImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

public class EpicsServiceFactory implements ManagedServiceFactory {
    private static final Logger LOG = Logger.getLogger(EpicsServiceFactory.class.getName());
    private static final String PROPERTY_ADDRESS_LIST = "addressList";
    private static final String PROPERTY_IO_TIMEOUT = "ioTimeout";
    private static final String PROPERTY_READ_RETRIES = "readRetries";
    private static final double DEFAULT_TIMEOUT = 1.0;
    private static final int DEFAULT_READ_RETRIES = 0;

    private final Map<String, EpicsServices> existingServices = Maps.newHashMap();
    private final BundleContext context;

    private class ServiceRef<T> {
        private final ServiceRegistration<?> serviceRegistration;
        private final T service;

        private ServiceRef(ServiceRegistration<?> serviceRegistration, T service) {
            this.serviceRegistration = serviceRegistration;
            this.service = service;
        }
    }

    private class EpicsServices {
        private final ServiceRef<EpicsService> epicsService;
        private final ServiceRef<EpicsWriter> epicsWriter;
        private final ServiceRef<EpicsReader> epicsReader;
        private final ServiceRef<EpicsObserverImpl> epicsObserver;
        private final ServiceTracker<EpicsClient, EpicsClient> clientServiceTracker;

        private EpicsServices(ServiceRef<EpicsService> epicsService, ServiceRef<EpicsWriter> epicsWriter, ServiceRef<EpicsReader> epicsReader, ServiceRef<EpicsObserverImpl> epicsObserver, ServiceTracker<EpicsClient, EpicsClient> clientServiceTracker) {
            this.epicsService = epicsService;
            this.epicsWriter = epicsWriter;
            this.epicsReader = epicsReader;
            this.epicsObserver = epicsObserver;
            this.clientServiceTracker = clientServiceTracker;
        }
    }

    private void changedAddress(EpicsService epicsService, Dictionary<String, ?> properties) {
        epicsService.setAddress(properties.get(PROPERTY_ADDRESS_LIST).toString());
    }

    private void changedTimeout(EpicsService epicsService, Dictionary<String, ?> properties) {
        if (properties.get(PROPERTY_IO_TIMEOUT) != null) {
            try {
                epicsService.setTimeout(Double.parseDouble(properties.get(PROPERTY_IO_TIMEOUT).toString()));
            } catch (NumberFormatException e) {
            }
        }
    }

    private void changedReadRetries(EpicsService epicsService, Dictionary<String, ?> properties) {
        if (properties.get(PROPERTY_READ_RETRIES) != null) {
            try {
                epicsService.setReadRetries(Integer.parseInt(properties.get(PROPERTY_READ_RETRIES).toString()));
            } catch (NumberFormatException e) {
            }
        }
    }

    EpicsServiceFactory(BundleContext context) {
        this.context = context;
    }

    public String getName() {
        return "EpicsService factory";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) {
        if (existingServices.containsKey(pid)) {
            EpicsService epicsService = existingServices.get(pid).epicsService.service;
            changedAddress(epicsService, properties);
            changedTimeout(epicsService, properties);
            changedReadRetries(epicsService, properties);
        } else {
            if (checkProperties(properties)) {
                EpicsService epicsService = createService(properties);
                epicsService.startService();
                ServiceRegistration<?> esRegistration = context.registerService(JCAContextController.class, epicsService, new Hashtable<>());

                EpicsWriter epicsWriter = new EpicsWriterImpl(epicsService);
                ServiceRegistration<?> ewRegistration = context.registerService(EpicsWriter.class, epicsWriter, new Hashtable<>());

                EpicsReader epicsReader = new EpicsReaderImpl(epicsService);
                ServiceRegistration<?> erRegistration = context.registerService(EpicsReader.class, epicsReader, new Hashtable<>());

                EpicsObserverImpl epicsObserver = new EpicsObserverImpl(epicsService);
                epicsObserver.startObserver();
                ServiceRegistration<?> eoRegistration = context.registerService(EpicsObserver.class, epicsObserver, new Hashtable<>());

                final EpicsClientSubscriber epicsClientSubscriber = new EpicsClientSubscriber(epicsObserver);

                ServiceTracker<EpicsClient, EpicsClient> clientServiceTracker = new ServiceTracker<>(context, EpicsClient.class, new ServiceTrackerCustomizer<EpicsClient, EpicsClient>() {
                    @Override
                    public EpicsClient addingService(ServiceReference<EpicsClient> reference) {
                        EpicsClient epicsClient = context.getService(reference);
                        HashMap<String, Object> properties = Maps.newHashMap();
                        for (String key : reference.getPropertyKeys()) {
                            properties.put(key, reference.getProperty(key));
                        }
                        epicsClientSubscriber.bindEpicsClient(epicsClient, ImmutableMap.copyOf(properties));
                        return epicsClient;
                    }

                    @Override
                    public void modifiedService(ServiceReference<EpicsClient> epicsClientServiceReference, EpicsClient epicsClient) {

                    }

                    @Override
                    public void removedService(ServiceReference<EpicsClient> epicsClientServiceReference, EpicsClient epicsClient) {
                        epicsClientSubscriber.unbindEpicsClient(epicsClient);
                    }
                });
                clientServiceTracker.open(true);

                existingServices.put(pid, new EpicsServices(new ServiceRef<>(esRegistration, epicsService), new ServiceRef<>(ewRegistration, epicsWriter), new ServiceRef<>(erRegistration, epicsReader), new ServiceRef<>(eoRegistration, epicsObserver), clientServiceTracker));
            } else {
                LOG.warning("Cannot build " + EpicsService.class.getName() + " without the required properties");
            }
        }
    }

    private EpicsService createService(Dictionary<String, ?> properties) {
        String addressList = properties.get(PROPERTY_ADDRESS_LIST).toString();
        double timeout;
        try {
            timeout = Double.parseDouble(properties.get(PROPERTY_IO_TIMEOUT).toString());
        } catch (NumberFormatException e) {
            timeout = DEFAULT_TIMEOUT;
        }
        int readRetries;
        try {
            readRetries = Integer.parseInt(properties.get(PROPERTY_READ_RETRIES).toString());
        } catch (NumberFormatException e) {
            readRetries = DEFAULT_READ_RETRIES;
        }
        return new EpicsService(addressList, timeout, readRetries);
    }

    private boolean checkProperties(Dictionary<String, ?> properties) {
        return properties.get(PROPERTY_ADDRESS_LIST) != null &&
                properties.get(PROPERTY_IO_TIMEOUT) != null;
    }

    @Override
    public void deleted(String pid) {
        if (existingServices.containsKey(pid)) {
            ServiceRef<EpicsObserverImpl> eoReference = existingServices.get(pid).epicsObserver;
            eoReference.serviceRegistration.unregister();
            eoReference.service.stopObserver();

            ServiceRef<EpicsService> reference = existingServices.get(pid).epicsService;
            reference.serviceRegistration.unregister();
            reference.service.stopService();

            ServiceRef<EpicsWriter> ewReference = existingServices.get(pid).epicsWriter;
            ewReference.serviceRegistration.unregister();

            ServiceRef<EpicsReader> erReference = existingServices.get(pid).epicsReader;
            erReference.serviceRegistration.unregister();

            existingServices.get(pid).clientServiceTracker.close();

            existingServices.remove(pid);
        }
    }

    public void stopServices() {
        for (String pid: existingServices.keySet()) {
            deleted(pid);
        }
    }

}
