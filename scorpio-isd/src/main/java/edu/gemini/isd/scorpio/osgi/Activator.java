package edu.gemini.isd.scorpio.osgi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.isd.scorpio.status.StatusCacheHandler;
import io.javalin.Javalin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minimal BundleActivator that prints a Hello World message when started/stopped.
 */
public class Activator implements BundleActivator {

    private static final Logger LOG = Logger.getLogger(Activator.class.getName());
    private static final String STATUS_CONFIG_RELATIVE_PATH = "services/isdStatus.cfg";

    private Javalin app;
    private ServiceRegistration<StatusHandler> statusHandlerRegistration;
    private StatusCacheHandler statusHandler;
    private final Gson gson = new Gson();

    @Override
    public void start(BundleContext context) throws Exception {
        // Use System.out so the message appears in the Felix console/log output reliably
        System.out.println("scorpio-isd: Hello World - bundle started");

        Set<String> statusItems = loadStatusItems(context);
        statusHandler = new StatusCacheHandler(statusItems);
        statusHandlerRegistration = context.registerService(StatusHandler.class, statusHandler, null);

        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());
        try {
            app = Javalin.create()
                    .get("/", ctx -> ctx.result(gson.toJson(statusHandler.snapshot())))
                    .start(7000);
            System.out.println("scorpio-isd: Javalin started on http://localhost:7000/");
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (statusHandlerRegistration != null) {
            statusHandlerRegistration.unregister();
            statusHandlerRegistration = null;
        }
        if (app != null) {
            app.stop();
        }
        System.out.println("scorpio-isd: Goodbye - bundle stopped");
    }

    private Set<String> loadStatusItems(BundleContext context) {
        String confBase = context.getProperty("conf.base");
        if (confBase == null || confBase.trim().isEmpty()) {
            confBase = "src/main/config";
        }
        Path configPath = Paths.get(confBase, STATUS_CONFIG_RELATIVE_PATH);
        if (!Files.exists(configPath)) {
            LOG.warning("Status config not found at " + configPath + ". No status items will be tracked.");
            return Collections.emptySet();
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String instrument = getString(root, "instrumentActive");
            if (instrument == null || instrument.trim().isEmpty()) {
                LOG.warning("instrumentActive not found in status config. No status items will be tracked.");
                return Collections.emptySet();
            }

            JsonObject instrumentObj = root.getAsJsonObject(instrument);
            if (instrumentObj == null) {
                LOG.warning("Instrument '" + instrument + "' not found in status config. No status items will be tracked.");
                return Collections.emptySet();
            }

            Set<String> items = new LinkedHashSet<>();
            collectItems(instrumentObj, items);
            return items;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to read status config. No status items will be tracked.", ex);
            return Collections.emptySet();
        }
    }

    private static String getString(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        return element.getAsString();
    }

    private static void collectItems(JsonObject obj, Set<String> items) {
        for (String key : obj.keySet()) {
            JsonElement value = obj.get(key);
            if (value == null) {
                continue;
            }
            if (value.isJsonArray()) {
                addArrayItems(value.getAsJsonArray(), items);
            } else if (value.isJsonObject()) {
                JsonObject nested = value.getAsJsonObject();
                for (String nestedKey : nested.keySet()) {
                    JsonElement nestedValue = nested.get(nestedKey);
                    if (nestedValue != null && nestedValue.isJsonArray()) {
                        addArrayItems(nestedValue.getAsJsonArray(), items);
                    }
                }
            }
        }
    }

    private static void addArrayItems(JsonArray array, Set<String> items) {
        for (JsonElement element : array) {
            if (element != null && element.isJsonPrimitive()) {
                items.add(element.getAsString());
            }
        }
    }
}
