package edu.gemini.aspen.integrationtests;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.dispatcher.FilteredStatusHandler;
import edu.gemini.aspen.giapi.status.dispatcher.StatusDispatcher;
import edu.gemini.aspen.giapi.status.impl.BasicStatus;
import edu.gemini.aspen.giapi.status.setter.StatusSetter;
import edu.gemini.aspen.giapi.statusservice.StatusHandlerAggregate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.WrappedUrlProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class StatusSetterIT extends FelixContainerConfigurationBase {

    @Configuration
    public Option[] withStatusDbAndJMSProviderConfig() {
        return concatenate(super.baseContainerConfig(), options(
                mavenBundle().artifactId("giapi").groupId("edu.gemini.aspen").versionAsInProject(),
                mavenBundle().artifactId("jms-api").groupId("edu.gemini.jms").versionAsInProject(),
                mavenBundle().artifactId("giapi-jms-util").groupId("edu.gemini.aspen").versionAsInProject(),
                mavenBundle().artifactId("gmp-statusdb").groupId("edu.gemini.aspen.gmp").versionAsInProject(),
                mavenBundle().artifactId("jms-activemq-provider").groupId("edu.gemini.jms").versionAsInProject(),
                mavenBundle().artifactId("jms-activemq-broker").groupId("edu.gemini.jms").versionAsInProject(),
                mavenBundle().artifactId("activemq-core").groupId("org.apache.activemq").versionAsInProject(),
                mavenBundle().artifactId("geronimo-j2ee-management_1.1_spec").groupId("org.apache.geronimo.specs").versionAsInProject(),
                mavenBundle().artifactId("kahadb").groupId("org.apache.activemq").versionAsInProject(),
                mavenBundle().artifactId("geronimo-annotation_1.0_spec").groupId("org.apache.geronimo.specs").versionAsInProject(),
                mavenBundle().artifactId("giapi-status-service").groupId("edu.gemini.aspen").update().versionAsInProject(),
                mavenBundle().artifactId("giapi-status-dispatcher").groupId("edu.gemini.aspen").update().versionAsInProject(),
                mavenBundle().artifactId("gmp-status-gateway").groupId("edu.gemini.aspen.gmp").update().versionAsInProject(),
                mavenBundle().artifactId("gmp-heartbeat").groupId("edu.gemini.aspen.gmp").update().versionAsInProject(),
                mavenBundle().artifactId("giapi-status-setter").groupId("edu.gemini.aspen.giapi").update().versionAsInProject(),
                mavenBundle().artifactId("gmp-heartbeat-distributor-service").groupId("edu.gemini.aspen").update().versionAsInProject(),
                wrappedBundle(maven().artifactId("giapi-test-support").groupId("edu.gemini.aspen").versionAsInProject()).overwriteManifest(WrappedUrlProvisionOption.OverwriteMode.FULL),
                wrappedBundle(maven().artifactId("integration-tests").groupId("edu.gemini.aspen").versionAsInProject()).overwriteManifest(WrappedUrlProvisionOption.OverwriteMode.FULL)
        ));
    }

    @Override
    protected String confDir() {
        return "/src/test/resources/conf/services";
    }

//    @Test
    public void bundleExistence() {
        assertNotNull(getStatusDispatcherBundle());
        assertTrue(isStatusDispatcherRunning());
    }

    private Bundle getStatusDispatcherBundle() {
        for (Bundle b : context.getBundles()) {
            if ("edu.gemini.aspen.giapi-status-dispatcher".equals(b.getSymbolicName())) {
                return b;
            }
        }
        fail();
        return null;
    }

    private boolean isStatusDispatcherRunning() {
        Bundle statusDispatcherBundle = getStatusDispatcherBundle();
        assert statusDispatcherBundle != null;
        for (ServiceReference s : statusDispatcherBundle.getRegisteredServices()) {
            if (s.isAssignableTo(statusDispatcherBundle, StatusDispatcher.class.getName())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void checkSending() throws Exception {
        //register handlers
        TestHandler testHandler1 = new TestHandler();
        context.registerService(FilteredStatusHandler.class.getName(), testHandler1, null);
        StatusSetter setter = (StatusSetter) context.getService(context.getServiceReference(StatusSetter.class.getName()));
        assertNotNull(setter);
        assertNotNull(context.getService(context.getServiceReference(StatusHandlerAggregate.class.getName())));
        assertNotNull(context.getService(context.getServiceReference(StatusHandler.class.getName())));

        // Wait a bit for the services to be registered before sending the status update
        TimeUnit.MILLISECONDS.sleep(1200);

        //send StatusItem update via JMS
        setter.setStatusItem(new BasicStatus<>("gpi:status1", "bla"));

        //wait for messages to arrive and assert
        assertTrue(testHandler1.waitOnLatch(1, TimeUnit.SECONDS));
        Assert.assertEquals(1, testHandler1.getCounter());
    }

}
