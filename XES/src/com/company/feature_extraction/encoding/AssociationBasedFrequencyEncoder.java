package com.company.feature_extraction.encoding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.w3c.dom.Attr;
import sun.awt.X11.XTranslateCoordinates;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class AssociationBasedFrequencyEncoder {

  ArrayList<ArrayList<String>> frequentItemstets;
  XLogManager xLogManager = null;
  Map<String, Integer> alphabetMap = new HashMap<>();
  HashMap<Integer, XTrace> traceMapping = new HashMap<>();
  Instances encodedTraces  = null;

  public AssociationBasedFrequencyEncoder (XLogManager xLogManager,
      ArrayList<ArrayList<String>> frequentItemstets) {
    this.frequentItemstets = frequentItemstets;
    this.xLogManager = xLogManager;
  }

  public void encodeTraces( ) {
    XLog logTracesToEncode = xLogManager.getxLog();

    for (XTrace trace : logTracesToEncode) {
      for (XEvent event : trace) {
        String eventLabel = XConceptExtension.instance().extractName(event);
        Integer index = alphabetMap.get(eventLabel);

        if (index == null) {
          index = alphabetMap.size();
          alphabetMap.put(eventLabel, index);
        }
      }
    }

    ArrayList<Attribute> attributes = new ArrayList<>();
    for (int i = 0; i < frequentItemstets.size(); ++i) {
      attributes.add(null);
    }


    int i = 0;
    for (ArrayList<String> itemSet : frequentItemstets) {

      String setName = itemSet.toString();
      //FIXME:: Make sure that this index below is right
      Attribute attr = new Attribute(setName, i);
      attributes.set(i, attr);
      i++;
    }

    ArrayList<String> labelValues = new ArrayList<>();
    labelValues.add("0");
    labelValues.add("1");
    Attribute label = new Attribute("label", labelValues);
    attributes.add(label);

    encodedTraces = new Instances("DATA", attributes, logTracesToEncode.size());

    i = 0;
    for (XTrace trace : logTracesToEncode) {
      Instance instance = new DenseInstance(frequentItemstets.size() + 1);


      //FIXME :: This part might be optimized
      for (int j = 0; j < frequentItemstets.size(); ++j) {
        instance.setValue(j, new Double(0));
      }

      for (int j = 0; j < frequentItemstets.size(); ++j) {
        ArrayList<String> itemSet = frequentItemstets.get(j);
        Double count = getCountOfItemSetInInstance(itemSet, trace);
        instance.setValue(j, count);
      }

      Double traceClass = xLogManager.classifyTrace(trace);
      instance.setValue(attributes.size() - 1, traceClass);
      encodedTraces.add(instance);
      traceMapping.put(i, trace);
      i++;
    }
  }

  /**
   * Returns 0 if trace contains all items of itemSet at least once.
   * Can be modified to return how many times traces contains all items from itemSet.
   * @param itemSet
   * @param trace
   * @return
   */
  Double getCountOfItemSetInInstance(ArrayList<String> itemSet, XTrace trace) {
    HashMap<String, Integer> eventMap = getEvents(trace);

    for (String item : itemSet) {
      if (!eventMap.containsKey(item))
        return 0.0;
    }
    return 1.0;
  }

  HashMap<String, Integer> getEvents (XTrace xTrace) {
    HashMap<String, Integer> map = new HashMap<>();
    for (XEvent event : xTrace) {
      String eventName = XConceptExtension.instance().extractName(event);
      if (!map.containsKey(eventName)) {
        map.put(eventName, 1);
      } else {
        Integer value = map.get(eventName);
        map.put(eventName, value + 1);
      }
    }
    return map;
  }

  public Instances getEncodedTraces() {
    return encodedTraces;
  }
}
