package com.company.SignatureDiscovery.settings;

import java.util.Set;
import org.processmining.plugins.signaturediscovery.types.EvaluationOptionType;
import org.processmining.plugins.signaturediscovery.types.FeatureType;
import org.processmining.plugins.signaturediscovery.types.LearningAlgorithmType;

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

public interface SignatureDiscoverySettingsListener {
	public void clearFeatureSelection();
	public void featureSelectionChanged(String featureString, boolean isSelected);
	public void setFrequencyCount(boolean isNominalCount);
	public void setFeatureCombination(String combinationString);
	public void setFeatureType(FeatureType featureType);
	public void setLearningAlgorithmType(LearningAlgorithmType learningAlgorithmType);
	public void setBaseFeatures(boolean isBaseFeatures);
	public void setJ48Parameters(boolean isPruneTrees, boolean isPessimisticErrorPruning,
      String confidenceFactorFoldsStr);
	public void setAssociationRuleParameters(boolean isClassAssociationRules,
      String sortRulesMetricStr, String sortRulesMetricValueStr, String minSupportValueStr,
      String maxSupportValueStr);
	public void setEvaluationOptions(EvaluationOptionType evaluationOptionType,
      String noFoldsPercentageSplitValueStr);
	public void setSignatureClassOptions(Set<String> generateSignaturesForClassLabelSet,
      int noRulesToGenerate);
	public void setKGramValue(int kGramValue);
	public void setNominalFeatureCount(boolean isNominalCount);
}
