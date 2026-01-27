package edu.gemini.isd.scorpio.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import io.javalin.Javalin;

/**
 * Minimal BundleActivator that prints a Hello World message when started/stopped.
 */
public class Activator implements BundleActivator {

    private Javalin app;

    @Override
    public void start(BundleContext context) throws Exception {
        // Use System.out so the message appears in the Felix console/log output reliably
        System.out.println("scorpio-isd: Hello World - bundle started");

        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());
        try {
            app = Javalin.create()
                    .get("/", ctx -> ctx.result("hello world"))
                    .start(7000);
            System.out.println("scorpio-isd: Javalin started on http://localhost:7000/");
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (app != null) {
            app.stop();
        }
        System.out.println("scorpio-isd: Goodbye - bundle stopped");
    }
}
