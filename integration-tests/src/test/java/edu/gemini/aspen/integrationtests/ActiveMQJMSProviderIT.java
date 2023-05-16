package edu.gemini.aspen.integrationtests;

import edu.gemini.jms.api.JmsProvider;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import javax.inject.Inject;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

/**
 * Basic integration test for JMS Provider basically verifying that the service starts and can be configured

 * For testing it uses pax-exam to create a felix based OSGi container.
 *
 * <a href="http://paxrunner.ops4j.org/display/paxexam/Advanced+JUnit+usage">Pax-examn</a>
 *
 * @author cquiroz
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ActiveMQJMSProviderIT extends FelixContainerConfigurationBase {
    @Inject
    private BundleContext context;

    @Configuration
    public Option[] baseConfig() {
        return concatenate(super.baseContainerConfig(), options(
                mavenBundle().artifactId("activemq-core").groupId("org.apache.activemq").versionAsInProject(),
                mavenBundle().artifactId("geronimo-j2ee-management_1.1_spec").groupId("org.apache.geronimo.specs").versionAsInProject(),
                mavenBundle().artifactId("kahadb").groupId("org.apache.activemq").versionAsInProject(),
                mavenBundle().artifactId("geronimo-annotation_1.0_spec").groupId("org.apache.geronimo.specs").versionAsInProject(),
                mavenBundle().artifactId("jms-api").groupId("edu.gemini.jms").versionAsInProject(),
                mavenBundle().artifactId("jms-activemq-provider").groupId("edu.gemini.jms").versionAsInProject()
        ));
    }

    protected String confDir() {
        return "/src/test/resources/conf/jms-provider";
    }

    @Test
    public void bundleExistence() {
        assertNotNull(getJmsProviderBundle());
    }

    private Bundle getJmsProviderBundle() {
        for (Bundle b : context.getBundles()) {
            if ("edu.gemini.jms.activemq-provider".equals(b.getSymbolicName())) {
                return b;
            }
        }
        fail("Bundle not found");
        throw new AssertionError();
    }

    @Test
    public void doesServiceExist() {
        assertTrue(isJMSProviderAvailable());
        JmsProvider provider = findJmsProvider();
        // WARN extra dependency on ActiveMQ API
        assertEquals("failover:(tcp://localhost:51616)", ((ActiveMQConnectionFactory)provider.getConnectionFactory()).getBrokerURL());

    }

    private boolean isJMSProviderAvailable() {
        ServiceReference providerReference = context.getServiceReference(JmsProvider.class.getName());
        return providerReference != null;
    }

    private JmsProvider findJmsProvider() {
        ServiceReference providerReference = context.getServiceReference(JmsProvider.class.getName());
        return (JmsProvider)context.getService(providerReference);
    }

}
