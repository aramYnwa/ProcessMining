package com.company.serializers;

import com.company.xlog.XLogHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public abstract class ArffLogSerializer {
  protected Path arffPath;
  protected long averageTime;
  protected ArrayList<String> attributes;
  protected XLogHandler logHandler;

  public ArffLogSerializer(XLogHandler logHandler) {
    this.logHandler = logHandler;
    attributes = new ArrayList<>();
    attributes = logHandler.getAttributes();
    averageTime = logHandler.getAverageTraceTime();
  }

  public void serialize() {
    // We will collect all string data to this list
    // At the end will write the array to the file
    List<String> arffFile = new ArrayList<>();

    createAttributes(arffFile);
    createData(arffFile);
    writeToArff(arffFile);
  }

  protected void createAttributes (List<String> file) {

  }

  protected void createData (List<String> file) {
    file.add("@DATA \n");


    XLog logFile = logHandler.getLogFile();
    for (XTrace trace : logFile) {
      file.add(" ");
      String label = ", " + classifyTrace(trace);
      String instanceString = serializeTrace(trace);
      String arffInstance = instanceString.substring(1, instanceString.length() - 1);
      arffInstance += label;
      file.add(arffInstance);
    }
  }


  private int getIndexOfEvent(XEvent event) {
    String eventName = XConceptExtension.instance().extractName(event);
    eventName = eventName.replaceAll("[^A-Za-z0-9 ]", "");
    eventName = eventName.replaceAll(" ", "_").toLowerCase();
    Integer index = attributes.indexOf(eventName);
    return index;
  }

  /**
   * Our goal is examine slower traces. We classify by "1" traces which is slow (lasts longer then
   * average) We classify by "0" traces which is fast.
   */
  protected String classifyTrace(XTrace trace) {
    long traceExecutionTime = logHandler.getTraceDuration(trace);
    if (traceExecutionTime > averageTime)
      return "1";
    else
      return "0";
  }

  protected String serializeTrace(XTrace trace) {
    Integer length = attributes.size();
    List<Integer> instance = new ArrayList<>(Collections.nCopies(length, 0));
    for (XEvent event : trace) {
      int index = getIndexOfEvent(event);
      fillValue(instance, index);
    }
    return instance.toString();
  }

  private void writeToArff(List<String> file) {
    try {
      arffPath = Paths.get(logHandler.getFileName() + ".arff");
      Files.write(arffPath, file);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  public Path getArffPath() {
    return arffPath;
  }

  protected void fillValue(List<Integer> instance, int index) {

  }
}
