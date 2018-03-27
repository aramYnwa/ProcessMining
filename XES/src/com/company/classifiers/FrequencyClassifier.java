package com.company.classifiers;

import com.company.xlog.XLogHandler;
import java.util.List;

public class FrequencyClassifier extends IndividualActivtyBasedClassifier {

  public FrequencyClassifier (XLogHandler handler) {
    super(handler);
  }

  @Override
  protected void fillValue(List<Integer> instance, int index) {
    int value = instance.get(index);
    instance.set(index, value + 1);
  }
}
