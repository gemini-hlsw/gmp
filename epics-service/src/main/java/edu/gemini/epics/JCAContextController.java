package edu.gemini.epics;

import com.cosylab.epics.caj.CAJContext;

/**
 * An JCAContextController holds the control of JCAContext taking care of building and disposing it
 */
public interface JCAContextController {
    /**
     * Returns a valid JCA context
     *
     * @return a valid JCA Context
     * @throws IllegalStateException In case the Context is not available
     */
    CAJContext getJCAContext() throws IllegalStateException;

    /**
     * Indicates if a JCA Context is available. Should be used before getJCAContext
     *
     * @return <code>true</code> if you can get a JCA Context, <code>false</code> otherwise
     */
    boolean isContextAvailable();

    /**
     * Retrieve default IO timeout from context.
     *
     * @return timeout value, in seconds
     */
    double timeout();


    /**
     * Retrieve default number of retries for channel read
     *
     * @return number of read retries.
     */
    int readRetries();
}
