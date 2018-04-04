package com.company.serializers;

import com.company.xlog.XLogHandler;
import java.util.List;

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
    String labelAttribute = "@ATTRIBUTE class {0, 1}";
    file.add(labelAttribute);
    file.add("\n");
  }

  @Override
  protected void fillValue(List<Integer> instance, int index) {
      instance.set(index, 1);
  }
}
