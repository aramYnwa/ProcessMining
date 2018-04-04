package com.company.serializers;

import com.company.xlog.XLogHandler;
import java.util.List;

public abstract class IndividualActivtyBasedSerializer extends ArffLogSerializer{

  public IndividualActivtyBasedSerializer(XLogHandler handler) {
    super(handler);
  }

  @Override
  protected void createAttributes (List<String> file) {

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

}
