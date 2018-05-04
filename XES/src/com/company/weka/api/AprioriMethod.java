package com.company.weka.api;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import weka.associations.Apriori;
import weka.associations.AssociationRule;
import weka.associations.Item;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class AprioriMethod {
  private Double frequency;
  private Path sourcePath;

  public AprioriMethod(Double frequency, Path sourcePath) {
    this.frequency = frequency;
    this.sourcePath = sourcePath;
  }

  public ArrayList<ArrayList<String>> findFrequentItemsets () {
    ArrayList<ArrayList<String>> frequentItemsets = new ArrayList<>();
      try {
        DataSource source = new DataSource(sourcePath.toString());
        Instances dataSet = source.getDataSet();
        //FIXME: by default last one is class.
        dataSet.setClassIndex(dataSet.numAttributes() - 1);

        Apriori aprioriModel = new Apriori();
        String[] options = new String[6];
        options[0] = "-C"; // intends that first class is for missing values
        options[1] = "-0.1";
        options[2] = "-M";
        options[3] = frequency.toString();
        options[4] = "-N";
        options[5] = "100";

        aprioriModel.setOptions(options);
        aprioriModel.buildAssociations(dataSet);

        System.out.println(aprioriModel);

        frequentItemsets = aprioriModel.getM_LsToString();

      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    return frequentItemsets;
  }

  private ArrayList<ArrayList<String>> parseRules (List<AssociationRule> rules) {
    ArrayList<ArrayList<String>> rulesAttrCollection = new ArrayList<>();
    for (AssociationRule rule : rules) {
      ArrayList<String> ruleAttributes = new ArrayList<>();
      System.out.println(rule);
      ArrayList<Item> premise = new ArrayList<>(rule.getPremise());
      ArrayList<Item> consequence = new ArrayList<>(rule.getConsequence());

      System.out.println(premise);
      System.out.println(consequence);
      System.out.println("\n");
      premise.addAll(consequence);

      for(Item item : premise){
        System.out.println(item.getAttribute().name());
        ruleAttributes.add(item.getAttribute().name());
      }
      if (!rulesAttrCollection.contains(ruleAttributes)) {
        rulesAttrCollection.add(ruleAttributes);
      }
    }
    return rulesAttrCollection;
  }

}
