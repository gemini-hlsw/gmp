package edu.gemini.isd.scorpio.models;

import java.util.HashMap;
import java.util.Map;

public class StatusNameDictionary {
    private static final Map<String, String> dictionary = new HashMap<>();

    public static void addToDictionary(String giapi, String translated) {
        dictionary.put(giapi, translated);
    }

    public static String translate(String nameToTranslate){
        return dictionary.get(nameToTranslate);
    }
}
