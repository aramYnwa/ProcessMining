package com.company.serializers;

import com.company.xlog.XLogHandler;
import java.util.List;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class AssosiationBasedSerializer extends ArffLogSerializer{

  public AssosiationBasedSerializer(XLogHandler handler) {
    super(handler);
  }

  @Override
  protected void createAttributes (List<String> file) {

    String relationName = "@RELATION " + logHandler.getFileName();
    file.add(relationName);
    file.add("\n");

    for (String attribute : logHandler.getAttributes()) {
      String arffAttr = "@ATTRIBUTE " + attribute + " {0, 1}";
      file.add(arffAttr);
    }

    // Add the label column which is binary
    // but in ARFF format there is no BINARY type.
    //String labelAttribute = "@ATTRIBUTE class {0, 1}";
    //file.add(labelAttribute);
    file.add("\n");
  }

  @Override
  protected void createData (List<String> file) {
    file.add("@DATA \n");


    XLog logFile = logHandler.getLogFile();
    for (XTrace trace : logFile) {
      file.add(" ");
      String instanceString = serializeTrace(trace);
      String arffInstance = instanceString.substring(1, instanceString.length() - 1);
      file.add(arffInstance);
    }
  }

  @Override
  protected void fillValue(List<Integer> instance, int index) {
      instance.set(index, 1);
  }
}
