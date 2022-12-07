package edu.gemini.aspen.gmp.health;

import edu.gemini.aspen.giapi.status.impl.HealthStatus;
import edu.gemini.aspen.giapi.status.setter.StatusSetter;
import edu.gemini.gmp.top.Top;
import edu.gemini.jms.api.JmsArtifact;
import edu.gemini.jms.api.JmsProvider;

import javax.jms.JMSException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class Health evaluates the health of the GMP
 */
public class Health implements JmsArtifact {
    private static final Logger LOG = Logger.getLogger(Health.class.getName());
    private final StatusSetter statusSetter;
    private final Top top;
    private final String healthStatusName;
    private final BundlesDatabase bundlesDatabase;

    private final ScheduledThreadPoolExecutor executor;
    private ScheduledFuture future;
    private final HealthChecker checker = new HealthChecker();

    private edu.gemini.aspen.giapi.status.Health health = null;

    public Health(String healthStatusName,
            Top top, StatusSetter statusSetter, BundlesDatabase bundlesDatabase) {
        LOG.info("Health Constructor on status " + healthStatusName);
        this.top = top;
        this.statusSetter = statusSetter;
        this.healthStatusName = healthStatusName;
        this.bundlesDatabase = bundlesDatabase;
        executor = new ScheduledThreadPoolExecutor(1);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    protected void setupHealthValue() {
        try {
            edu.gemini.aspen.giapi.status.Health health = edu.gemini.aspen.giapi.status.Health.GOOD;
            if (bundlesDatabase.getPercentageActive().get() < 1.0) {
                health = edu.gemini.aspen.giapi.status.Health.WARNING;
            }
            if (!health.equals(this.health)) {
                if (statusSetter.setStatusItem(new HealthStatus(top.buildStatusItemName(healthStatusName), health))) {
                    LOG.info("GMP Health changed to " + health);
                    this.health = health;
                }
            }
        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Error setting up health", e);
        }
    }

    @Override
    public void startJms(JmsProvider provider) throws JMSException {
        LOG.info("Start GMP Health");
        future = executor.scheduleAtFixedRate(checker, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stopJms() {
        LOG.info("Stop GMP Health");
        if (!future.isCancelled()) {
            future.cancel(false);
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public void stopService() {
        stopJms();
    }

    private class HealthChecker implements Runnable {

        @Override
        public void run() {
            setupHealthValue();
        }
    }
}
