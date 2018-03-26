package com.company.xlog;

import java.util.HashMap;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;


//FIXME :Make XLongHandler singleton
public class XLogHandler {
  private HashMap<String, Integer> attributeDictionary;
  private String fileName;
  private XLog logFile;

  public XLogHandler(XLog logFile, String fileName) {
    this.fileName = fileName;
    this.logFile = logFile;
    this.attributeDictionary = new HashMap<>();
    createAttributeDictionary();
  }

  private void createAttributeDictionary () {
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

  public String getFileName() {
    return fileName;
  }
  
  public HashMap<String, Integer> getAttributeDictionary() {
    return attributeDictionary;
  }
}
