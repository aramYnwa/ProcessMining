package com.company.serializers;

import com.company.xlog.XLogHandler;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class AssosiationBasedBinarySerializer extends ArffLogSerializer {
  private ArrayList<ArrayList<String>> attributes;

  public AssosiationBasedBinarySerializer(XLogHandler handler,
      ArrayList<ArrayList<String>>  attributes) {
    super(handler);
    this.attributes = attributes;
  }

  @Override
  protected void createAttributes (List<String> file) {
    String relationName = "@RELATION " + logHandler.getFileName();
    file.add(relationName);
    file.add("\n");

    for (ArrayList arrayList : attributes) {
      String attributeName = arrayList.toString();
      attributeName =  attributeName.replaceAll(", ", "&");
      String arffAttr = "@ATTRIBUTE " + attributeName + " NUMERIC";
      file.add(arffAttr);
    }

    String labelAttribute = "@ATTRIBUTE class {0, 1}";
    file.add(labelAttribute);
    file.add("\n");
  }

  @Override
  protected void createData (List<String> file) {
    file.add("@DATA \n");

    Integer length = attributes.size();

    XLog logFile = logHandler.getLogFile();
    ArrayList<ArrayList<String>> logFileString = logHandler.getLog();


    for (int i = 0; i < logFile.size(); ++i) {
      ArrayList<String> logInstance = logFileString.get(i);
      XTrace trace = logFile.get(i);
      List<Integer> instance = new ArrayList<>(Collections.nCopies(length, 0));
      String labelOfTrace = classifyTrace(trace);

      for (int j = 0; j < attributes.size(); ++j) {
        ArrayList<String> itemSet = attributes.get(j);
        for (String item : itemSet) {
          if (logInstance.contains(item))
            instance.set(attributes.indexOf(itemSet), 1);
        }
      }

      String instanceToString = instance.toString();
      String arffInstance = instanceToString.substring(1, instanceToString.length() - 1);
      arffInstance += ", " + labelOfTrace;
      file.add(arffInstance);

    }
  }
}
