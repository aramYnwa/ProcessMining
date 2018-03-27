package com.company.classifiers;

import com.company.translation.XLogToArff;
import com.company.xlog.XLogHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public abstract class ArffLogClassifier {
  protected Path arffPath;
  protected long averageTime;
  protected ArrayList<String> attributes;
  protected XLogHandler logHandler;

  public ArffLogClassifier(XLogHandler logHandler) {
    this.logHandler = logHandler;
    attributes = new ArrayList<>();
    attributes = logHandler.getAttributes();
    arffPath = Paths.get(logHandler.getFileName() + "new.arff");
    averageTime = logHandler.getAverageTraceTime();
  }


}
