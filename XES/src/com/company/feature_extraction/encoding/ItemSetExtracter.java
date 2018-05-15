package com.company.feature_extraction.encoding;

import java.util.ArrayList;
import org.deckfour.xes.model.XLog;
import weka.associations.Apriori;
import weka.core.Instances;

public class ItemSetExtracter {

  XLogManager xLogManager = null;

  public ItemSetExtracter (XLog log) {
    this.xLogManager = new XLogManager(log);
  }

  /**
   * Main function for extracting frequent itemsets from log.
   * 1. Encodes log according transaction style.
   * 2. Runs Apriori method on encoded log.
   * 3. Returns frequent itemsets.
   * @return
   */
  public ArrayList<ArrayList<String>> extractItemSets () {
    ArrayList<ArrayList<String>> frequentItemsets = null;
    try {
      // Encoding log with transaction style for Apriori.
      TransactionBasedEncoder transactionBasedEncoder = new TransactionBasedEncoder(
          xLogManager.getxLog());
      transactionBasedEncoder.encodeTraces(xLogManager.getxLog());
      Instances transactionInstances = transactionBasedEncoder.getEncodedTraces();

      //Run Apriori to make itemsets.
      Apriori apriori = new Apriori();

      String[] options = new String[2];
      options[0] = "-S";
      options[1] = "0.8";
      apriori.setOptions(options);
      apriori.buildAssociations(transactionInstances);

      //Extract frequent itemsets from Apriori method.
      frequentItemsets = apriori.getM_LsToString();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
    return frequentItemsets;
  }
}
