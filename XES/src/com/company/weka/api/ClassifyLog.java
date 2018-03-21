package com.company.weka.api;

import weka.classifiers.AbstractClassifier;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.J48;
import weka.core.Instances;


public class ClassifyLog {

  public void run () {
    try {

      DataSource source = new DataSource("Hospital.arff");

      Instances dataSet = source.getDataSet();
      dataSet.setClassIndex(dataSet.numAttributes() - 1);

      for (int i = 0; i < dataSet.numClasses(); ++i) {
        String classValue = dataSet.classAttribute().value(i);
        System.out.println("Class Value " + i + " is " + classValue);
      }

      int trainSize = (int) Math.round(dataSet.numInstances() * 0.8);
      int testSize = dataSet.numInstances() - trainSize;
      Instances trainSet = new Instances(dataSet, 0, trainSize);
      Instances testSet = new Instances(dataSet, trainSize, testSize);


      J48 decisionTree = new J48();
      decisionTree.buildClassifier(trainSet);


      System.out.println("\nRunning classifier on train data");
      predict(trainSet, decisionTree);

      System.out.println("Running classifier on test data");
      predict(testSet, decisionTree);

      //System.out.printf("\nDecision Tree Graph \n %s \n", decisionTree.graph());

    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private void predict (Instances dataset, AbstractClassifier classifier) {
    try {
      double rightPredictions = 0;
      for (int i = 0; i < dataset.numInstances(); ++i) {
        double actualClass = dataset.instance(i).classValue();
        double predClass = classifier.classifyInstance(dataset.instance(i));

        if (actualClass == predClass) {
          rightPredictions += 1;
        }
      }
      double accuracy = rightPredictions / dataset.numInstances();
      System.out.printf(" Accuracy = %f \n", accuracy);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

  }

}
