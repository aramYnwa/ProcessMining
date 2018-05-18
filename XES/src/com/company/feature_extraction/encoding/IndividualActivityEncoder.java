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


public class IndividualActivityEncoder {

  Map<String, Integer> alphabetMap = new HashMap<String, Integer>();
  HashMap<Integer, XTrace> traceMapping = new HashMap<Integer, XTrace>();
  Instances encodedTraces = null;
  XLogManager xLogManager = null;
  EncodingType encodingType;

  public IndividualActivityEncoder(XLog xlog, EncodingType encodingType) {
    this.xLogManager = new XLogManager(xlog);
    this.encodingType = encodingType;
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
    HashMap<XTrace, Double> traceLabelMap = xLogManager.traceLabelMap;

    //FIXME:: Move this part to XLogManager
    for (XTrace trace : logTracesToEncode) {
      for (XEvent event : trace) {
        String eventName = XConceptExtension.instance().extractName(event);
        Integer index = alphabetMap.get(eventName);

        if (index == null) {
          index = alphabetMap.size();
          alphabetMap.put(eventName, index);
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

    Integer classIndex = attributes.size() - 1;
    if (encodingType.equals(EncodingType.FREQUENCY)) {
      int i = 0;
      for (XTrace trace : logTracesToEncode) {
        Instance instance = new DenseInstance(alphabetMap.size() + 1);

        //FIXME :: This part might be optimized
        for (int j = 0; j < alphabetMap.size(); ++j) {
          instance.setValue(j, new Double(0));
        }

        for (XEvent event : trace) {
          String eventName = XConceptExtension.instance().extractName(event);
          Integer index = alphabetMap.get(eventName);
          Double value = instance.value(index);
          //FIXME:: It seems the check below is redundant.
          if (value.isNaN())
            instance.setValue(index, 1);
          else
            instance.setValue(index, value + 1);
        }

        Double traceClass = traceLabelMap.get(trace);
        instance.setValue(classIndex, traceClass);
        encodedTraces.add(instance);
        traceMapping.put(i, trace);
        i++;
      }
    } else if (encodingType.equals(EncodingType.BINARY)) {
      int i = 0;
      for (XTrace trace : logTracesToEncode) {
        Instance instance = new DenseInstance(alphabetMap.size() + 1);

        for (int j = 0; j < alphabetMap.size(); ++j) {
          instance.setValue(j, 0);
        }

        for (XEvent event : trace) {
          String eventName = XConceptExtension.instance().extractName(event);
          Integer index = alphabetMap.get(eventName);
          instance.setValue(index, 1);
        }

        Double traceClass = traceLabelMap.get(trace);
        instance.setValue(classIndex, traceClass);
        encodedTraces.add(instance);
        traceMapping.put(i, trace);
        i++;
      }
    }
  }
}