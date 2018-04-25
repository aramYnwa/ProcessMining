package com.company.xlog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class XLogHandler {
  private HashMap<String, Integer> attributeDictionary;
  private String fileName;
  private XLog logFile;
  private ArrayList<String> attributes;
  private ArrayList<ArrayList<String>> log;
  private Long averageTraceTime;

  public XLogHandler(XLog xlogFile, String logFileName) {
    log = new ArrayList<>();
    fileName = logFileName;
    logFile = xlogFile;
    attributeDictionary = new HashMap<>();
    attributes = createAttributeDictionary();
    averageTraceTime = calculateAverageTime(logFile);
  }

  private ArrayList<String> createAttributeDictionary () {
    System.out.println("Creating event dictionary");
    for (XTrace trace : logFile) {
      ArrayList <String> traceString = new ArrayList<>();
      for (XEvent event : trace) {
        XAttributeMap attributes = event.getAttributes();
        String conceptName = XConceptExtension.KEY_NAME;
        if (attributes.containsKey(conceptName))
        {
          String key = attributes.get(conceptName).toString();
          key = key.replaceAll("[^A-Za-z0-9 ]", "");
          key = key.replaceAll(" ", "_").toLowerCase();
          traceString.add(key);
          if (attributeDictionary.containsKey(key)) {
            Integer frequency = attributeDictionary.get(key);
            attributeDictionary.put(key, frequency + 1);
          } else {
            attributeDictionary.put(key, 1);
          }
        }
      }
      log.add(traceString);
    }
    System.out.println("Attributes dictionary is ready");
    return new ArrayList<>(attributeDictionary.keySet());
  }

  private Long calculateAverageTime(XLog xlog) {
    long accumulator = 0;
    long temp = 0;
    for (XTrace trace : xlog) {
      long seconds = getTraceDuration(trace);
      accumulator += seconds;
      temp++;

    }
    return accumulator / temp;
  }

  public long getTraceDuration(XTrace trace) {
    int length = trace.size();
    XEvent firstEvent = trace.get(0);
    Date firstEventTS = XTimeExtension.instance().extractTimestamp(firstEvent);

    XEvent lastEvent = trace.get(length - 1);
    Date lastEventTS = XTimeExtension.instance().extractTimestamp(lastEvent);

    //Getting difference by days.
    long days = (lastEventTS.getTime() - firstEventTS.getTime()) / (1000 * 60 * 60 * 24);

    return days;
  }

  public String getFileName() {
    return fileName;
  }

  public ArrayList<String> getAttributes() {
    return attributes;
  }

  public Long getAverageTraceTime() {
    return averageTraceTime;
  }

  public XLog getLogFile() {
    return logFile;
  }

  public ArrayList<ArrayList<String>> getLog() {
    return log;
  }
}
