package com.company.classifiers;

import com.company.xlog.XLogHandler;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public abstract class IndividualActivtyBasedClassifier extends ArffLogClassifier{

  public IndividualActivtyBasedClassifier(XLogHandler handler) {
    super(handler);
  }

  public void serialize() {
    // We will collect all string data to this list
    // At the end will write the array to the file
    List<String> arffFile = new ArrayList<>();

    createAttributes(arffFile);
    createData(arffFile);
    writeToArff(arffFile);
  }


  private void createAttributes (List<String> file) {

    String relationName = "@RELATION " + logHandler.getFileName();
    file.add(relationName);
    file.add("\n");

    for (String attribute : logHandler.getAttributes()) {
      String arffAttr = "@ATTRIBUTE " + attribute + " NUMERIC";
      file.add(arffAttr);
    }

    // Add the label column which is binary
    // but in ARFF format there is no BINARY type.
    String labelAttribute = "@ATTRIBUTE class {0, 1}";
    file.add(labelAttribute);
    file.add("\n");
  }


  private void createData (List<String> file) {
    file.add("@DATA \n");

    XLog logFile = logHandler.getLogFile();
    for (XTrace trace : logFile) {
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
  private String classifyTrace(XTrace trace) {
    long traceExecutionTime = logHandler.getTraceDuration(trace);
    if (traceExecutionTime > averageTime)
      return "1";
    else
      return "0";
  }

  private String serializeTrace(XTrace trace) {
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
      Files.write(arffPath, file);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  protected void fillValue(List<Integer> instance, int index) {

  }

}
