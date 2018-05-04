package com.company.feature_extraction.encoding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class SequenceBasedEncoder {
  ArrayList<ArrayList<String>> frequentItemstets;
  XLogManager xLogManager = null;
  Map<String, Integer> alphabetMap = new HashMap<>();
  HashMap<Integer, XTrace> traceMapping = new HashMap<>();
  Instances encodedTraces  = null;

  public SequenceBasedEncoder (XLogManager xLogManager,
      ArrayList<ArrayList<String>> frequentItemstets) {
    this.xLogManager = xLogManager;
    this.frequentItemstets = frequentItemstets;
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

      ArrayList<String> eventSeq = traceToEventSeq(trace);
      for (int j = 0; j < frequentItemstets.size(); ++j) {
        ArrayList<String> itemSeq = frequentItemstets.get(j);
        Double count = traceContainsItemseq(itemSeq, eventSeq);
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
   * Returns 1 if events sequence of trace contains given event sequence
   * and 0 in other case
   * @param eventSeq
   * @param eventSeqOfTrace
   * @return
   */
  private Double traceContainsItemseq (ArrayList<String> eventSeq,
      ArrayList<String> eventSeqOfTrace) {
    int index = Collections.indexOfSubList(eventSeqOfTrace , eventSeq);
    if (index == -1)
      return 0.0;
    else
      return 1.0;
  }

  /**
   * Make sequence of events from event trace.
   * @param xTrace
   * @return
   */
  private ArrayList<String> traceToEventSeq (XTrace xTrace) {
    ArrayList<String> eventSeq = new ArrayList<>();
    for (XEvent event : xTrace) {
      String eventName = XConceptExtension.instance().extractName(event);
      eventSeq.add(eventName);
    }
    return eventSeq;
  }

  public Instances getEncodedTraces() {
    return encodedTraces;
  }
}
