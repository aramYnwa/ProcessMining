package com.company.feature_extraction.SignatureDiscovery.util;

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
	
	public Map<Set<String>, Set<String>> getAlphabetEquivalenceClassMap(int encodingLength, Set<String> featureSet){
		Map<Set<String>, Set<String>> alphabetEquivalenceClassMap = new HashMap<Set<String>, Set<String>>();
		
		Set<String> featureAlphabet;
		TreeSet<String> featureAlphabetEquivalenceSet;
		int featureLength;
		for(String feature : featureSet){
			featureLength = feature.length()/encodingLength;
			featureAlphabet = new TreeSet<String>();
			
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
	
	public Map<String, Set<String>> getStartSymbolEquivalenceClassMap(int encodingLength, Set<String> featureSet){
		Logger.printCall("Calling getStartSymbolEquivalenceClassMap: "+featureSet.size());
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
		Logger.printReturn("Returning getStartSymbolEquivalenceClassMap");
		return startSymbolEquivalenceClassMap;
	}
	
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

