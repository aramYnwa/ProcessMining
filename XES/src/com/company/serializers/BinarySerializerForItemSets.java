package com.company.serializers;

import java.util.ArrayList;
import org.deckfour.xes.model.XLog;

public class BinarySerializerForItemSets {
  private ArrayList<ArrayList<String>> features;
  private XLog logFile;

  public BinarySerializerForItemSets (XLog logFile, ArrayList<ArrayList<String>> features) {
    this.logFile = logFile;
    this.features = features;
  }

  public void serialize() {

  }
}
