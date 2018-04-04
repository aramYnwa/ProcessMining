package com.company.serializers;

import com.company.xlog.XLogHandler;
import java.util.List;

public class FrequencySerializer extends IndividualActivtyBasedSerializer {

  public FrequencySerializer (XLogHandler handler) {
    super(handler);
  }

  @Override
  protected void fillValue(List<Integer> instance, int index) {
    int value = instance.get(index);
    instance.set(index, value + 1);
  }
}
