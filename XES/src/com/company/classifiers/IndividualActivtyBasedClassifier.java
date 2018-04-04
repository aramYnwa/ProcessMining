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
