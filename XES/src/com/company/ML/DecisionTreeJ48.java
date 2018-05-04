package com.company.ML;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Sourcable;
import weka.classifiers.trees.J48;
import weka.core.Instances;

public class DecisionTreeJ48 {
  private Instances dataSet;

  public DecisionTreeJ48 (Instances dataSet) {
    this.dataSet = dataSet;
  }

  public void classify () {
    try {
      // Setting last feature as class feature.
      dataSet.setClassIndex(dataSet.numAttributes() - 1);

      // Splitting data into train and test parts
      int trainSize = (int) Math.round(dataSet.numInstances() * 0.8);
      int testSize = dataSet.numInstances() - trainSize;

      Instances trainSet = new Instances(dataSet, 0, trainSize);
      Instances testSet = new Instances(dataSet, trainSize, testSize);

      J48 decisionTree = new J48();
      decisionTree.buildClassifier(trainSet);

      System.out.println("\n Running J48 on train data");
      predict(trainSet, decisionTree);

      System.out.println("\n Running J48 on test data");
      predict(testSet, decisionTree);

    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void predict (Instances dataSet, AbstractClassifier classifier) {
    try {
      double truePredictions = 0;
      for (int i = 0; i < dataSet.numInstances(); ++i) {
        double actualClass = dataSet.instance(i).classValue();
        double predClass = classifier.classifyInstance(dataSet.instance(i));

        if (actualClass == predClass)
          truePredictions += 1;
      }

      double accuracy = truePredictions / dataSet.numInstances();
      System.out.printf(" Accuracy = %f \n", accuracy);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

  }}
