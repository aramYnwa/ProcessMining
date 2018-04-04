package com.company.weka.api;


import java.nio.file.Path;
import java.util.List;
import weka.associations.Apriori;
import weka.associations.AssociationRule;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class AprioriMethod {
  private Double frequency;
  private Path sourcePath;

  public AprioriMethod(Double frequency, Path sourcePath) {
    this.frequency = frequency;
    this.sourcePath = sourcePath;
  }

  public void findFrequentItemsets () {
      try {
        DataSource source = new DataSource(sourcePath.toString());

        Instances dataSet = source.getDataSet();
        //FIXME: by default last one is class.
        dataSet.setClassIndex(dataSet.numAttributes() - 1);

        Apriori aprioriModel = new Apriori();
        String[] options = new String[4];
        options[0] = "-Z"; // intends that first class is for missing values
        options[1] = "-I";
        options[2] = "-M";
        options[3] = frequency.toString();

        aprioriModel.setOptions(options);
        aprioriModel.buildAssociations(dataSet);

        List<AssociationRule> rules = aprioriModel.getAssociationRules().getRules();
        for (AssociationRule rule :rules) {
          System.out.println(rule.toString());
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
  }

}
