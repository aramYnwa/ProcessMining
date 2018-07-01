package com.company.feature_extraction.encoding;

/*import com.company.feature_extraction.SignatureDiscovery.DiscoverSignatures;
import com.company.feature_extraction.SignatureDiscovery.FeatureExtraction;
import com.company.feature_extraction.SignatureDiscovery.SignatureDiscoveryInput;
import com.company.feature_extraction.SignatureDiscovery.types.Feature;*/
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.deckfour.xes.model.XLog;

public class SequentalPattern {
  XLogManager xLogManager = null;
  XLog log = null;

  public SequentalPattern (XLog log) {
    this.log = log;
    xLogManager = new XLogManager(log);
    initialization();
  }

  void initialization() {
    /*SignatureDiscoveryInput input = new SignatureDiscoveryInput();

    input.removeAllFeatures();
    input.addFeature("Tandem Repeat");
    input.addFeature("Maximal Repeat");
    input.addFeature("Tandem Repeat Alphabet");
    input.addFeature("Maximal Repeat Alphabet");

    //input.setAssociationRuleParameters(true,"","", "0.2", "0.9");
    DiscoverSignatures discoverSignatures = new DiscoverSignatures(log, input);
    //System.out.println(discoverSignatures.getEncodedDecodedRuleMap());
    FeatureExtraction featureExtraction =  discoverSignatures.getFeatureExtraction();

    Map<String,String> charToActivity =  featureExtraction.getCharActivityMap();
    Map<String,String> ActivityToChar =  featureExtraction.getActivityCharMap();

    int codingUnitLenght = findCodingUnitSize(charToActivity);


    Map<Feature, Set<String>> originalSequenceFeatureSetMap =  featureExtraction.getOriginalSequenceFeatureSetMap();
    Set<String> mr_set = originalSequenceFeatureSetMap.get(Feature.MR);

    for(String seq : mr_set) {
      //FIXME:: 2 below should be replaced by the lenght of encoding.
      String[] strSubstrings = seq.split("(?<=\\G.{2})");
      //String[] substrings = seq.su
      for (String subS : strSubstrings) {
        String activity = charToActivity.get(subS);
        System.out.println(activity);
      }
      System.out.println("\n");
    }*/
  }


  private int findCodingUnitSize(Map<String, String> charToActivity) {
    Map.Entry<String, String> entry = charToActivity.entrySet().iterator().next();
    String key = entry.getKey();
    return key.length();
  }
}
