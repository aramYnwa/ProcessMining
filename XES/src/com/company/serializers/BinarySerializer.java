package com.company.serializers;

import com.company.xlog.XLogHandler;
import java.util.List;

public class BinarySerializer extends IndividualActivtyBasedSerializer {

  public BinarySerializer (XLogHandler handler) {
    super(handler);
  }

  @Override
  protected void fillValue(List<Integer> instance, int index) {
    instance.set(index, 1);
  }
}
