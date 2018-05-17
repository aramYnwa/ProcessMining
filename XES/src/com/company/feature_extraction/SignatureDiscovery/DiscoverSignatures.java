package com.company.feature_extraction.SignatureDiscovery;

import static com.company.feature_extraction.SignatureDiscovery.types.LearningAlgorithmType.Id3;

import com.company.feature_extraction.SignatureDiscovery.encoding.ActivityOverFlowException;
import com.company.feature_extraction.SignatureDiscovery.encoding.EncodeActivitySet;
import com.company.feature_extraction.SignatureDiscovery.encoding.EncodeTraces;
import com.company.feature_extraction.SignatureDiscovery.encoding.EncodingNotFoundException;
import com.company.feature_extraction.SignatureDiscovery.encoding.InstanceProfile;
import com.company.feature_extraction.SignatureDiscovery.encoding.InstanceVector;
import com.company.feature_extraction.SignatureDiscovery.learningalgorithm.ClassSpecificAssociationRuleMiner;
import com.company.feature_extraction.SignatureDiscovery.metrics.Metrics;
import com.company.feature_extraction.SignatureDiscovery.metrics.RuleListMetrics;
import com.company.feature_extraction.SignatureDiscovery.types.EvaluationOptionType;
import com.company.feature_extraction.SignatureDiscovery.types.Feature;
import com.company.feature_extraction.SignatureDiscovery.types.FeatureType;
import com.company.feature_extraction.SignatureDiscovery.types.LearningAlgorithmType;
import com.company.feature_extraction.SignatureDiscovery.util.FileIO;
import com.company.feature_extraction.SignatureDiscovery.util.Logger;
import com.company.feature_extraction.SignatureDiscovery.settings.AssociationRuleSettings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

/**
 * @author R.P. Jagadeesh Chandra 'JC' Bose
 * @date 14 July 2010 
 * @since 01 July 2010
 * @version 1.0
 * @email j.c.b.rantham.prabhakara@tue.nl
 * @copyright R.P. Jagadeesh Chandra 'JC' Bose
 * 			  Architecture of Information Systems Group (AIS) 
 * 			  Department of Mathematics and Computer Science
 * 			  University of Technology, Eindhoven, The Netherlands
 */

public class DiscoverSignatures {
	XLog log;
	SignatureDiscoveryInput input;
	int encodingLength;
	List<String> encodedTraceList;
	List<String> loopReductEncodedTraceList;
	
	FeatureExtraction featureExtraction;
	List<InstanceProfile> instanceProfileList;

	Map<Feature, Set<String>> actualFeatureSequenceFeatureSetMap;
	Map<Feature, Map<String, Integer>> actualFeatureSequenceNOCMap;
	Map<Feature, Map<String, Integer>> actualFeatureSequenceInstanceCountPercentageMap;
	Map<Feature, Map<Set<String>, Integer>> actualFeatureAlphabetNOCMap;
	Map<Feature, Map<Set<String>, Integer>> actualFeatureAlphabetInstanceCountPercentageMap;
	
	Map<Feature, Map<Set<String>, Set<String>>> actualFeatureAlphabetFeatureSetMap;
	
	Map<Feature, Set<String>> filteredActualFeatureSequenceFeatureSetMap;
	Map<Feature, Map<Set<String>, Set<String>>> filteredActualFeatureAlphabetFeatureSetMap;
	
	Map<Feature, List<InstanceVector>> featureInstanceVectorListMap;
	Map<Feature, List<String>> featureAttributeNameListMap;
	
	Map<Feature, Instances> featureNominalWekaInstancesMap = new HashMap<Feature, Instances>();
	Map<Feature, Instances> featureNumericWekaInstancesMap = new HashMap<Feature, Instances>();
	
	Map<String, String> charActivityMap;
	Map<String, String> activityCharMap;
	

	Set<String> attributesInRuleSet;
	float threshold = 0.3f;
	List<String> finalRuleList;
	Map<String, Metrics> finalRuleListMetricsMap;
	Map<String, String> encodedDecodedRuleMap;
	
	String maxOptionsString;
	String maxClass;
	Feature maxFeature;
	double maxF1Score;
	
	boolean hasSignatures;
	
	public DiscoverSignatures(XLog log, SignatureDiscoveryInput input){
		this.log = log;
		this.input = input;
		hasSignatures = true;
		encodeLog();
		computeFeatureSets();
		setActualFeatureSet();
		filterFeatureSet();
		createInstanceVector();
		prepareWekaData();
		findSignatures();
	}
	
	/**
	 * This method encodes the given log into character streams
	 */
	private void encodeLog(){
		/*
		 * activitySet accumulates the set of distinct
		 * activities/events in the event log; it doesn't store the trace
		 * identifier for encoding; Encoding trace identifier is required only
		 * when any of the maximal repeat (alphabet) features is selected
		 */

		Set<String> activitySet = new HashSet<String>();
		XAttributeMap attributeMap;
		Set<String> eventTypeSet = new HashSet<String>();
		
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				attributeMap = event.getAttributes();
				activitySet.add(attributeMap.get("concept:name").toString() + "-"
						+ attributeMap.get("lifecycle:transition").toString());
				eventTypeSet.add(attributeMap.get("lifecycle:transition").toString());
			}
			activitySet.add(trace.getAttributes().get("concept:name").toString());
		}

		
		try {
			EncodeActivitySet encodeActivitySet = new EncodeActivitySet(activitySet);
			encodingLength = encodeActivitySet.getEncodingLength();

			activityCharMap = encodeActivitySet.getActivityCharMap();
			charActivityMap = encodeActivitySet.getCharActivityMap();
//			System.out.println("Encoding Length: "+encodingLength);
//			System.out.println("activityCharMap size: "+activityCharMap.size());
			/*
			 * Encode each trace to a charStream
			 */
			EncodeTraces encodeTraces = new EncodeTraces(activityCharMap, log);
			encodedTraceList = encodeTraces.getCharStreamList();
			instanceProfileList = encodeTraces.getInstanceProfileList();
		}catch(ActivityOverFlowException e){
			e.printStackTrace();
		}catch(EncodingNotFoundException e){
			e.printStackTrace();
		}
	}
	
	private void computeFeatureSets(){
		// Get combination Features
		Set<Feature> selectedFeatureSet = input.selectedFeatureSet;
		Set<Feature> expandedSelectedFeatureSet = input.selectedFeatureSet;
		
		if(selectedFeatureSet.contains(Feature.IE)){
			if(selectedFeatureSet.contains(Feature.TR))
				expandedSelectedFeatureSet.add(Feature.IE_TR);
			if(selectedFeatureSet.contains(Feature.MR))
				expandedSelectedFeatureSet.add(Feature.IE_MR);
			if(selectedFeatureSet.contains(Feature.SMR))
				expandedSelectedFeatureSet.add(Feature.IE_SMR);
			if(selectedFeatureSet.contains(Feature.NSMR))
				expandedSelectedFeatureSet.add(Feature.IE_NSMR);
			
			if(selectedFeatureSet.contains(Feature.TRA))
				expandedSelectedFeatureSet.add(Feature.IE_TRA);
			if(selectedFeatureSet.contains(Feature.MRA))
				expandedSelectedFeatureSet.add(Feature.IE_MRA);
			if(selectedFeatureSet.contains(Feature.SMRA))
				expandedSelectedFeatureSet.add(Feature.IE_SMRA);
			if(selectedFeatureSet.contains(Feature.NSMRA))
				expandedSelectedFeatureSet.add(Feature.IE_NSMRA);
		}
		if(selectedFeatureSet.contains(Feature.TR)){
			if(selectedFeatureSet.contains(Feature.MR))
				expandedSelectedFeatureSet.add(Feature.TR_MR);
			if(selectedFeatureSet.contains(Feature.SMR))
				expandedSelectedFeatureSet.add(Feature.TR_SMR);
			if(selectedFeatureSet.contains(Feature.NSMR))
				expandedSelectedFeatureSet.add(Feature.TR_NSMR);
		}
		
		if(selectedFeatureSet.contains(Feature.TRA)){
			if(selectedFeatureSet.contains(Feature.MRA))
				expandedSelectedFeatureSet.add(Feature.TRA_MRA);
			if(selectedFeatureSet.contains(Feature.SMRA))
				expandedSelectedFeatureSet.add(Feature.TRA_SMRA);
			if(selectedFeatureSet.contains(Feature.NSMRA))
				expandedSelectedFeatureSet.add(Feature.TRA_NSMRA);
		}
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(Feature.TR) && selectedFeatureSet.contains(Feature.MR))
			expandedSelectedFeatureSet.add(Feature.IE_TR_MR);
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(Feature.TR) && selectedFeatureSet.contains(Feature.SMR))
			expandedSelectedFeatureSet.add(Feature.IE_TR_SMR);
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(Feature.TR) && selectedFeatureSet.contains(Feature.NSMR))
			expandedSelectedFeatureSet.add(Feature.IE_TR_NSMR);
		
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(Feature.TRA) && selectedFeatureSet.contains(Feature.MRA))
			expandedSelectedFeatureSet.add(Feature.IE_TRA_MRA);
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(Feature.TRA) && selectedFeatureSet.contains(Feature.SMRA))
			expandedSelectedFeatureSet.add(Feature.IE_TRA_SMRA);
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(Feature.TRA) && selectedFeatureSet.contains(Feature.NSMRA))
			expandedSelectedFeatureSet.add(Feature.IE_TRA_NSMRA);
		
		input.selectedFeatureSet = expandedSelectedFeatureSet;
		featureExtraction = new FeatureExtraction(encodingLength, activityCharMap, charActivityMap, instanceProfileList, expandedSelectedFeatureSet, input.kGramValue);
		featureExtraction.computeNonOverlapFeatureMetrics();
	}

	private void setActualFeatureSet(){
		Logger.printCall("Calling Discover Signatures -> setActualFeatureSet()");
		if(input.isBaseFeatures || input.featureType == FeatureType.Best){
			actualFeatureSequenceFeatureSetMap = featureExtraction.getBaseSequenceFeatureSetMap();
			actualFeatureAlphabetFeatureSetMap = featureExtraction.getBaseAlphabetFeatureSetMap();
			actualFeatureSequenceNOCMap = featureExtraction.getBaseSequenceFeatureNOCMap();
			actualFeatureSequenceInstanceCountPercentageMap = featureExtraction.getBaseSequenceFeatureInstanceCountPercentageMap();
			
			actualFeatureAlphabetNOCMap = featureExtraction.getBaseAlphabetFeatureNOCMap();
			actualFeatureAlphabetInstanceCountPercentageMap = featureExtraction.getBaseAlphabetFeatureInstanceCountPercentageMap();
			
			if(input.featureType == FeatureType.Best){
				actualFeatureAlphabetNOCMap = featureExtraction.getOriginalAlphabetFeatureNOCMap();
				actualFeatureAlphabetInstanceCountPercentageMap = featureExtraction.getOriginalAlphabetFeatureInstanceCountPercentageMap();
			}
		}else if(!input.isBaseFeatures){
			Logger.println("Here !isBaseFeature");
			actualFeatureSequenceFeatureSetMap = featureExtraction.getOriginalSequenceFeatureSetMap();
			actualFeatureSequenceNOCMap = featureExtraction.getOriginalSequenceFeatureNOCMap();
			actualFeatureSequenceInstanceCountPercentageMap = featureExtraction.getOriginalSequenceFeatureInstanceCountPercentageMap();

			actualFeatureAlphabetFeatureSetMap = featureExtraction.getOriginalAlphabetFeatureSetMap();
			actualFeatureAlphabetNOCMap = featureExtraction.getOriginalAlphabetFeatureNOCMap();
			actualFeatureAlphabetInstanceCountPercentageMap = featureExtraction.getOriginalAlphabetFeatureInstanceCountPercentageMap();
		}
		Logger.printReturn("Returning Discover Signatures -> setActualFeatureSet()");
	}
	
	private void filterFeatureSet(){
		Logger.printCall("Calling Discover Signatures -> filterFeatureSet()");
		filteredActualFeatureSequenceFeatureSetMap = new HashMap<Feature, Set<String>>(actualFeatureSequenceFeatureSetMap);
		filteredActualFeatureAlphabetFeatureSetMap = new HashMap<Feature, Map<Set<String>, Set<String>>>(actualFeatureAlphabetFeatureSetMap);
		Logger.printReturn("Returning Discover Signatures -> filterFeatureSet()");
	}
	
	private void createInstanceVector(){
		if(featureInstanceVectorListMap == null){
			featureInstanceVectorListMap = new HashMap<Feature, List<InstanceVector>>();
		}else{
			featureInstanceVectorListMap.clear();
		}
		
		if(featureAttributeNameListMap == null){
			featureAttributeNameListMap = new HashMap<Feature, List<String>>();
		}else{
			featureAttributeNameListMap.clear();
		}
		
		List<InstanceVector> instanceVectorList;
		InstanceVector instanceVector;
		List<String> attributeNameList;

		Set<String> sequenceFeatureSet;
		List<String> sequenceFeatureList = new ArrayList<String>();
		
		for (Feature feature : filteredActualFeatureSequenceFeatureSetMap.keySet()) {
			sequenceFeatureSet = filteredActualFeatureSequenceFeatureSetMap.get(feature);
			
			sequenceFeatureList.clear();
			sequenceFeatureList.addAll(sequenceFeatureSet);
			
			instanceVectorList = new ArrayList<InstanceVector>();
			
			for (InstanceProfile instanceProfile : instanceProfileList) {
				instanceVector = new InstanceVector();
				instanceVector.setLabel(instanceProfile.getLabel());
				
				
				instanceVector.setSequenceFeatureCountMap(featureExtraction
						.computeNonOverlapSequenceFeatureCountMap(encodingLength,
								instanceProfile.getEncodedTrace(), sequenceFeatureSet));
				
				instanceVector.standarizeNumericVector(sequenceFeatureList);
				instanceVector.standarizeNominalVector(sequenceFeatureList);
				instanceVectorList.add(instanceVector);
			}
			featureInstanceVectorListMap.put(feature, instanceVectorList);
			attributeNameList = new ArrayList<String>();
			attributeNameList.addAll(sequenceFeatureList);
			featureAttributeNameListMap.put(feature, attributeNameList);
		}
		
		Map<Set<String>, Set<String>> alphabetFeatureSetMap;
		List<Set<String>> alphabetFeatureList = new ArrayList<Set<String>>();
		
		for(Feature feature : filteredActualFeatureAlphabetFeatureSetMap.keySet()){
			alphabetFeatureSetMap = filteredActualFeatureAlphabetFeatureSetMap.get(feature);
			alphabetFeatureList.clear();
			alphabetFeatureList.addAll(alphabetFeatureSetMap.keySet());
			
			instanceVectorList = new ArrayList<InstanceVector>();
			for(InstanceProfile instanceProfile : instanceProfileList){
				instanceVector = new InstanceVector();
				instanceVector.setLabel(instanceProfile.getLabel());

				instanceVector.setAlphabetFeatureCountMap(featureExtraction.computeNonOverlapAlphabetFeatureCountMap(encodingLength, instanceProfile.getEncodedTrace(), alphabetFeatureSetMap));
				instanceVector.standarizeNumericVector(alphabetFeatureList);
				instanceVector.standarizeNominalVector(alphabetFeatureList);
				instanceVectorList.add(instanceVector);
				
			}
			featureInstanceVectorListMap.put(feature, instanceVectorList);
			
			attributeNameList = new ArrayList<String>();
			for(Set<String> alphabet : alphabetFeatureList)
				attributeNameList.add(alphabet.toString());

			/*
			 * Add the attribute names
			 */
			featureAttributeNameListMap.put(feature, attributeNameList);
		}
	}
	
	private void prepareWekaData(){
		// This is the property name for accessing OS temporary directory or
		String tempDirProperty = "java.io.tmpdir";
		String fileSeparator = System.getProperty("file.separator");
		// Set the outputDir within the tempDir
		String outputDir = System.getProperty(tempDirProperty)+fileSeparator+"ProM"+fileSeparator+"SignatureDiscovery";
//		System.out.println("Temp Dir: "+outputDir);
		FileIO io = new FileIO();
		io.writeToFile(outputDir, "charActivityMap.txt", charActivityMap, "\\^");	
		io.writeToFile(outputDir, "instanceProfileList.txt", instanceProfileList);
		List<InstanceVector> instanceVectorList;
		List<String> attributeNameList;
		
		Set<String> classLabelSet = new HashSet<String>();
		FileOutputStream fos;
		PrintStream ps;
		Iterator<String> it;
		try{
			for(Feature feature : featureInstanceVectorListMap.keySet()){
				instanceVectorList = featureInstanceVectorListMap.get(feature);
				attributeNameList = featureAttributeNameListMap.get(feature);
				classLabelSet.clear();
				
				for(InstanceVector instanceVector : instanceVectorList){
					classLabelSet.add(instanceVector.getLabel());
				}
				
				/*
				 * Generate Weka data file
				 */
				
				if(!new File(outputDir+"\\Weka").exists()){
					new File(outputDir+"\\Weka").mkdirs();
				}
				
				
				fos = new FileOutputStream(outputDir+"\\Weka\\"+feature+"_Nominal.arff");
				ps = new PrintStream(fos);
				ps.println("@RELATION SignatureDiscovery");
				for(String attributeName : attributeNameList){
					ps.println("@ATTRIBUTE "+attributeName.replaceAll(", ", "_").trim()+" {0,1}");
				}
				ps.print("@ATTRIBUTE class {");
				it = classLabelSet.iterator();
				while(it.hasNext()){
					ps.print(it.next());
					if(it.hasNext())
						ps.print(",");
				}
				ps.println("}");
				ps.println();
				ps.println("@DATA");
				for(InstanceVector instanceVector : instanceVectorList){
					ps.println(instanceVector.toStringStandarizedNominalVector());
				}
				ps.close();
				fos.close();
				
				fos = new FileOutputStream(outputDir+"\\Weka\\"+feature+"_Numeric.arff");
				ps = new PrintStream(fos);
				ps.println("@RELATION SignatureDiscovery");
				for(String attributeName : attributeNameList){
					ps.println("@ATTRIBUTE "+attributeName.replaceAll(", ", "_").trim()+" REAL");
				}
				ps.print("@ATTRIBUTE class {");
				it = classLabelSet.iterator();
				while(it.hasNext()){
					ps.print(it.next());
					if(it.hasNext())
						ps.print(",");
				}
				ps.println("}");
				ps.println();
				ps.println("@DATA");
				for(InstanceVector instanceVector : instanceVectorList){
					ps.println(instanceVector.toStringStandarizedNumericVector());
				}
				ps.close();
				fos.close();
				
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		
		//	Now read the data from the generated .arff files
		
		featureNominalWekaInstancesMap = new HashMap<Feature, Instances>();
		featureNumericWekaInstancesMap = new HashMap<Feature, Instances>();
		
		DataSource dataSource;
		Instances data;
		try {
			for(Feature feature : featureInstanceVectorListMap.keySet()){
				if(input.isNominalCount || input.featureType == FeatureType.Best){
					dataSource = new DataSource(outputDir+"\\Weka\\"+feature+"_Nominal.arff");
				
					data = dataSource.getDataSet();
					if(data.classIndex() == -1)
						data.setClassIndex(data.numAttributes()-1);
					featureNominalWekaInstancesMap.put(feature, data);
				}
				if(!input.isNominalCount || input.featureType == FeatureType.Best){
					dataSource = new DataSource(outputDir+"\\Weka\\"+feature+"_Numeric.arff");
					data = dataSource.getDataSet();
					if(data.classIndex() == -1)
						data.setClassIndex(data.numAttributes()-1);
					featureNumericWekaInstancesMap.put(feature, data);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void findSignatures(){
//		System.out.println(input.learningAlgorithmType);
		attributesInRuleSet = new HashSet<String>();
		String optionsString;
		Map<String, Map<Feature, RuleListMetrics>> optionsStringFeatureRuleListMetricsMap = new HashMap<String, Map<Feature,RuleListMetrics>>();
		Map<Feature, RuleListMetrics> featureRuleListMetricsMap;
		
		try{
			if(input.learningAlgorithmType == LearningAlgorithmType.Best){
				//Try Decision Tree; ID3 doesn't depend on any parameters 
				featureRuleListMetricsMap = findID3Signatures();
				optionsStringFeatureRuleListMetricsMap.put("Nominal", featureRuleListMetricsMap);
			
				//Parameter tuning is required only for J48 and not for ID3;			
				for(double c = 0.2; c < 0.5 ; c += 0.1){
					optionsString ="-C "+c+" -M 1";
					if(featureNominalWekaInstancesMap.size() > 0){
						featureRuleListMetricsMap = findJ48Signatures(optionsString, featureNominalWekaInstancesMap);
						optionsStringFeatureRuleListMetricsMap.put(optionsString+" Nominal", featureRuleListMetricsMap);
					}
					if(featureNumericWekaInstancesMap.size() > 0){
						featureRuleListMetricsMap = findJ48Signatures(optionsString, featureNumericWekaInstancesMap);
						optionsStringFeatureRuleListMetricsMap.put(optionsString+" Numeric", featureRuleListMetricsMap);
					}
					
				}
				
				//Parameter tuning is required for association rules; we restrict it to minSupport >= 0.2 and minConfidence >= 0.9 
				for(float minSupport = 0.2f; minSupport < 1.0; minSupport += 0.1){
					optionsString = "AssociationRules -minSupport "+minSupport+" -minConfidence "+0.9;
					featureRuleListMetricsMap = findAssociationRules(minSupport, 0.9f);
					optionsStringFeatureRuleListMetricsMap.put(optionsString+" Nominal", featureRuleListMetricsMap);
				}
			}else{
				//One of decision tree or association rule learning algorithms would have been selected
				optionsString = getParameterOptions();
				if(input.learningAlgorithmType == LearningAlgorithmType.AssociationRules){
					featureRuleListMetricsMap = findAssociationRules();
					optionsStringFeatureRuleListMetricsMap.put(optionsString+" Nominal", featureRuleListMetricsMap);
					Logger.println("Options String: "+optionsString);
				}else if(input.learningAlgorithmType == LearningAlgorithmType.J48){
						if(featureNominalWekaInstancesMap.size() > 0){
							featureRuleListMetricsMap = findJ48Signatures(optionsString, featureNominalWekaInstancesMap);
							optionsStringFeatureRuleListMetricsMap.put(optionsString+" Nominal", featureRuleListMetricsMap);
						}
						if(featureNumericWekaInstancesMap.size() > 0){
							featureRuleListMetricsMap = findJ48Signatures(optionsString, featureNumericWekaInstancesMap);
							optionsStringFeatureRuleListMetricsMap.put(optionsString+" Numeric", featureRuleListMetricsMap);
						}
				}else if(input.learningAlgorithmType == Id3){
					featureRuleListMetricsMap = findID3Signatures();
					optionsStringFeatureRuleListMetricsMap.put("Nominal", featureRuleListMetricsMap);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		chooseBestRuleList(optionsStringFeatureRuleListMetricsMap);
	}
	
	@SuppressWarnings("unchecked")
	private Map<Feature, RuleListMetrics> findID3Signatures(){
		Instances filteredData, trainData, testData = null, data;
		List<String> currentIterationRuleList = new ArrayList<String>();
		List<String> ruleList = new ArrayList<String>();
		
		Metrics metrics;
		boolean isCurrentIterationRulesOk;
		
		float[] weightedAvgMetrics;
		
		Map<Feature, RuleListMetrics> featureRuleListMetricsMap = new HashMap<Feature, RuleListMetrics>();
		Map<String, Metrics> classMetricsMap;

		Enumeration<Object> classValueEnumeration;
		
		Attribute classAttribute;
		int classIndex;
		int tp, fp, tn, fn;
		
		int noCVFolds = 10;
		try{
			for(Feature feature : featureNominalWekaInstancesMap.keySet()){
				Classifier id3 = new J48();
				
				data = featureNominalWekaInstancesMap.get(feature);
				
				classMetricsMap = new HashMap<String, Metrics>();
				classAttribute = data.classAttribute();
				classValueEnumeration = classAttribute.enumerateValues();
				while(classValueEnumeration.hasMoreElements()){
					classMetricsMap.put(classValueEnumeration.nextElement().toString(), new Metrics(0,0,0,0,0));
				}
				
				Logger.println("Class Values: "+classMetricsMap.keySet());
				
				trainData = data;
				filteredData = trainData;
				
				ruleList.clear();
				
				while(ruleList.size() < input.noRulesToGenerate && filteredData.numAttributes() > 0){
					isCurrentIterationRulesOk = false;
					attributesInRuleSet.clear();

					Logger.println("No. Instances Before Zero Row: "+filteredData.numInstances());
					Logger.println("No Attributes: "+filteredData.numAttributes());

					/*
					 * Check if there are instances with all attribute values
					 * as 0; If so, remove those instances
					 */
					
					filteredData = getNonZeroInstances(filteredData);
					
					Logger.println("No. Instances After Zero Row: "+filteredData.numInstances());
					
					id3.buildClassifier(filteredData);
				
					currentIterationRuleList.clear();
					
					currentIterationRuleList.addAll(convertDecisionTreeToRules(id3.toString()));
					
					/*
					 * There are no rules generated in this current iteration; So, exit out of the loop
					 */
					if(attributesInRuleSet.size() == 0)
						break;
					
					/*
					 * Perform Evaluation based on the chosen settings; If
					 * choose automatically, then evaluation would be based on
					 * Cross Validation (default settings)
					 */
					
					Evaluation eval = new Evaluation(filteredData);
					if(input.evaluationOptions.evaluationOptionType == EvaluationOptionType.TrainingSet){
						eval.evaluateModel(id3, filteredData);

						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;
							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger.println("EM: "+tp+","+fp+","+tn+","+fn+","+filteredData.numInstances());
								
								metrics.add(tp, fp, tn, fn, filteredData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}else if(input.evaluationOptions.evaluationOptionType == EvaluationOptionType.PercentageSplit){
						data.randomize(new Random(1));
						double percent = new Double(input.evaluationOptions.noFoldsPercentageSplitValueStr.trim());
						int trainSize = (int) Math.round(data.numInstances() * percent/100);
						int testSize = data.numInstances()-trainSize;
						trainData = new Instances(data, 0, trainSize);
						testData = new Instances(data, trainSize, testSize);
						
						Classifier id3Temp = new J48();
						
						id3Temp.buildClassifier(trainData);
						//use test set
						eval.evaluateModel(id3Temp, testData);
						
						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;

							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger.println("EM: "+tp+","+fp+","+tn+","+fn+","+testData.numInstances());
								
								metrics.add(tp, fp, tn, fn, testData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}else{
						// by default do cross validation
						
						noCVFolds = new Integer(input.evaluationOptions.noFoldsPercentageSplitValueStr.trim()).intValue();
						if(noCVFolds > filteredData.numInstances())
							noCVFolds = filteredData.numInstances()-1;
						eval.crossValidateModel(id3, filteredData, noCVFolds, new Random(1));
						
						Logger.println("% Correct: "+eval.pctCorrect());
						
						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;

							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger.println("EM: "+tp+","+fp+","+tn+","+fn+","+filteredData.numInstances());
								
								metrics.add(tp, fp, tn, fn, filteredData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}
					
					if(isCurrentIterationRulesOk)
						ruleList.addAll(currentIterationRuleList);
					
					Logger.println("Attributes in Rule Set: "+attributesInRuleSet);
					
					filteredData = filterInstancesOnAttributes(attributesInRuleSet, filteredData);
					
					Logger.println("No. Attributes After Filter: "+filteredData.numAttributes());
				}
				featureRuleListMetricsMap.put(feature, new RuleListMetrics("", ruleList, classMetricsMap));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return featureRuleListMetricsMap;
	}
	
	@SuppressWarnings("unchecked")
	private Map<Feature, RuleListMetrics> findJ48Signatures(String optionsString, Map<Feature, Instances> featureWekaInstancesMap){
		Logger.printCall("Calling findJ48Signatures");
		Attribute classAttribute;
		Instances filteredData, trainData, testData = null, data;
		List<String> ruleList = new ArrayList<String>();
		List<String> currentIterationRuleList = new ArrayList<String>();
		int noCVFolds = 10;
		int tp, tn, fp, fn;
		Enumeration<Object> classValueEnumeration;
		int classIndex;
		Metrics metrics;
		boolean isCurrentIterationRulesOk;
		float[] weightedAvgMetrics;
		
		Map<Feature, RuleListMetrics> featureRuleListMetricsMap = new HashMap<Feature, RuleListMetrics>();
		Map<String, Metrics> classMetricsMap;
		
		try{
			for(Feature feature : featureWekaInstancesMap.keySet()){
				Classifier j48 = new J48();
				((J48)j48).setOptions(weka.core.Utils.splitOptions(optionsString));
				
				data = featureWekaInstancesMap.get(feature);
				
				classMetricsMap = new HashMap<String, Metrics>();
				classAttribute = data.classAttribute();
				classValueEnumeration = classAttribute.enumerateValues();
				while(classValueEnumeration.hasMoreElements()){
					classMetricsMap.put(classValueEnumeration.nextElement().toString(), new Metrics(0,0,0,0,0));
				}
				
				Logger.println("Class Values: "+classMetricsMap.keySet());
				
				trainData = data;
				filteredData = trainData;

				ruleList.clear();
				while(ruleList.size() < input.noRulesToGenerate && filteredData.numAttributes() > 0){
					isCurrentIterationRulesOk = false;
					attributesInRuleSet.clear();

					Logger.println("No. Instances (before zero row removal): "+filteredData.numInstances());
					Logger.println("No Attributes: "+filteredData.numAttributes());

					/*
					 * Check if there is an instance with all attribute values as 0;
					 * If so, remove that instance
					 */
					
					filteredData = getNonZeroInstances(filteredData);
					
					Logger.println("No. Instances (after zero row removal): "+filteredData.numInstances());
					
					currentIterationRuleList.clear();
					
					j48.buildClassifier(filteredData);

					currentIterationRuleList.addAll(convertDecisionTreeToRules(j48.toString()));
					
					/*
					 * There are no rules generated in this current iteration; So, exit out of the loop
					 */
					if(attributesInRuleSet.size() == 0)
						break;
					
					/*
					 * Perform Evaluation based on the chosen settings; If
					 * choose automatically, then evaluation would be based on
					 * Cross Validation (default settings)
					 */
					
					Evaluation eval = new Evaluation(filteredData);
					if(input.evaluationOptions.evaluationOptionType == EvaluationOptionType.TrainingSet){
						eval.evaluateModel(j48, trainData);

						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;
							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger.println("EM: "+tp+","+fp+","+tn+","+fn+","+filteredData.numInstances());
								
								metrics.add(tp, fp, tn, fn, filteredData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}else if(input.evaluationOptions.evaluationOptionType == EvaluationOptionType.PercentageSplit){
						data.randomize(new Random(1));
						double percent = new Double(input.evaluationOptions.noFoldsPercentageSplitValueStr.trim());
						int trainSize = (int) Math.round(data.numInstances() * percent/100);
						int testSize = data.numInstances()-trainSize;
						trainData = new Instances(data, 0, trainSize);
						testData = new Instances(data, trainSize, testSize);
						
						Classifier j48Temp = new J48();
						((J48)j48Temp).setOptions(weka.core.Utils.splitOptions(optionsString));
						j48Temp.buildClassifier(trainData);
						//use test set
						eval.evaluateModel(j48Temp, testData);
						
						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;

							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger.println("EM: "+tp+","+fp+","+tn+","+fn+","+testData.numInstances());
								
								metrics.add(tp, fp, tn, fn, testData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}else{
						// by default do cross validation
						noCVFolds = new Integer(input.evaluationOptions.noFoldsPercentageSplitValueStr).intValue();
						if(noCVFolds > filteredData.numInstances())
							noCVFolds = filteredData.numInstances()-1;
						eval.crossValidateModel(j48, filteredData, noCVFolds, new Random(1));
						Logger.println("% Correct: "+eval.pctCorrect());
						
						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;

							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger.println("EM: "+tp+","+fp+","+tn+","+fn+","+filteredData.numInstances());
								
								metrics.add(tp, fp, tn, fn, filteredData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}
					
					/*
					 * Add the rules generated in this iteration only if its
					 * performance is above the threshold; We consider the
					 * F-measure as the basis metric
					 */
					if(isCurrentIterationRulesOk)
						ruleList.addAll(currentIterationRuleList);
					
					filteredData = filterInstancesOnAttributes(attributesInRuleSet, filteredData);
					
					Logger.println("No. Attributes (after filtering attributes involved in rules): "+filteredData.numAttributes());
				}
				
				for(String classValue : classMetricsMap.keySet()){
					metrics = classMetricsMap.get(classValue);
					Logger.println(classValue+" @ "+metrics.getTP()+","+metrics.getFP()+","+metrics.getTN()+","+metrics.getFN()+","+metrics.getNoInstances());
				}
				featureRuleListMetricsMap.put(feature, new RuleListMetrics(optionsString, ruleList, classMetricsMap));
			}
//			buildResultsPanel(ruleList);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		Logger.printReturn("Calling findJ48Signatures");
		return featureRuleListMetricsMap;
	}
	
	/**
	 * This method removes the set of attributes (and the corresponding values)
	 * passed in attributeNameSet from the data instances. If the original
	 * number of attributes are N then the filtered data will be N-M where M is
	 * the number of attributes in attributeNameSet
	 * 
	 * @param attributeNameSet
	 * @param data
	 * @return
	 */
	private Instances filterInstancesOnAttributes(Set<String> attributeNameSet, Instances data){
		Logger.printCall("Calling filterInstancesOnAttributes()");
		Instances filteredData = data;
		try {
			Set<Integer> attributeIndicesSet = new HashSet<Integer>();

			for (int i = 0; i < data.numAttributes(); i++) {
				if (attributeNameSet.contains(data.attribute(i).name())) {
					attributeIndicesSet.add(i);
				}
			}

			int[] attributeIndicesArray = new int[attributeIndicesSet.size()];
			int index = 0;
			for (Integer attributeIndex : attributeIndicesSet)
				attributeIndicesArray[index++] = attributeIndex;

			Remove remove = new Remove();
			remove.setAttributeIndicesArray(attributeIndicesArray);
			remove.setInputFormat(data);
			filteredData = Filter.useFilter(data, remove);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Logger.printReturn("Returning filterInstancesOnAttributes()");
		return filteredData;
	}
	
	/**
	 * This method filters any instance whose attribute values are all zeros. 
	 * @param data
	 * @return
	 */
	private Instances getNonZeroInstances(Instances data){
		Logger.printCall("Calling getNonZeroInstances()");
		Instance instance;
		boolean zeroRowExists = false;
		boolean isZeroRow;
		do{
			zeroRowExists = false;
			for(int i = 0; i < data.numInstances(); i++){
				instance = data.instance(i);
				isZeroRow = true;
				for(int j = 0; j < instance.numAttributes(); j++){
					if(instance.value(j) != 0){
						isZeroRow = false;
						break;
					}
				}
				if(isZeroRow){
					data.delete(i);
					zeroRowExists = true;
					break;
				}
			}
		}while(zeroRowExists);
		
		Logger.printReturn("Returning getNonZeroInstances()");
		return data;
	}
	
	private List<String> convertDecisionTreeToRules(String tree){
		Logger.printCall("Calling convertDecisionTreeToRules()");
		Logger.println(tree);

		String[] treeSplit = tree.replaceAll(" ", "").split("[\r\n\b]+");
		
		Map<Integer, String> levelConstriantMap = new HashMap<Integer, String>();
		int depth;
		String[] ruleSplit;
		List<String> ruleList = new ArrayList<String>();
		String constraint, rule;
		int startIndex, endIndex;
		String classLabel;
		if(tree.contains("J48")){
			//The last two rows in treeSplit will give details about number of leaves and size of tree the first two lines will contain J48 (un)pruned and ------
			//
			startIndex = 2;
			endIndex = treeSplit.length-1;
		}else if(tree.contains("Id3")){
			startIndex = 1;
			endIndex = treeSplit.length;
		}else{
			startIndex = 0;
			endIndex = treeSplit.length;
		}
		
		for(int i = startIndex; i < endIndex; i++){
//			System.out.println(treeSplit[i]);
			if(treeSplit[i].contains("Number"))
				break;
			if(treeSplit[i].contains("|")){
				depth = treeSplit[i].split("\\|").length;
//				System.out.println("Depth: "+depth);
				if(treeSplit[i].contains(":")){
					ruleSplit = treeSplit[i].split("\\|")[depth-1].split(":");
					classLabel = ruleSplit[1].split("\\(")[0].trim();
					if(input.generateRulesForClassLabelSet.contains(classLabel)){
						rule = "IF ";
						for(int j = 0; j < depth-1; j++){
							rule += levelConstriantMap.get(j)+" AND ";
						}
						if(ruleSplit[0].contains("="))
							attributesInRuleSet.add(ruleSplit[0].split("=")[0]);
						else if(ruleSplit[0].contains("<"))
							attributesInRuleSet.add(ruleSplit[0].split("<")[0]);
						else if(ruleSplit[0].contains(">"))
							attributesInRuleSet.add(ruleSplit[0].split(">")[0]);
						
						rule += ruleSplit[0]+" THEN "+classLabel;
	//						System.out.println("RULE: "+rule);
						ruleList.add(rule);
					}
				}else{
					constraint = treeSplit[i].split("\\|")[depth-1];
					levelConstriantMap.put(depth-1, constraint);
					if(constraint.contains("="))
						attributesInRuleSet.add(constraint.split("=")[0]);
					else if(constraint.contains("<"))
						attributesInRuleSet.add(constraint.split("<")[0]);
					else if(constraint.contains(">"))
						attributesInRuleSet.add(constraint.split(">")[0]);
//					System.out.println("Constraint: "+constraint);
				}
			}else{
				if(treeSplit[i].contains(":")){
					//A rule with just one constraint (no depth of tree involved with |)
					ruleSplit = treeSplit[i].split(":");
					classLabel = ruleSplit[1].split("\\(")[0].trim();
					if(input.generateRulesForClassLabelSet.contains(classLabel)){
						ruleList.add("IF "+ruleSplit[0]+" THEN "+classLabel);
						if(ruleSplit[0].contains("="))
							attributesInRuleSet.add(ruleSplit[0].split("=")[0]);
						else if(ruleSplit[0].contains("<"))
							attributesInRuleSet.add(ruleSplit[0].split("<")[0]);
						else if(ruleSplit[0].contains(">"))
							attributesInRuleSet.add(ruleSplit[0].split(">")[0]);
					}
				}else{
					constraint = treeSplit[i];
					levelConstriantMap.put(0, treeSplit[i]);
					if(constraint.contains("="))
						attributesInRuleSet.add(constraint.split("=")[0]);
					else if(constraint.contains("<"))
						attributesInRuleSet.add(constraint.split("<")[0]);
					else if(constraint.contains(">"))
						attributesInRuleSet.add(constraint.split(">")[0]);
				}
			}
		}

		Logger.printReturn("Returning convertDecisionTreeToRules()");
		return ruleList;
	}
	
	private float[] getWeightedAverageMetrics(String classDetailEvaluationString){
		Logger.printCall("Calling getWeightedAverageMetrics()");
		float[] weightedAverageMetrics = new float[6];
		
		classDetailEvaluationString = classDetailEvaluationString.split("Weighted Avg.")[1].trim();
		String[] classDetailEvaluationStringSplit = classDetailEvaluationString.split(" ");
		int noSplits = classDetailEvaluationStringSplit.length;
		int currentIndex = 0;
		for(int i = 0; i < noSplits; i++){
			if(classDetailEvaluationStringSplit[i].trim().equalsIgnoreCase(""))
				continue;
			else{
				weightedAverageMetrics[currentIndex] = new Float(classDetailEvaluationStringSplit[i].trim()).floatValue();
				currentIndex++;
			}
		}
		
		Logger.printReturn("Returning getWeightedAverageMetrics()");
		return weightedAverageMetrics;
	}
	
	private Map<Feature, RuleListMetrics> findAssociationRules(){
//		System.out.println("In findAssociation Rules");
		float minSupport = new Float(input.getAssociationRuleSettings().getMinSupportValueStr()).floatValue();
		float minConfidence = new Float(input.getAssociationRuleSettings().getSortRulesMetricValueStr()).floatValue();
		ClassSpecificAssociationRuleMiner c = new ClassSpecificAssociationRuleMiner(featureNominalWekaInstancesMap, input.getGenerateSignaturesForClassLabelSet(), minSupport, minConfidence);
		return c.getFeatureRuleListMetrics();
	}
	
	private Map<Feature, RuleListMetrics> findAssociationRules(float minSupport, float minConfidence){
//		System.out.println("In findAssociation Rules");
		ClassSpecificAssociationRuleMiner c = new ClassSpecificAssociationRuleMiner(featureNominalWekaInstancesMap, input.getGenerateSignaturesForClassLabelSet(), minSupport, minConfidence);
		return c.getFeatureRuleListMetrics();
	}
	
	private String getParameterOptions(){
		String optionsString = "";
		if(input.learningAlgorithmType == LearningAlgorithmType.J48){
			if(input.j48Settings.isPruneTrees){
				if(input.j48Settings.isPessimisticErrorPruning){
					optionsString = optionsString.concat("-C ").concat(input.j48Settings.confidenceFactorFoldsStr).concat(" -M 1");
				}else{
					optionsString = optionsString.concat("-R ").concat("-N ").concat(input.j48Settings.confidenceFactorFoldsStr).concat(" -Q 1 -M 1");
				}
			}else{
				optionsString = optionsString.concat("-U -M 1");
			}
		}else if(input.learningAlgorithmType == LearningAlgorithmType.WekaAssociationRules){
			optionsString = optionsString.concat("-N ").concat(input.noRulesToGenerate+" ");
			optionsString = optionsString.concat("-T ").concat(input.associationRuleSettings.sortRulesMetricStr);
			optionsString = optionsString.concat("-C ").concat(input.associationRuleSettings.sortRulesMetricValueStr);
			optionsString = optionsString.concat("-D 0.05 ");
			optionsString = optionsString.concat("-U ").concat(input.associationRuleSettings.maxSupportValueStr);
			optionsString = optionsString.concat("-M ").concat(input.associationRuleSettings.minSupportValueStr);
			if(input.associationRuleSettings.isClassAssociationRules){
				optionsString = optionsString.concat("-A ");
			}
			//Indicate that the last attribute is the one that holds the class label
			optionsString = optionsString.concat("-c -1");
		}else if(input.learningAlgorithmType == LearningAlgorithmType.AssociationRules){
			optionsString = optionsString.concat("-minSupport ").concat(input.associationRuleSettings.getMinSupportValueStr());
			optionsString = optionsString.concat("-minConfidence ").concat(input.associationRuleSettings.getSortRulesMetricValueStr());
		}
		Logger.printReturn("Returning getParameterOptions()");
		return optionsString;
	}

	private void chooseBestRuleList(Map<String, Map<Feature, RuleListMetrics>> optionsStringFeatureRuleListMetricsMap){
		Map<Feature, RuleListMetrics> featureRuleListMetricsMap;
		RuleListMetrics ruleListMetrics;
		Metrics metrics;
		double f1Score;
		Map<String, Metrics> classMetricsMap;
		
		maxOptionsString="null";
		maxClass = "";
		maxFeature = Feature.None;
		maxF1Score = 0;
		
		for(String optionsString : optionsStringFeatureRuleListMetricsMap.keySet()){
			featureRuleListMetricsMap = optionsStringFeatureRuleListMetricsMap.get(optionsString);
			for(Feature feature : featureRuleListMetricsMap.keySet()){
				
				ruleListMetrics = featureRuleListMetricsMap.get(feature);
				classMetricsMap = ruleListMetrics.getClassMetricsMap();
				for(String classValue : classMetricsMap.keySet()){
					metrics = classMetricsMap.get(classValue);
					f1Score = metrics.getF1Score();
					if(f1Score > maxF1Score){
						maxOptionsString = optionsString;
						maxFeature = feature;
						maxF1Score = f1Score;
						maxClass = classValue;
					}
				}
			}
		}
		
//		System.out.println("Max F1 Score: "+maxF1Score);
//		System.out.println("Max Options String: "+maxOptionsString);
//		System.out.println("Max FeatureType: "+maxFeature);
		
		if(maxOptionsString.equals("null") || maxFeature.equals(Feature.None)){
			hasSignatures = false;
			return;
		}
		
		featureRuleListMetricsMap = optionsStringFeatureRuleListMetricsMap.get(maxOptionsString);
		ruleListMetrics = featureRuleListMetricsMap.get(maxFeature);
		finalRuleList = ruleListMetrics.getRuleList();
		prepareEncodedDecodedRuleListMap();
		
		
		if(maxOptionsString.contains("Nominal"))
			evaluateRuleList(finalRuleList, featureNominalWekaInstancesMap.get(maxFeature));
		else
			evaluateRuleList(finalRuleList, featureNumericWekaInstancesMap.get(maxFeature));
		classMetricsMap = ruleListMetrics.getClassMetricsMap();
		metrics = classMetricsMap.get(maxClass);
	}
	
	private void prepareEncodedDecodedRuleListMap(){
		if(encodedDecodedRuleMap == null){
			encodedDecodedRuleMap = new HashMap<String, String>();
		}else{
			encodedDecodedRuleMap.clear();
		}
		
		String[] ruleSplit; 
		String[] antecedantSplit;

		StringBuilder decodedRuleStringBuilder = new StringBuilder();
		for(String encodedRule : finalRuleList){
			
			ruleSplit = encodedRule.replaceAll("IF ", "").split(" THEN ");
			antecedantSplit = ruleSplit[0].split(" AND ");
//			System.out.println(encodedRule+"@"+antecedantSplit.length);
			decodedRuleStringBuilder.setLength(0);
			decodedRuleStringBuilder.append("IF ");
			for(int i = 0; i < antecedantSplit.length; i++){
				if(antecedantSplit[i].contains(">=")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],">="));
				}else if(antecedantSplit[i].contains(">")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],">"));
				}else if(antecedantSplit[i].contains("<=")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],"<="));
				}else if(antecedantSplit[i].contains("<")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],"<"));
				}else if(antecedantSplit[i].contains("=")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],"="));
				}
				
				if(i < antecedantSplit.length-1){
					decodedRuleStringBuilder.append(" AND ");
				}
			}
			
			decodedRuleStringBuilder.append(" THEN ").append(ruleSplit[1]);
			
			encodedDecodedRuleMap.put(encodedRule, decodedRuleStringBuilder.toString());
		}
	}
	
	private String getDecodedAntecedant(String antecedant, String constraint){
		StringBuilder decodedAntecedant = new StringBuilder();
		String[] featureSplit = antecedant.split(constraint);
		String[] activitySplit = featureSplit[0].replaceAll("\\[", "").replaceAll("\\]", "").split("_");
		
		if(antecedant.contains("["))
			decodedAntecedant.append("[");
		int noActivites = activitySplit.length;
		int index = 0;
		int featureLength;
		for(String encodedActivity : activitySplit){
			if(encodedActivity.length() == encodingLength){
				decodedAntecedant.append(charActivityMap.get(encodedActivity.trim()));
				index++;
				if(index < noActivites)
					decodedAntecedant.append(", ");
			}else{
				//Sequence Feature
				featureLength = encodedActivity.length()/encodingLength;
				for(int i = 0; i < featureLength; i++){
					decodedAntecedant.append(charActivityMap.get(encodedActivity.substring(i*encodingLength, (i+1)*encodingLength)));
				}
			}
		}	
		
		if(antecedant.contains("]"))
			decodedAntecedant.append("]");
		decodedAntecedant.append(constraint).append(featureSplit[1]);
		
		return decodedAntecedant.toString();
	}
	
	private void evaluateRuleList(List<String> ruleList, Instances data){
		Logger.printCall("Calling evaluateRuleList()");
		
		if(finalRuleListMetricsMap == null){
			finalRuleListMetricsMap = new HashMap<String, Metrics>();
		}else{
			finalRuleListMetricsMap.clear();
		}
		
		int noInstances = data.numInstances();
		Instance instance;
		
		String[] ruleSplit;
		String[] antecedantSplit, antecedantContraintSplit;

		int noAntecedants;
		List<String> antecedantAttributeList = new ArrayList<String>();
		List<String> antecedantConstraintList = new ArrayList<String>();
		double numericValue;
		String nominalValue;
		boolean isAntecedantConstraintsSatisfied;
		List<Integer> ruleSatisifyingInstanceList = new ArrayList<Integer>();
		List<Integer> constraintSatisifyingInstanceList = new ArrayList<Integer>();
		
		Logger.println("No. Instances: "+data.numInstances()+" @ No.Rules: "+ruleList.size());
		
		int noInstancesWithRuleClassValue;
		int tp, fp, tn, fn;
		for(String rule : ruleList){
			ruleSplit = rule.replaceAll("IF ", "").split(" THEN ");
			antecedantSplit = ruleSplit[0].split(" AND ");
			noAntecedants = antecedantSplit.length;
			
			Logger.println("Rule: "+rule);
			Logger.println("No. Antecedants: "+noAntecedants);
			
			antecedantAttributeList.clear();
			antecedantConstraintList.clear();
			for(String antecedant : antecedantSplit){
				if(antecedant.contains(">=")){
					antecedantContraintSplit = antecedant.split(">=");
				}else if(antecedant.contains("<=")){
					antecedantContraintSplit = antecedant.split("<=");
				}else if(antecedant.contains("=")){
					antecedantContraintSplit = antecedant.split("=");
				}else if(antecedant.contains(">")){
					antecedantContraintSplit = antecedant.split(">");
				}else{
					antecedantContraintSplit = antecedant.split("<");
				}
				
				Logger.println(antecedant+"@("+antecedantContraintSplit[0].trim()+","+data.attribute(antecedantContraintSplit[0].trim()).toString()+")@"+antecedantContraintSplit[1]);
				
				antecedantAttributeList.add(antecedantContraintSplit[0].trim());
				antecedantConstraintList.add(antecedantContraintSplit[1].trim());
			}
			
			Logger.println("Consequent: "+ruleSplit[1]);
			noInstancesWithRuleClassValue = 0;
			
			constraintSatisifyingInstanceList.clear();
			ruleSatisifyingInstanceList.clear();
			fn = tn = tp = fp = 0;
			for(int i = 0; i < noInstances; i++){
				instance = data.instance(i);
				
				if(instance.stringValue(instance.classAttribute()).equals(ruleSplit[1].trim()))
					noInstancesWithRuleClassValue++;
				
				isAntecedantConstraintsSatisfied = true;
				for(int j = 0; j < noAntecedants; j++){
					if(antecedantSplit[j].contains(">=")){
						numericValue = instance.value(data.attribute(antecedantAttributeList.get(j)));
						if(!(numericValue >= new Double(antecedantConstraintList.get(j)).doubleValue())){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}else if(antecedantSplit[j].contains("<=")){
						numericValue = instance.value(data.attribute(antecedantAttributeList.get(j)));
						if(!(numericValue <= new Double(antecedantConstraintList.get(j)).doubleValue())){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}else if(antecedantSplit[j].contains(">")){
						numericValue = instance.value(data.attribute(antecedantAttributeList.get(j)));
						if(!(numericValue > new Double(antecedantConstraintList.get(j)).doubleValue())){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}else if(antecedantSplit[j].contains("<")){
						numericValue = instance.value(data.attribute(antecedantAttributeList.get(j)));
						if(!(numericValue < new Double(antecedantConstraintList.get(j)).doubleValue())){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}else{
						//Nominal attributes have only constraints as =
						nominalValue = instance.stringValue(data.attribute(antecedantAttributeList.get(j)));
//						System.out.println(antecedantAttributeList.get(j)+"@"+data.attribute(antecedantAttributeList.get(j))+"@ Nominal value: "+nominalValue+"@ Cons: "+antecedantConstraintList.get(j));
						if(!nominalValue.equals(antecedantConstraintList.get(j))){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}
				}
				if(isAntecedantConstraintsSatisfied){
					constraintSatisifyingInstanceList.add(i);
					//Check consequent satisfaction
					if(instance.stringValue(instance.classAttribute()).equals(ruleSplit[1].trim())){
//						System.out.println("Adding Instance: "+i);
						ruleSatisifyingInstanceList.add(i);
						tp++;
					}else{
						fp++;
					}
				}else{
					if(instance.stringValue(instance.classAttribute()).equals(ruleSplit[1].trim())){
						fn++;
					}else{
						tn++;
					}
				}
			}
			
			Logger.println("No. Constraints Satisfying Instances: "+constraintSatisifyingInstanceList.size());
			Logger.println("Constraint Satisfying Instance List: "+constraintSatisifyingInstanceList);
			Logger.println("No. Rule Satisfying Instances: "+ruleSatisifyingInstanceList.size());
			Logger.println("Rule Satisfying Instance List: "+ruleSatisifyingInstanceList);
			finalRuleListMetricsMap.put(rule, new Metrics(tp,fp,tn,fn,noInstances));
		}
		Logger.printReturn("Returning evaluateRuleList()");
	}
	
	/*public SignaturePatternsFrame getSignaturePatternsFrame(){
		SignaturePatternsFrame signaturePatternsFrame = new SignaturePatternsFrame(this);
		return signaturePatternsFrame;
	}*/

	public List<String> getFinalRuleList() {
		return finalRuleList;
	}

	public Map<String, Metrics> getFinalRuleListMetricsMap() {
		return finalRuleListMetricsMap;
	}

	public Map<String, String> getEncodedDecodedRuleMap() {
		return encodedDecodedRuleMap;
	}
	
	public SignatureDiscoveryInput getSignatureDiscoveryInput(){
		return input;
	}

	public String getMaxOptionsString() {
		return maxOptionsString;
	}

	public Feature getMaxFeature() {
		return maxFeature;
	}

	public double getMaxF1Score() {
		return maxF1Score;
	}
	
	public boolean hasSignatures(){
		return hasSignatures;
	}
}
