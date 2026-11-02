package edu.gemini.isd.scorpio.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * A dictionary class that maps the Giapi names and the frontend names
 */
public class StatusNameDictionary {
    private static final Map<String, String> dictionary = new HashMap<>();

    public static void addToDictionary(String giapi, String translated) {
        if (giapi != null && translated != null){
            dictionary.put(giapi, translated);
        }
    }

    /**
     * Returns the translation of the giapi name to the frontend name, if the name is not found returns the nameToTranslate
     * @param nameToTranslate the giapi name
     * @return the frontend name
     */
    public static String translate(String nameToTranslate){
        return dictionary.getOrDefault(nameToTranslate, nameToTranslate);
    }
}
