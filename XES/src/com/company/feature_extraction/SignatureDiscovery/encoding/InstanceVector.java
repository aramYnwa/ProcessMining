package com.company.feature_extraction.SignatureDiscovery.encoding;

import java.util.List;
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

public class InstanceVector {
	Map<String, Integer> featureCountMap;
	Map<Set<String>, Integer> featureAlphabetCountMap;
	int[] standardizedNumericVector;
	int[] standardizedNominalVector;
	
	String label;
	
	public InstanceVector(){
		
	}
	
	public Map<String, Integer> getSequenceFeatureCountMap(){
		return featureCountMap;
	}
	
	public Map<Set<String>, Integer> getAlphabetFeatureCountMap(){
		return featureAlphabetCountMap;
	}
	
	public void setSequenceFeatureCountMap(Map<String, Integer> featureCountMap){
		this.featureCountMap = featureCountMap;
	}
	
	public void setAlphabetFeatureCountMap(Map<Set<String>, Integer> featureAlphabetCountMap){
		this.featureAlphabetCountMap = featureAlphabetCountMap;
	}
	
	public String getLabel(){
		return label;
	}
	
	public void setLabel(String label){
		this.label = label;
	}
	
	public <E> int[] toNumericAttributeVector(List<E> featureList){
		int[] attributeVector = new int[featureList.size()];
		int noFeatures = featureList.size();
		for(int i = 0; i < noFeatures; i++){
			if(featureCountMap != null && featureCountMap.containsKey(featureList.get(i)))
				attributeVector[i] = featureCountMap.get(featureList.get(i));
			else if(featureAlphabetCountMap != null && featureAlphabetCountMap.containsKey(featureList.get(i)))
				attributeVector[i] = featureAlphabetCountMap.get(featureList.get(i));
			else
				attributeVector[i] = 0;
		}
		return attributeVector;
	}
	
	public <E> String[] toNominalAttributeVector(List<E> featureList){
		String[] attributeVector = new String[featureList.size()];
		int noFeatures = featureList.size();
		for(int i = 0; i < noFeatures; i++){
			if(featureCountMap != null && featureCountMap.containsKey(featureList.get(i)))
				attributeVector[i] = "Y";
			else if(featureAlphabetCountMap != null && featureAlphabetCountMap.containsKey(featureList.get(i)))
				attributeVector[i] = "Y";
			else
				attributeVector[i] = "N";
		}
		return attributeVector;
	}
	
	public String toStringNumericVector(int[] numericVector){
		String str = numericVector[0]+"";
		for(int i = 1; i < numericVector.length; i++)
			str = str+","+numericVector[i];
		str += ","+label;
		return str;
	}
	
	public String toStringNominalVector(String[] nominalVector){
		String str = nominalVector[0];
		for(int i = 1; i < nominalVector.length; i++)
			str = str+","+nominalVector[i];
		str += ","+label;
		return str;
	}
	
	public String toStringStandarizedNumericVector(){
		String str = standardizedNumericVector[0]+"";
		for(int i = 1; i < standardizedNumericVector.length; i++)
			str = str+","+standardizedNumericVector[i];
		str += ","+label;
		return str;
	}
	
	public String toStringStandarizedNominalVector(){
		String str = standardizedNominalVector[0]+"";
		for(int i = 1; i < standardizedNominalVector.length; i++)
			str = str+","+standardizedNominalVector[i];
		str += ","+label;
		return str;
	}
	
	public <E> void standarizeNumericVector(List<E> featureList){
//		Logger.printCall("Calling InstanceVector->standardizeNumericVector()");
		int noFeatures = featureList.size();
		standardizedNumericVector = new int[noFeatures];
		
		for(int i = 0; i < noFeatures; i++){
			if(featureCountMap != null && featureCountMap.containsKey(featureList.get(i)))
				standardizedNumericVector[i] = featureCountMap.get(featureList.get(i));
			else if(featureAlphabetCountMap != null && featureAlphabetCountMap.containsKey(featureList.get(i)))
				standardizedNumericVector[i] = featureAlphabetCountMap.get(featureList.get(i));
			else
				standardizedNumericVector[i] = 0;
		}
//		Logger.printReturn("Returning InstanceVector->standardizeNumericVector()");
	}
	
	public <E> void standarizeNominalVector(List<E> featureList){
//		Logger.printCall("Calling InstanceVector->standardizeNominalVector()");
		
		int noFeatures = featureList.size();
		standardizedNominalVector = new int[noFeatures];
		
		for(int i = 0; i < noFeatures; i++){
			if(featureCountMap != null && featureCountMap.containsKey(featureList.get(i)))
				standardizedNominalVector[i] = 1;
			else if(featureAlphabetCountMap != null && featureAlphabetCountMap.containsKey(featureList.get(i)))
				standardizedNominalVector[i] = 1;
			else
				standardizedNominalVector[i] = 0;
		}
		
//		Logger.printReturn("Returning InstanceVector->standardizeNominalVector()");
	}

	public int[] getStandardizedNumericVector() {
		return standardizedNumericVector;
	}

	public void setStandardizedNumericVector(int[] standardizedVector) {
		this.standardizedNumericVector = standardizedVector;
	}
	
	public int[] getStandardizedNominalVector() {
		return standardizedNominalVector;
	}

	public void setStandardizedNominalVector(int[] standardizedVector) {
		this.standardizedNominalVector = standardizedVector;
	}
}
