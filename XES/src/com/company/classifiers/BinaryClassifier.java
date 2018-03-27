package com.company.classifiers;

import com.company.xlog.XLogHandler;
import java.util.List;

public class BinaryClassifier extends IndividualActivtyBasedClassifier {

  public BinaryClassifier (XLogHandler handler) {
    super(handler);
  }

  @Override
  protected void fillValue(List<Integer> instance, int index) {
    instance.set(index, 1);
  }
}
