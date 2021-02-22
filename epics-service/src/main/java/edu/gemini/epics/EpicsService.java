package edu.gemini.epics;

import com.cosylab.epics.caj.CAJContext;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import gov.aps.jca.CAException;
import gov.aps.jca.JCALibrary;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The EpicsService is a Component that has a reference to a JCA Context
 * implementing {@link JCAContextController} which is used by many other service
 *
 * EpicsService is in charge of creating the JCA Context and manage its life cycle
 */
public class EpicsService implements JCAContextController {
    private static final Logger LOG = Logger.getLogger(EpicsService.class.getName());
    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private String _addressList;

    // Timeout in seconds
    private double ioTimeout = 1.0;

    // Number of read retries
    private int readRetries = 0;

    private CAJContext _ctx;

    public EpicsService(String addressList, double ioTimeout, int readRetries) {
        LOG.info("EpicsService created with " + addressList + ", IO timeout is " + ioTimeout + "[s]");
        validateAddressToConnect(addressList);
        this._addressList = addressList;
        this.ioTimeout = ioTimeout;
        this.readRetries = readRetries;
    }

    public EpicsService(CAJContext context) {
        Preconditions.checkArgument(context != null, "JCAContext cannot be null");
        _ctx = context;
        this._addressList = null;
    }

    private void validateAddressToConnect(String addressList) {
        Preconditions.checkArgument(addressList != null, "Address to connect cannot be null");
        String[] adresses = addressList.split("\\s");
        for (String addr: adresses) {
            Preconditions.checkArgument(addr.matches(IPADDRESS_PATTERN), "Address should be an IP address: " + addr);
        }
    }

    @Override
    public CAJContext getJCAContext() {
        if (!isContextAvailable()) {
            throw new IllegalStateException("Illegal State, attempt to read the Context before it has been made available");
        }
        return _ctx;
    }

    public boolean isContextAvailable() {
        return _ctx != null;
    }

    public void setAddress(String addressList) {
        if (addressList != null) {
            this._addressList = addressList;
            LOG.fine("Address List changed to " + this._addressList);
        }
    }

    public void setTimeout(double ioTimeout) {
        this.ioTimeout = ioTimeout;
        LOG.fine("Default IO timeout changed to " + this.ioTimeout);
    }

    public void setReadRetries(int retries) {
        this.readRetries = retries;
        LOG.fine("Default read retries changed to " + this.readRetries);
    }

    /**
     * Called when the service is stopped, it informs the known clients and closes the context
     */
    public void stopService() {
        if (isContextAvailable()) {
            try {
                _ctx.destroy();
                _ctx = null;
            } catch (CAException e) {
                LOG.log(Level.SEVERE, "Exception destroying the JCA Context", e);
            }
        }
    }

    /**
     * Starts the context and binds IEpicClients already found
     */
    public void startService() {
        if (isContextAvailable()) {
            stopService();
        }
        validateAddressToConnect(_addressList);

        System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", _addressList);
        System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false");

        try {
            _ctx = (CAJContext)JCALibrary.getInstance().createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
            _ctx.initialize();
            LOG.fine("JCALibrary built connecting to: " + _addressList);

            logContextInfo();
        } catch (CAException e) {
            LOG.log(Level.SEVERE, "Cannot start JCALibrary", e);
        }
        LOG.fine("Epics Service started on : " + _addressList);
    }

    private void logContextInfo() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bos);
        _ctx.printInfo(out);

        String ctxInfo = new String(bos.toByteArray());
        Iterable<String> ctxInfoLines = Splitter.on('\n').trimResults().split(ctxInfo);
        for (String line : ctxInfoLines) {
            if (!line.isEmpty()) {
                LOG.info("JCALibrary Info: " + line);
            }
        }
    }

    @Override
    public double timeout() {
        return ioTimeout;
    }

    @Override
    public int readRetries() {
        return readRetries;
    }

}
