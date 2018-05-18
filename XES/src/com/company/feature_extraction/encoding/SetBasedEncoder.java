package com.company.feature_extraction.encoding;

import java.util.ArrayList;
import java.util.HashMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class SetBasedEncoder {

  EncodingType encodingType = null;
  XLogManager xLogManager = null;
  XLog xLog = null;
  Instances encodedTraces = null;

  public SetBasedEncoder(XLog log, EncodingType encodingType) {
    this.xLog = log;
    this.xLogManager = new XLogManager(log);
    this.encodingType = encodingType;
  }

  /**
   *
   */
  public void encodeTraces() {
    HashMap<XTrace, Double> traceLabelMap = xLogManager.traceLabelMap;
    ItemSetExtracter itemSetExtracter = new ItemSetExtracter(xLog);
    ArrayList<ArrayList<String>> frequentItemsets = itemSetExtracter.extractItemSets();

    if (frequentItemsets == null) {
      System.out.println("Empty frequent itemsets");
    }

    Integer frequentItemsetsCount = frequentItemsets.size();

    //Starting set based encoding.
    ArrayList<Attribute> attributes = new ArrayList<>();
    for (int i = 0; i < frequentItemsets.size(); ++i) {
      attributes.add(null);
    }

    int i = 0;
    for (ArrayList<String> itemSet : frequentItemsets) {
      String setName = itemSet.toString();
      Attribute attribute = new Attribute(setName, i);
      attributes.set(i, attribute);
      i++;
    }

    ArrayList<String> labelValues = new ArrayList<>();
    labelValues.add("0");
    labelValues.add("1");

    Attribute label = new Attribute("label", labelValues);
    attributes.add(label);

    encodedTraces = new Instances("DATA", attributes, xLog.size());

    i = 0;
    for (XTrace trace : xLog) {
      Instance instance = new DenseInstance(frequentItemsetsCount + 1);

      for (int j = 0; j < frequentItemsetsCount; ++j) {
        instance.setValue(j, 0);
      }

      HashMap<String, Integer> eventFreqMap = xLogManager.getEventFrequencyMap(trace);

      for (int j = 0; j < frequentItemsetsCount; ++j) {
        ArrayList<String> itemSet = frequentItemsets.get(j);
        Double count = getCountOfItemSetInTrace(itemSet, eventFreqMap, encodingType);
        instance.setValue(j, count);
      }

      double traceClass = traceLabelMap.get(trace);
      instance.setValue(frequentItemsetsCount, traceClass);
      encodedTraces.add(instance);
      i++;
    }
  }

  private Double getCountOfItemSetInTrace(ArrayList<String> itemSet,
      HashMap<String, Integer> eventMap, EncodingType encodingType) {
    double count = Double.MAX_VALUE;
    for (String item : itemSet) {
      if (!eventMap.containsKey(item)) {
        count = 0.0;
        return count;
      } else {
        Integer value = eventMap.get(item);
        if (value < count)
          count = value;
      }
    }
    if (encodingType.equals(EncodingType.BINARY))
      return 1.0;
    if (encodingType.equals(EncodingType.FREQUENCY))
      return count;
    else {
      System.out.println("Encoding type error!");
      return count;
    }
  }

  public Instances getEncodedTraces() {
    return encodedTraces;
  }
}
