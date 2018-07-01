package com.company.SignatureDiscovery.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

public class EquivalenceClass {
	class DescendingStringLengthComparator implements Comparator<String>{
		public int compare(String str1, String str2){
			return (str1.length() < str2.length()) ? 1 : -1;
//			return str1.compareTo(str2) * (str1.length() < str2.length() ? 1 : -1);
		}
	}
	
	class AscendingStringLengthComparator implements Comparator<String>{
		public int compare(String str1, String str2){
			return (str1.length() >= str2.length()) ? 1 : -1;
		}
	}
	
	/*
	/*
	 * featureSet: set of features ("ab0ab1", "cd0cd1cd2", "de0de1", ...), encodingLength=3
	 * encodingLength: length of encoded activity name (the same for all activities)
	 * Return: map from the feature to a set of its activities in encoded form
	 * "ab0ab1"		, ("ab0", "ab1")
	 * "cd0cd1cd2"	, ("cd0", "cd1", "cd2")
	 * "de0de1"		, ("de0", "de1")
	 */
	public Map<String, TreeSet<String>> getPatternAlphabetMap(int encodingLength, Set<String> featureSet){
		Map<String, TreeSet<String>> featureAlphabetMap = new HashMap<String, TreeSet<String>>();
		int featureLength;
		TreeSet<String> featureAlphabet;
		for(String feature : featureSet){
			featureLength = feature.length()/encodingLength;
			featureAlphabet = new TreeSet<String>();
			for(int i = 0; i < featureLength; i++)
				featureAlphabet.add(feature.substring(i*encodingLength, (i+1)*encodingLength));
			featureAlphabetMap.put(feature, featureAlphabet);
		}
		
		return featureAlphabetMap;
	}
	
	/* Create feature alphabet for every feature
	 * featureSet: contains features ("ab0ab1", "cd0cd1cd2", "de0de1", ...), encodingLength=3
	 * encodingLength: length of encoded activity name (the same for all activities)
	 * Return: map from a treeset to a treeset, each (key, value) pair is for one feature in the featureSet
	 * Key: a set of feature alphabet, e.g. ("ab0", "ab1", "ab2")
	 * Value: set of feature which have the same alphabet in the key
	 * Example of return:
	 * ("ab0", "ab1"), ("ab0ab1", "ab1ab0", "ab0ab0ab1", "ab0ab1ab0ab1")
	 * ("cd0", "cd1", "cd2), ("cd0cd1cd2", "cd0cd2cd1", "cd1cd0cd0cd0cd2")
	 * In the above example:
	 * 		"ab0ab1", "ab1ab0", "ab0ab0ab1", "ab0ab1ab0ab1" are equivalence classes of alphabet ("ab0", "ab1")
	 * 		"cd0cd1cd2", "cd0cd2cd1", "cd1cd0cd0cd0cd2" are equivalence classes of alphabet ("cd0", "cd1", "cd2)
	 */
	public Map<Set<String>, Set<String>> getAlphabetEquivalenceClassMap(int encodingLength, Set<String> featureSet){
		Map<Set<String>, Set<String>> alphabetEquivalenceClassMap = new HashMap<Set<String>, Set<String>>();
		
		Set<String> featureAlphabet;
		TreeSet<String> featureAlphabetEquivalenceSet;
		int featureLength;
		for(String feature : featureSet){
			featureLength = feature.length()/encodingLength;
			featureAlphabet = new TreeSet<String>();
			
			// Note that only add distinct alphabet components (due to Set type)
			for(int i = 0; i < featureLength; i++){
				featureAlphabet.add(feature.substring(i*encodingLength, (i+1)*encodingLength));
			}
			
			if(alphabetEquivalenceClassMap.containsKey(featureAlphabet)){
				featureAlphabetEquivalenceSet = (TreeSet<String>)alphabetEquivalenceClassMap.get(featureAlphabet);
			}else{
				featureAlphabetEquivalenceSet = new TreeSet<String>(new DescendingStringLengthComparator());
			}
			featureAlphabetEquivalenceSet.add(feature);
			alphabetEquivalenceClassMap.put(featureAlphabet, featureAlphabetEquivalenceSet);
		}
		
		return alphabetEquivalenceClassMap;
	}
	
	/*
	 * See other getStartSymbolEquivalenceClassMap
	 */
	public Map<String, Set<String>> getStartSymbolEquivalenceClassMap(int encodingLength, Set<String> featureSet, boolean isDescending){
		Map<String, Set<String>> startSymbolEquivalenceClassMap = new HashMap<String, Set<String>>();
		
		TreeSet<String> startSymbolFeatureSet;
		String startSymbol;
		for(String feature : featureSet){
			startSymbol = feature.substring(0, encodingLength);
			if(startSymbolEquivalenceClassMap.containsKey(startSymbol)){
				startSymbolFeatureSet = (TreeSet<String>)startSymbolEquivalenceClassMap.get(startSymbol);
			}else{
				if(isDescending)
					startSymbolFeatureSet = new TreeSet<String>(new DescendingStringLengthComparator());
				else
					startSymbolFeatureSet = new TreeSet<String>();
			}
			startSymbolFeatureSet.add(feature);
//			System.out.println(startSymbolFeatureSet);
			startSymbolEquivalenceClassMap.put(startSymbol, startSymbolFeatureSet);
		}
		
		return startSymbolEquivalenceClassMap;
	}
	
	/*
	 * featurSet: set of features selected: (ab0ab1ab2, ab0ab3, cd0cd1, cd0cd2, de0de1, ef0ef1ef2ef3...)
	 * Return: a map including a start activity as string and a tree set of feature containing that activity at start
	 * The key is the first unique activity of every feature 
	 * The value is a treeset containing all features having the same activity (key) as the start activity
	 * The treeset is sorted from the longest to the shortest feature
	 * ab0, (ab0ab1ab2, ab0ab3)
	 * cd0, (cd0cd1, cd0cd2)
	 * de0, (de0de1)
	 * ef0, (ef0ef1ef2ef3)
	 */
	public Map<String, Set<String>> getStartSymbolEquivalenceClassMap(int encodingLength, Set<String> featureSet){
		//Logger.printCall("Calling getStartSymbolEquivalenceClassMap: "+featureSet.size());
		Map<String, Set<String>> startSymbolEquivalenceClassMap = new HashMap<String, Set<String>>();
		
		TreeSet<String> startSymbolFeatureSet;
		String startSymbol;
		for(String feature : featureSet){
			startSymbol = feature.substring(0, encodingLength);
			if(startSymbolEquivalenceClassMap.containsKey(startSymbol)){
				startSymbolFeatureSet = (TreeSet<String>)startSymbolEquivalenceClassMap.get(startSymbol);
			}else{
				startSymbolFeatureSet = new TreeSet<String>(new DescendingStringLengthComparator());
			}
			startSymbolFeatureSet.add(feature);
//			System.out.println(startSymbolFeatureSet);
			startSymbolEquivalenceClassMap.put(startSymbol, startSymbolFeatureSet);
		}
		//Logger.printReturn("Returning getStartSymbolEquivalenceClassMap");
		return startSymbolEquivalenceClassMap;
	}
	
	/*
	 * See other getStartSymbolEquivalenceClassMap
	 */
	public Map<String, Set<Set<String>>> getStartSymbolEquivalenceClassAlphabetMap(int encodingLength, Set<String> featureSet){
		Map<String, Set<String>> startSymbolEquivalenceClassMap = getStartSymbolEquivalenceClassMap(encodingLength, featureSet);
		
		Map<String, Set<Set<String>>> startSymbolEquivalenceClassAlphabetMap = new HashMap<String, Set<Set<String>>>();
		
		Map<String, Set<String>> featureAlphabetMap = new HashMap<String, Set<String>>();
		int featureLength;
		Set<String> featureAlphabet;
		for(String feature : featureSet){
			featureLength = feature.length()/encodingLength;
			featureAlphabet = new TreeSet<String>();
			for(int i = 0; i < featureLength; i++){
				featureAlphabet.add(feature.substring(i*encodingLength, (i+1)*encodingLength));
			}
			featureAlphabetMap.put(feature, featureAlphabet);
		}
		
		Set<String> startSymbolEquivalenceClassPatternSet;
		Set<Set<String>>	startSymbolEquivalenceClassAlphabetSet;
		for(String startSymbol : startSymbolEquivalenceClassMap.keySet()){
			startSymbolEquivalenceClassPatternSet = startSymbolEquivalenceClassMap.get(startSymbol);
			startSymbolEquivalenceClassAlphabetSet = new HashSet<Set<String>>();
			for(String pattern : startSymbolEquivalenceClassPatternSet){
				startSymbolEquivalenceClassAlphabetSet.add(featureAlphabetMap.get(pattern));
			}
			startSymbolEquivalenceClassAlphabetMap.put(startSymbol, startSymbolEquivalenceClassAlphabetSet);
		}
		return startSymbolEquivalenceClassAlphabetMap;
	}
}

