package com.company;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.*;

import java.util.*;

public class AttributeDictionary {
    private String dbname;
    private HashMap<String, Integer> attributeDictionary;


    public AttributeDictionary (String logInfo, XLog logFile) {
        dbname = logInfo;
        attributeDictionary = new HashMap<>();
        System.out.println("Creating event dictionary");
        for (XTrace trace : logFile) {
            for (XEvent event : trace) {
                XAttributeMap attributes = event.getAttributes();
                String conceptName = XConceptExtension.KEY_NAME;
                if (attributes.containsKey(conceptName))
                {
                    String key = attributes.get(conceptName).toString();
                    key = key.replaceAll("[^A-Za-z0-9 ]", "");
                    key = key.replaceAll(" ", "_").toLowerCase();
                    //XAttribute code = attributes.get("Activity code");
                    if (attributeDictionary.containsKey(key)) {
                        Integer frequency = attributeDictionary.get(key);
                        attributeDictionary.put(key, frequency + 1);
                    } else {
                        attributeDictionary.put(key, 1);
                    }
                }
            }
        }
        System.out.println("Attributes dictionary is ready");
    }

    public String getDbname() {
        return dbname;
    }

    public HashMap<String, Integer> getAttributeDictionary() {
        return attributeDictionary;
    }
}
