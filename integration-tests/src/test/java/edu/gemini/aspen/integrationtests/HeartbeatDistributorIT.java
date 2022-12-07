// package edu.gemini.aspen.integrationtests;
//
// import edu.gemini.aspen.heartbeatdistributor.HeartbeatConsumer;
// import edu.gemini.jms.api.JmsProvider;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.ops4j.pax.exam.Option;
// import org.ops4j.pax.exam.junit.Configuration;
// import org.ops4j.pax.exam.junit.JUnit4TestRunner;
// import org.ops4j.pax.exam.options.WrappedUrlProvisionOption;
//
// import java.util.concurrent.TimeUnit;
//
// import static org.junit.Assert.assertNotNull;
// import static org.junit.Assert.assertTrue;
// import static org.ops4j.pax.exam.CoreOptions.*;
//
// /**
//  * Class HeartbeatDistributorIT
//  *
//  * @author Nicolas A. Barriga
//  *         Date: 3/10/11
//  */
// @RunWith(JUnit4TestRunner.class)
// public class HeartbeatDistributorIT extends FelixContainerConfigurationBase {
//     @Configuration
//     public static Option[] withJMSProviderConfig() {
//         return options(
//                 mavenBundle().artifactId("giapi").groupId("edu.gemini.aspen").versionAsInProject(),
//                 mavenBundle().artifactId("jms-api").groupId("edu.gemini.jms").versionAsInProject(),
//                 mavenBundle().artifactId("giapi-jms-util").groupId("edu.gemini.aspen").versionAsInProject(),
//                 mavenBundle().artifactId("jms-activemq-provider").groupId("edu.gemini.jms").versionAsInProject(),
//                 mavenBundle().artifactId("jms-activemq-broker").groupId("edu.gemini.jms").versionAsInProject(),
//                 mavenBundle().artifactId("eproperties").groupId("edu.gemini.external.osgi.net.jmatrix.eproperties").versionAsInProject(),
//                 mavenBundle().artifactId("activemq-core").groupId("org.apache.activemq").versionAsInProject(),
//                 mavenBundle().artifactId("geronimo-j2ee-management_1.1_spec").groupId("org.apache.geronimo.specs").versionAsInProject(),
//                 mavenBundle().artifactId("kahadb").groupId("org.apache.activemq").versionAsInProject(),
//                 mavenBundle().artifactId("geronimo-annotation_1.0_spec").groupId("org.apache.geronimo.specs").versionAsInProject(),
//                 mavenBundle().artifactId("gmp-top").groupId("edu.gemini.aspen.gmp").update().versionAsInProject(),
//                 mavenBundle().artifactId("gmp-heartbeat").groupId("edu.gemini.aspen.gmp").update().versionAsInProject(),
//                 mavenBundle().artifactId("gmp-heartbeat-distributor-service").groupId("edu.gemini.aspen").update().versionAsInProject(),
//                 mavenBundle().artifactId("joda-time").groupId("joda-time").update().versionAsInProject(),
//                 mavenBundle().artifactId("giapi-status-dispatcher").groupId("edu.gemini.aspen").update().versionAsInProject(),
//                 wrappedBundle(maven().artifactId("integration-tests").groupId("edu.gemini.aspen").versionAsInProject()).overwriteManifest(WrappedUrlProvisionOption.OverwriteMode.FULL)
//         );
//     }
//
//     @Override
//     protected String confDir() {
//         return "/src/test/resources/conf/services";
//     }
//
//     @Test
//     public void bundleExistence() {
//         assertNotNull(getBundle("edu.gemini.aspen.heartbeat-distributor-service"));
//     }
//
//     @Test
//     public void checkNotification() throws Exception {
//         TimeUnit.MILLISECONDS.sleep(400);
//         //register handlers
//         JmsProvider provider = (JmsProvider) context.getService(context.getServiceReference("edu.gemini.jms.api.JmsProvider"));
//
//         TestConsumerComponent comp = new TestConsumerComponent(2);
//         context.registerService(HeartbeatConsumer.class.getName(), comp, null);
//
//         //wait at most 3 second for 2 beats to arrive
//         comp.waitOnLatch(3, TimeUnit.SECONDS);
//         assertTrue(comp.getLast() > 0);
//         assertTrue(comp.getCount() >= 2);
//
//     }
// }
