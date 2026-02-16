package edu.gemini.isd.scorpio.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.gemini.isd.scorpio.utils.StatusNameDictionary;
import org.osgi.framework.BundleContext;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationReader {
    private static final Logger LOG = Logger.getLogger(ConfigurationReader.class.getName());
    private static final String STATUS_CONFIG_RELATIVE_PATH = "services/edu.gemini.isd.statusItemsConfiguration.json";
    //private static final String ACTIVE_INSTRUMENT = "SCO";

    /**
     * This method process the configuration file to a string Set.
     * First looks for the ActiveInstrument value and search inside the key with the same value of ActiveInstrument and collect every defined item
     * @param context of the bundle
     * @return the set of Status Items
     */
    public static Set<String> loadStatusItems(BundleContext context) {
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

    /**
     * This method iterates through the JSON object of the instrument
     * @param instrumentObj the object of the active instrument
     * @param items list to add the Status Items
     */
    private static void collectItems(JsonObject instrumentObj, Set<String> items) {
        // 1. Subsystem: CC, DC, IS...
        for (String subsystemKey : instrumentObj.keySet()) {
            JsonObject subsystemObj = instrumentObj.getAsJsonObject(subsystemKey);
            if (subsystemObj == null) continue;

            // 2. Component: cover, ADC, CRYO...
            for (String componentKey : subsystemObj.keySet()) {
                JsonObject componentObj = subsystemObj.getAsJsonObject(componentKey);
                if (componentObj == null) continue;

                // 3. Status: enabled, cover...
                for (String statusKey : componentObj.keySet()) {
                    JsonObject statusObj = componentObj.getAsJsonObject(statusKey);
                    if (statusObj == null) continue;

                    // 4. names: giapi & front-end
                    addArrayItems(statusObj, items);
                    sendToDictionary(statusObj);
                }
            }
        }
    }

    private static void addArrayItems(JsonObject obj, Set<String> items) {
        JsonElement element = obj.get("giapi");
        if (element != null && element.isJsonPrimitive()) {
            items.add(element.getAsString());
        }
    }

    private static void sendToDictionary(JsonObject obj) {
        JsonElement element = obj.get("giapi");
        JsonElement elementTranslated = obj.get("front-end");

        StatusNameDictionary.addToDictionary(element.getAsString(), elementTranslated.getAsString());
    }
}
