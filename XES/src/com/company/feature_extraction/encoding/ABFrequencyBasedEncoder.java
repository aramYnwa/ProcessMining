package com.company.feature_extraction.encoding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlEnumValue;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ABFrequencyBasedEncoder {

  Map<String, Integer> alphabetMap = new HashMap<String, Integer>();
  HashMap<Integer, XTrace> traceMapping = new HashMap<Integer, XTrace>();
  Instances encodedTraces = null;
  XLogManager xLogManager = null;

  public ABFrequencyBasedEncoder (XLog xLog) {
    this.xLogManager = new XLogManager(xLog);
  }

  public void encodeTraces(XLog logTracesToEncode) {
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

    // For weka.apriori features should have some exact form.
    // They should be nominal and have a value {t}
    ArrayList<String> labelValues = new ArrayList<>();
    labelValues.add("t");
    for (String key : alphabetMap.keySet()) {
      Attribute attr = new Attribute(key, labelValues);
      attributes.set(alphabetMap.get(key), attr);
    }

    encodedTraces = new Instances("DATA", attributes, logTracesToEncode.size());


    int i = 0;
    for (XTrace trace : logTracesToEncode) {
      Instance instance = new DenseInstance(alphabetMap.size());
      instance.setDataset(encodedTraces);
      /*for (int j = 0; j < alphabetMap.size(); ++j) {
        instance.setValue(j, "?");
      }*/

      //Adding true values.
      for (XEvent event : trace) {
        String eventName = XConceptExtension.instance().extractName(event);
        Integer index = alphabetMap.get(eventName);

        instance.setValue(index, "t");
      }

      encodedTraces.add(instance);
      traceMapping.put(i, trace);
      i++;
    }
  }

  public Instances getEncodedTraces() {
    return encodedTraces;
  }
}
