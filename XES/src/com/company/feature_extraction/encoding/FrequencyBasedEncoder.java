package com.company.feature_extraction.encoding;

import java.util.ArrayList;
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


public class FrequencyBasedEncoder {

  Map<String, Integer> alphabetMap = new HashMap<String, Integer>();
  HashMap<Integer, XTrace> traceMapping = new HashMap<Integer, XTrace>();
  Instances encodedTraces = null;
  XLogManager xLogManager = null;

  public FrequencyBasedEncoder (XLog xlog) {
    this.xLogManager = new XLogManager(xlog);
  }

  public HashMap<Integer, XTrace> getTraceMapping() {
    return traceMapping;
  }

  public Instances getEncodedTraces() {
    return encodedTraces;
  }

  public Map<String, Integer> getAlphabetMap() {
    return alphabetMap;
  }

  //FIXME:: Change this to use local xlog file.
  public void encodeTraces(XLog logTracesToEncode) {
    //FIXME:: Move this part to XLogManager
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

    ArrayList<Attribute> attributes = new ArrayList<Attribute>();
    for (int i = 0; i < alphabetMap.size(); ++i) {
      attributes.add(null);
    }

    for (String key : alphabetMap.keySet()) {
      Attribute attr = new Attribute(key, alphabetMap.get(key));
      attributes.set(alphabetMap.get(key), attr);
    }


    //FIXME:: Make sure this part is correct
    // Here should be addition of class attribute.
    ArrayList<String> labelValues = new ArrayList<>();
    labelValues.add("0");
    labelValues.add("1");
    Attribute label = new Attribute("label", labelValues);
    attributes.add(label);

    encodedTraces = new Instances("DATA", attributes, logTracesToEncode.size());

    int i = 0;
    for (XTrace trace : logTracesToEncode) {
      Instance instance = new DenseInstance(alphabetMap.size() + 1);

      //FIXME :: This part might be optimized
      for (int j = 0; j < alphabetMap.size(); ++j) {
        instance.setValue(j, new Double(0));
      }

      for (XEvent event : trace) {
        String eventLabel = XConceptExtension.instance().extractName(event);
        Integer index = alphabetMap.get(eventLabel);
        Double value = instance.value(index);
        //FIXME:: It seems the check below is redundant.
        if (value.isNaN())
          instance.setValue(index, new Double(1));
        else
          instance.setValue(index, value + 1);
      }

      Double traceClass = xLogManager.classifyTrace(trace);
      instance.setValue(attributes.size() - 1, traceClass);
      encodedTraces.add(instance);
      traceMapping.put(i, trace);
      i++;
    }
  }
}