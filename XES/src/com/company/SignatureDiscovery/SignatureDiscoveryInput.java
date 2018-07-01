package com.company.SignatureDiscovery;

import com.company.SignatureDiscovery.settings.AssociationRuleSettings;
import com.company.SignatureDiscovery.types.EvaluationOptionType;
import com.company.SignatureDiscovery.types.Feature;
import com.company.SignatureDiscovery.types.FeatureCombination;
import com.company.SignatureDiscovery.types.FeatureType;
import com.company.SignatureDiscovery.types.LearningAlgorithmType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


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

class J48Settings{
	String confidenceFactorFoldsStr;
	boolean isPruneTrees;
	boolean isPessimisticErrorPruning;
	
	public J48Settings(boolean isPruneTrees, boolean isPessimisticErrorPruning, String confidenceFactorFoldsStr){
		this.isPruneTrees = isPruneTrees;
		this.isPessimisticErrorPruning = isPessimisticErrorPruning;
		this.confidenceFactorFoldsStr = confidenceFactorFoldsStr;
	}
}

class EvaluationOptions{
	EvaluationOptionType evaluationOptionType;
	String noFoldsPercentageSplitValueStr;
	
	public EvaluationOptions(EvaluationOptionType evaluationOptionType, String noFoldsPercentageSplitValueStr){
		this.evaluationOptionType = evaluationOptionType;
		this.noFoldsPercentageSplitValueStr = noFoldsPercentageSplitValueStr;
	}
}

/*
 * Bruce
 * This class consolidates all user-defined settings for the pattern discovery
 */
public class SignatureDiscoveryInput {
	Map<String, Feature> featureStringValueFeatureMap;
	Set<Feature> selectedFeatureSet;
	FeatureType featureType;
	FeatureCombination featureCombination;
	LearningAlgorithmType learningAlgorithmType;
	J48Settings j48Settings;
	AssociationRuleSettings associationRuleSettings;
	EvaluationOptions evaluationOptions;
	
	Set<String> generateRulesForClassLabelSet;
	int noRulesToGenerate;
	int kGramValue;
	
	boolean isBaseFeatures;
	boolean isNominalCount;
	
	public SignatureDiscoveryInput(){
		this.selectedFeatureSet = new HashSet<Feature>();
		createFeatureStringValueFeatureMap();
	}
	
	private void createFeatureStringValueFeatureMap(){
		featureStringValueFeatureMap = new HashMap<String, Feature>();
		featureStringValueFeatureMap.put("Individual Event", Feature.IE);
		featureStringValueFeatureMap.put("K-Gram", Feature.KGram);
		featureStringValueFeatureMap.put("Tandem Repeat", Feature.TR);
		featureStringValueFeatureMap.put("Maximal Repeat", Feature.MR);
		featureStringValueFeatureMap.put("Super Maximal Repeat", Feature.SMR);
		featureStringValueFeatureMap.put("Near Super Maximal Repeat", Feature.NSMR);
		featureStringValueFeatureMap.put("Tandem Repeat Alphabet", Feature.TRA);
		featureStringValueFeatureMap.put("Maximal Repeat Alphabet", Feature.MRA);
		featureStringValueFeatureMap.put("Super Maximal Repeat Alphabet", Feature.SMRA);
		featureStringValueFeatureMap.put("Near Super Maximal Repeat Alphabet", Feature.NSMRA);
	}
	
	public void addFeature(String featureString){
		selectedFeatureSet.add(featureStringValueFeatureMap.get(featureString));
	}
	
	public void removeAllFeatures(){
		selectedFeatureSet.clear();
	}
	
	public void removeFeature(String featureString){
		selectedFeatureSet.remove(featureStringValueFeatureMap.get(featureString));
	}
	
	public void setFeatureCombination(FeatureCombination featureCombination){
		this.featureCombination = featureCombination;
	}
	
	public void setFeatureType(FeatureType featureType){
		this.featureType = featureType;
	}
	
	public Set<Feature> getSelectedFeatureSet(){
		return selectedFeatureSet;
	}
	
	public void setBaseFeatures(boolean isBaseFeatures){
		this.isBaseFeatures = isBaseFeatures;
	}
	
	public void setNominalCount(boolean isNominalCount){
		this.isNominalCount = isNominalCount;
	}
	
	public void setFeatureCombination(String combinationString){
		if(combinationString.equalsIgnoreCase("Best"))
			featureCombination = FeatureCombination.Best;
		else if(combinationString.equalsIgnoreCase("Mix"))
			featureCombination = FeatureCombination.Mix;
		else
			featureCombination = FeatureCombination.All;
	}
	
	public void setLearningAlgorithmType(LearningAlgorithmType learningAlgorithmType){
		this.learningAlgorithmType = learningAlgorithmType;
	}
	
	public void setJ48Parameters(boolean isPruneTrees, boolean isPessimisticErrorPruning, String confidenceFactorFoldsStr){
		j48Settings = new J48Settings(isPruneTrees, isPessimisticErrorPruning, confidenceFactorFoldsStr);
	}
	
	public void setAssociationRuleParameters(boolean isClassAssociationRules, String sortRulesMetricStr, String sortRulesMetricValueStr, String minSupportValueStr, String maxSupportValueStr){
		associationRuleSettings = new AssociationRuleSettings(isClassAssociationRules, sortRulesMetricStr, sortRulesMetricValueStr, minSupportValueStr, maxSupportValueStr);
	}
	
	public void setEvaluationOptions(EvaluationOptionType evaluationOptionType, String noFoldsPercentageSplitValueStr){
		evaluationOptions = new EvaluationOptions(evaluationOptionType, noFoldsPercentageSplitValueStr);
	}
	
	public Set<String> getGenerateSignaturesForClassLabelSet(){
		return generateRulesForClassLabelSet;
	}
	
	public void setGenerateSignaturesForClassLabelSet(Set<String> generateSignaturesForClassLabelSet){
		this.generateRulesForClassLabelSet = generateSignaturesForClassLabelSet;
	}
	
	public void setNoRulesToGenerate(int noRulesToGenerate){
		this.noRulesToGenerate = noRulesToGenerate;
	}
	
	public void setKGramValue(int kGramValue){
		this.kGramValue = kGramValue;
	}
	
	public AssociationRuleSettings getAssociationRuleSettings(){
		return associationRuleSettings;
	}

	public LearningAlgorithmType getLearningAlgorithmType() {
		return learningAlgorithmType;
	}
}
