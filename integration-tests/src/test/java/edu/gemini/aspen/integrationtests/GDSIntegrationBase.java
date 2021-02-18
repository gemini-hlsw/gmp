package edu.gemini.aspen.integrationtests;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import edu.gemini.aspen.gds.api.fits.HeaderItem;
import edu.gemini.aspen.gds.fits.FitsReader;
import edu.gemini.aspen.giapi.data.DataLabel;
import edu.gemini.aspen.giapi.data.ObservationEvent;
import edu.gemini.aspen.giapi.data.ObservationEventHandler;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.WrappedUrlProvisionOption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.ops4j.pax.exam.CoreOptions.*;
import static scala.jdk.CollectionConverters.seqAsJavaList;

/**
 * Base class for the integration tests related to the GDS
 */
public class GDSIntegrationBase extends FelixContainerConfigurationBase {
    protected static final String FINAL_FITS_FILE = "sample1.fits";
    protected static final String INITIAL_FITS_FILE = "sample1.fits";
    protected static final String INITIAL_FITS_DIR = "/tmp/";
    protected static final String FINAL_FITS_DIR = "/tmp/perm/";

    @Before
    public void removeFiles() {
        removeTestFile(FINAL_FITS_DIR + FINAL_FITS_FILE);
        removeTestFile(INITIAL_FITS_DIR + INITIAL_FITS_FILE);
    }

    @Before
    public void createDirs() {
        new File(INITIAL_FITS_DIR).mkdirs();
        new File(FINAL_FITS_DIR).mkdirs();
    }

    @Configuration
    Option[] gdsBundles() {
        return concatenate(super.baseContainerConfig(), options(
                systemProperty("org.osgi.service.http.port").value("9999"),
                mavenBundle().artifactId("giapi").groupId("edu.gemini.aspen").versionAsInProject(),
                mavenBundle().artifactId("giapi-jms-util").groupId("edu.gemini.aspen").versionAsInProject(),
                mavenBundle().artifactId("fits").groupId("edu.gemini.external.osgi.nom.tam").versionAsInProject(),
                mavenBundle().artifactId("scala-library").groupId("org.scala-lang").versionAsInProject(),
                mavenBundle().artifactId("scala-actors").groupId("org.scala-lang").versionAsInProject(),
                mavenBundle().artifactId("gds-api").groupId("edu.gemini.aspen.gds").versionAsInProject(),
                mavenBundle().artifactId("jms-api").groupId("edu.gemini.jms").versionAsInProject(),
                mavenBundle().artifactId("gmp-services").groupId("edu.gemini.aspen.gmp").versionAsInProject(),
                mavenBundle().artifactId("gds-keywords-database").groupId("edu.gemini.aspen.gds").versionAsInProject(),
                mavenBundle().artifactId("gds-performance-monitoring").groupId("edu.gemini.aspen.gds").versionAsInProject(),
                mavenBundle().artifactId("gds-actors-composer").groupId("edu.gemini.aspen.gds").versionAsInProject(),
                mavenBundle().artifactId("gds-fits-updater").groupId("edu.gemini.aspen.gds").versionAsInProject(),
                mavenBundle().artifactId("gds-observation-state-publisher").groupId("edu.gemini.aspen.gds").versionAsInProject(),
                mavenBundle().artifactId("gds-obsevent-handler").groupId("edu.gemini.aspen.gds").versionAsInProject(),
                mavenBundle().artifactId("pax-web-jetty-bundle").groupId("org.ops4j.pax.web").versionAsInProject(),
                mavenBundle().artifactId("pax-web-extender-whiteboard").groupId("org.ops4j.pax.web").versionAsInProject(),
                mavenBundle().artifactId("pax-web-spi").groupId("org.ops4j.pax.web").versionAsInProject(),
                mavenBundle().artifactId("pax-web-descriptor").groupId("org.ops4j.pax.web").versionAsInProject(),
                mavenBundle().artifactId("pax-web-api").groupId("org.ops4j.pax.web").versionAsInProject(),
                mavenBundle().artifactId("xbean-finder").groupId("org.apache.xbean").versionAsInProject(),
                mavenBundle().artifactId("xbean-bundleutils").groupId("org.apache.xbean").versionAsInProject(),
                mavenBundle().artifactId("asm").groupId("org.ow2.asm").versionAsInProject(),
                mavenBundle().artifactId("asm-commons").groupId("org.ow2.asm").versionAsInProject(),
                mavenBundle().artifactId("asm-debug-all").groupId("org.ow2.asm").versionAsInProject(),
                mavenBundle().artifactId("ecj").groupId("edu.gemini.external.osgi.org.eclipse.jdt.core.compiler").versionAsInProject(),
                wrappedBundle(mavenBundle().artifactId("casdb").groupId("edu.gemini.cas").versionAsInProject()).overwriteManifest(WrappedUrlProvisionOption.OverwriteMode.FULL)
         ));
    }

    @Override
    protected String confDir() {
        return "/src/test/resources/conf/services";
    }

    private void removeTestFile(String fileName) {
        File finalFile = new File(fileName);
        if (finalFile.exists()) {
            finalFile.delete();
        }
    }

    void copyInitialFile() throws IOException {
        copyInitialFile(INITIAL_FITS_FILE, INITIAL_FITS_DIR + INITIAL_FITS_FILE);
    }

    private void copyInitialFile(String src, String dest) throws IOException {
        ByteStreams.copy(getClass().getResourceAsStream(src), new FileOutputStream(dest));
    }

    void sendObservationEvents(ObservationEventHandler eventHandler, DataLabel dataLabel) throws InterruptedException {
        eventHandler.onObservationEvent(ObservationEvent.OBS_PREP, dataLabel);

        TimeUnit.MILLISECONDS.sleep(200);
        eventHandler.onObservationEvent(ObservationEvent.OBS_START_ACQ, dataLabel);

        TimeUnit.MILLISECONDS.sleep(200);
        eventHandler.onObservationEvent(ObservationEvent.OBS_END_ACQ, dataLabel);

        TimeUnit.MILLISECONDS.sleep(200);
        eventHandler.onObservationEvent(ObservationEvent.OBS_START_READOUT, dataLabel);

        TimeUnit.MILLISECONDS.sleep(200);
        eventHandler.onObservationEvent(ObservationEvent.OBS_END_READOUT, dataLabel);

        TimeUnit.MILLISECONDS.sleep(200);
        eventHandler.onObservationEvent(ObservationEvent.OBS_START_DSET_WRITE, dataLabel);

        TimeUnit.MILLISECONDS.sleep(200);
        eventHandler.onObservationEvent(ObservationEvent.OBS_END_DSET_WRITE, dataLabel);

        TimeUnit.MILLISECONDS.sleep(300);
    }

    Set<String> readFinalKeywords() {
        return readKeywords(FINAL_FITS_DIR + FINAL_FITS_FILE, 0);
    }

    private Set<String> readKeywords(String fileName, int header) {
        FitsReader reader = new FitsReader(new File(fileName));
        List<HeaderItem<?>> keyList = seqAsJavaList(reader.header(header).get().keywords());
        Set<String> set = Sets.newTreeSet();
        for (HeaderItem<?> h:keyList) {
            set.add(h.keywordName().key());
        }
        return set;
    }

    protected List<Set<String>> readAllExtensionsKeywords(String fileName, int headersCount) {
        List<Set<String>> extensions = Lists.newArrayList();
        for (int i=0;i<headersCount;i++) {
            extensions.add(readKeywords(fileName, i));
        }
        return extensions;
    }

    Set<String> readOriginalKeywords() {
        return readKeywords(INITIAL_FITS_DIR + INITIAL_FITS_FILE, 0);
    }
}
