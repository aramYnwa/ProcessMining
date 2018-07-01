package com.company.SignatureDiscovery.featureextraction;

import com.company.SignatureDiscovery.encoding.InstanceProfile;
import com.company.SignatureDiscovery.types.Feature;
import com.company.SignatureDiscovery.util.EquivalenceClass;
import com.company.SignatureDiscovery.util.FileIO;
import com.company.SignatureDiscovery.util.Logger;
import com.company.SignatureDiscovery.util.UkkonenSuffixTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
public class FeatureExtraction {
	int encodingLength;
	int kGramValue;
	boolean hasSequenceFeature = false;
	boolean hasAlphabetFeature = false;

	/*
	 * Example: IE, TR, MR
	 */
	Set<Feature> selectedFeatureSet;
	
	/* List of all instances with the following format
	 * 		ID	 , Label  , Encoded Trace
	 * 		S0001, "Quick", ab0cd1ef3ce3
	 * 		S0002, "Quick", ab0cd5de2ft3
	 * 		S0003, "Slow, ab1cd3de4cf4ek5xd3xd2	  
	 */
	List<InstanceProfile> instanceProfileList;
	
	
	List<InstanceProfile> modifiedInstanceProfileList;
	
	/* Mapping from real activity name to encoded name
	 * 		"New Claim", ab0
	 * 		"Contact Customer", cd1
	 * 		"Generate Payment", xw3 
	 */
	Map<String, String> activityCharMap;
	
	/* Mapping from encoded activity name to real activity name
	 * 		ab0, "New Claim"
	 * 		cd1, "Contact Customer"
	 * 		xw3, "Generate Payment"
	 */
	Map<String, String> charActivityMap;
	
	/* Map from feature type to sequence feature set
	 * Example sequence feature set
	 * (TR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 * (MR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 */
	Map<Feature, Set<String>> originalSequenceFeatureSetMap;
	
	/* Map from feature type (TR,MR,...) to a map which maps encoded activity names to a set of feature
	 * The encoded activity names are like alphabet components used to form every feature. See example.
	 * Example
	 * ("ab0", "ab1"), ("ab0ab1", "ab1ab0")
	 * ("cd0", "cd1", "cd2), ("cd0cd1cd2", "cd0cd2cd1", "cd1cd0cd2", "cd1cd2cd0")
	 */
	Map<Feature, Map<Set<String>, Set<String>>> originalAlphabetFeatureSetMap;
	
	/*
	 * (TR, [(ab0ab1,2), (cd1cd2cd3,3), (de1de2,1),...,(xy0xy1,2)])
	 */	
	Map<Feature, Map<String, Integer>> originalSequenceFeatureCountMap;
	
	/*
	 * (TR, [(ab0ab1,0.2), (cd1cd2cd3,0.62), (de1de2,0.7),...,(xy0xy1,0.5)])
	 */	
	Map<Feature, Map<String, Integer>> originalSequenceFeatureInstanceCountPercentageMap;
	
	/*
	 * (TR, [(ab0ab1,2), (cd1cd2cd3,3), (de1de2,1),...,(xy0xy1,2)])
	 */	
	Map<Feature, Map<Set<String>, Integer>> originalAlphabetFeatureCountMap;
	
	/*
	 * (TR, [(ab0ab1,0.2), (cd1cd2cd3,0.62), (de1de2,0.7),...,(xy0xy1,0.5)])
	 */	
	Map<Feature, Map<Set<String>, Integer>> originalAlphabetFeatureInstanceCountPercentageMap;
	
	/* Contains map from feature type (TR,MR...) to a set  of features
	 * These are only base features, meaning features where broken activities would be 
	 * the same as those unique activities used to form the features, e.g. ab0ab1ab2 -> ab0,ab1,ab2, NOT ab0ab0ab1ab2
	 * Example set of features
	 * (TR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 * (MR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 */	
	Map<Feature, Set<String>> baseSequenceFeatureSetMap;
	
	/* Map from feature type (TR,MR,...) to a map which maps encoded activity names to a set of feature
	 * The encoded activity names are like alphabet components used to form every feature. See example.
	 * This is for base features, meaning features where broken activities would be 
	 * the same as those unique activities used to form the features, e.g. ab0ab1ab2 -> ab0,ab1,ab2, NOT ab0ab0ab1ab2
	 * Example of the set
	 * ("ab0", "ab1"), ("ab0ab1", "ab1ab0")
	 * ("cd0", "cd1", "cd2), ("cd0cd1cd2", "cd0cd2cd1", "cd1cd0cd2", "cd1cd2cd0")
	 */	
	Map<Feature, Map<Set<String>, Set<String>>> baseAlphabetFeatureSetMap;
	
	/* This is for base features
	 * (TR, [(ab0ab1,2), (cd1cd2cd3,3), (de1de2,1),...,(xy0xy1,2)])
	 */
	Map<Feature, Map<String, Integer>> baseSequenceFeatureCountMap;
	
	/* This is for base features
	 * (TR, [(ab0ab1,0.2), (cd1cd2cd3,0.62), (de1de2,0.7),...,(xy0xy1,0.5)])
	 */
	Map<Feature, Map<String, Integer>> baseSequenceFeatureInstanceCountPercentageMap;

	/* This is for base features
	 * (TR, [(ab0ab1,2), (cd1cd2cd3,3), (de1de2,1),...,(xy0xy1,2)])
	 */	
	Map<Feature, Map<Set<String>, Integer>> baseAlphabetFeatureCountMap;
	
	/* This is for base features
	 * (TR, [(ab0ab1,0.2), (cd1cd2cd3,0.62), (de1de2,0.7),...,(xy0xy1,0.5)])
	 */	
	Map<Feature, Map<Set<String>, Integer>> baseAlphabetFeatureInstanceCountPercentageMap;
	
	/*
	 * Bruce
	 * encodingLength: e.g. 3
	 * activitycharMap 

	 * 
	 * charActivityMap
	 * 		ab0, "New Claim"
	 * 		cd1, "Contact Customer"
	 * 		xw3, "Generate Payment"
	 *
	 * InstanceProfileList

	 * 
	 * SelectedFeatureSet
	 * 		IE, TR, MR, SMR
	 * 
	 * kGramValue: 3
	 */
	public FeatureExtraction(int encodingLength, Map<String, String> activityCharMap, Map<String, String> charActivityMap, List<InstanceProfile> instanceProfileList, Set<Feature> selectedFeatureSet, int kGramValue){
		this.encodingLength = encodingLength;
		this.activityCharMap = activityCharMap;
		this.charActivityMap = charActivityMap;
		this.selectedFeatureSet = selectedFeatureSet;
		this.kGramValue = kGramValue;
		this.instanceProfileList = instanceProfileList;
		
		computeFeatureSets();
	}

	private void computeFeatureSets(){
		//Logger.printCall("Calling FeatureExtraction->computeFeatureSets()");
		
		Set<Feature> repeatFeatureSet = new HashSet<Feature>();
		Set<Feature> repeatAlphabetFeatureSet = new HashSet<Feature>();
		boolean hasRepeatFeature = false;
		boolean hasRepeatAlphabetFeature = false;
		
		originalSequenceFeatureSetMap = new HashMap<Feature, Set<String>>();
		originalAlphabetFeatureSetMap = new HashMap<Feature, Map<Set<String>,Set<String>>>();
		
		baseSequenceFeatureSetMap = new HashMap<Feature, Set<String>>();
		baseAlphabetFeatureSetMap = new HashMap<Feature, Map<Set<String>,Set<String>>>();
		
		for(Feature feature : selectedFeatureSet){
			switch(feature){
				case IE:	// Bruce: for IE, it sets both originalSequenceFeatureSetMap and originalAlphabetFeatureSetMap
					Set<String> individualEventFeatureSet = getIndividualEventFeatures();
					originalSequenceFeatureSetMap.put(feature, individualEventFeatureSet);
					baseSequenceFeatureSetMap.put(feature, individualEventFeatureSet);
					EquivalenceClass equivalenceClass = new EquivalenceClass();
					originalAlphabetFeatureSetMap.put(feature, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, individualEventFeatureSet));
					baseAlphabetFeatureSetMap.put(feature, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, individualEventFeatureSet));
					break;
				case KGram:
					hasSequenceFeature = true;
					Set<String> kGramSet = getKGrams();
					//System.out.println("kGramSet Size: "+kGramSet.size());
					originalSequenceFeatureSetMap.put(feature, kGramSet);
					baseSequenceFeatureSetMap.put(feature, kGramSet);
					break;
				case TR:
					computeTandemRepeatFeatureSet();
				case MR:
				case SMR:
				case NSMR:
					hasSequenceFeature = true;
					hasRepeatFeature = true;
					repeatFeatureSet.add(feature);
					break;
				case TRA:
					hasAlphabetFeature = true;
					computeTandemRepeatAlphabetFeatureSet();
				case MRA:
				case SMRA:
				case NSMRA:
					hasAlphabetFeature = true;
					repeatAlphabetFeatureSet.add(feature);
					hasRepeatAlphabetFeature = true;
					break;
				default:
					break;
			}
			
			System.out.println("Number of features generated for type " + feature.toString());
			if (originalSequenceFeatureSetMap.size() > 0) System.out.println("originalSequenceFeatureSetMap.size = " + originalSequenceFeatureSetMap.get(feature).size());
			if (originalAlphabetFeatureSetMap.size() > 0) System.out.println("originalAlphabetFeatureSetMap.size = " + originalAlphabetFeatureSetMap.get(feature).size());
			if (baseSequenceFeatureSetMap.size() > 0) System.out.println("baseSequenceFeatureSetMap.size = " + baseSequenceFeatureSetMap.get(feature).size());
			if (baseAlphabetFeatureSetMap.size() > 0) System.out.println("baseAlphabetFeatureSetMap.size = " + baseAlphabetFeatureSetMap.get(feature).size());
		}
		
		if (hasRepeatFeature || hasRepeatAlphabetFeature) {
			StringBuilder combinedStringBuilder = new StringBuilder();
			Set<String> charStreamSet = new HashSet<String>();
 
			modifiedInstanceProfileList = preprocessLogForTandemRepeats();
			
			/*
			 * Concatenate all traces to once single sequence, delimited by traceID
			 */
			for (InstanceProfile instanceProfile : modifiedInstanceProfileList) {
				if (!charStreamSet.contains(instanceProfile.getEncodedTrace())) {
					combinedStringBuilder.append(instanceProfile.getEncodedTrace());
					combinedStringBuilder.append(activityCharMap.get(instanceProfile.getName()));
				}
			}

			if (hasRepeatFeature) {
				computeRepeatfeatureFeatureSetMap(encodingLength, combinedStringBuilder.toString(), repeatFeatureSet);
			} 
			
			/*
			 * When choose best features option is chosen, then we need to
			 * compute both sequence as well as alphabet features;
			 */
			if (hasRepeatAlphabetFeature) {
				computeRepeatAlphabetfeatureFeatureSetMap(encodingLength, combinedStringBuilder.toString(),
						repeatAlphabetFeatureSet);
			}
		}
		
		/*
		 * When choose best features option is chosen, then we need to generate
		 * combination feature sets
		 */
		computeCombinationFeatureSets();
		
		if(selectedFeatureSet.size() > 1){
			computeUnionFeatureSet();
		}
		
		//Logger.printReturn("Returning FeatureExtraction->computeFeatureSets()");
		
	}
	
	/*
	 * Return (ab0, ab1, ab2, ab3, ab4...., xy1)
	 */
	private Set<String> getIndividualEventFeatures(){
		//Logger.printCall("Calling FeatureExtraction->getIndividualEventFeatures()");
		
		Set<String> individualEventFeatureSet = new HashSet<String>();
		String encodedTrace, encodedActivity;
		int encodedTraceLength;
		for(InstanceProfile instanceProfile : instanceProfileList){
			encodedTrace = instanceProfile.getEncodedTrace();
			encodedTraceLength = encodedTrace.length()/encodingLength;
			for(int i = 0; i < encodedTraceLength; i++){
				encodedActivity = encodedTrace.substring(i*encodingLength, (i+1)*encodingLength);
				if(!charActivityMap.get(encodedActivity).contains("Delimiter"))
					individualEventFeatureSet.add(encodedActivity);
			}
		}
		
		//Logger.printReturn("Returning FeatureExtraction->getIndividualEventFeatures() "+individualEventFeatureSet.size());

		return individualEventFeatureSet;
	}
	
	/*
	 * Return (k=3) (ab0ab1ab2, cd0cd1cd2, de0de1de2,....,xy0xy1xy2)
	 */
	private Set<String> getKGrams(){
		//Logger.printCall("Calling FeatureExtraction->getKGrams()");
		
		Set<String> kGramFeatureSet = new HashSet<String>();
		String encodedTrace;
		int encodedTraceLength;
		for(InstanceProfile instanceProfile : instanceProfileList){
			encodedTrace = instanceProfile.getEncodedTrace();
			encodedTraceLength = encodedTrace.length()/encodingLength;
			for(int i = 0; i < encodedTraceLength-kGramValue; i++)
				kGramFeatureSet.add(encodedTrace.substring(i*encodingLength, (i+kGramValue)*encodingLength));
		}
		
		//Logger.printReturn("Returning FeatureExtraction->getKGrams() "+kGramFeatureSet.size());
		
		return kGramFeatureSet;
	}

	/* This function is compute tandem repeat which is feature within one trace only
	 * Return originalSequenceFeatureSetMap, baseSequenceFeatureSetMap
	 * (TR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 * (MR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 */
	private void computeTandemRepeatFeatureSet(){
		//Logger.printCall("Calling FeatureExtraction->computeTandemRepeatFeatureSet()");
		
		UkkonenSuffixTree suffixTree;
		Set<String> tandemRepeatSet = new HashSet<String>();
		Set<String> baseTandemRepeatSet = new HashSet<String>();
		Map<TreeSet<String>, TreeSet<String>> loopAlphabetLoopPatternSetMap;
		Set<String> loopAlphabetPatternSet;
		for(InstanceProfile instanceProfile : instanceProfileList){
//			System.out.println("Trace: "+instanceProfile.getEncodedTrace());
			if(instanceProfile.getEncodedTrace().length() > 2*encodingLength){
				suffixTree = new UkkonenSuffixTree(encodingLength, instanceProfile.getEncodedTrace());
				suffixTree.LZDecomposition();
				loopAlphabetLoopPatternSetMap = suffixTree.getPrimitiveTandemRepeats();
				for(Set<String> trAlphabet : loopAlphabetLoopPatternSetMap.keySet()){
					loopAlphabetPatternSet = loopAlphabetLoopPatternSetMap.get(trAlphabet);
					tandemRepeatSet.addAll(loopAlphabetPatternSet);
					
					for(String tandemRepeat : loopAlphabetPatternSet){
						if(tandemRepeat.length()/encodingLength == trAlphabet.size()){
							baseTandemRepeatSet.add(tandemRepeat);
						}
					}
				}
			}
		}
		
		originalSequenceFeatureSetMap.put(Feature.TR, tandemRepeatSet);
		//System.out.println(tandemRepeatSet.toString());
		baseSequenceFeatureSetMap.put(Feature.TR, baseTandemRepeatSet);
		//System.out.println(baseTandemRepeatSet.toString());
		
		//Logger.println("No. Tandem Repeat Sequence Features: "+tandemRepeatSet.size());
		//Logger.println("No. Tandem Repeat (Base) Sequence Features: "+baseTandemRepeatSet.size());
		
		//Logger.printReturn("Returning FeatureExtraction->computeTandemRepeatFeatureSet()");
	}
	
	/*
	 * Return originalAlphabetFeatureSetMap, baseAlphabetFeatureSetMap
	 * (TR, ([("ab0", "ab1"), ("ab0ab1", "ab1ab0")], [("cd0", "cd1", "cd2), ("cd0cd2cd1", "cd1cd0cd2", "cd1cd2cd0")])
	 * (MR, ([("ab0", "ab1"), ("ab0ab1", "ab1ab0")], [("cd0", "cd1", "cd2), ("cd0cd2cd1", "cd1cd0cd2", "cd1cd2cd0")])
	 */	
	private void computeTandemRepeatAlphabetFeatureSet(){
		//Logger.printCall("Calling FeatureExtraction->computeTandemRepeatAlphabetFeatureSet()");
		
		UkkonenSuffixTree suffixTree;
		Set<String> tandemRepeatSet = new HashSet<String>();
		Set<String> baseTandemRepeatSet = new HashSet<String>();
		
		Map<TreeSet<String>, TreeSet<String>> loopAlphabetLoopPatternSetMap;
		Set<String> loopAlphabetPatternSet;
		
		for(InstanceProfile instanceProfile : instanceProfileList){
			if(instanceProfile.getEncodedTrace().length() > 2*encodingLength){
				suffixTree = new UkkonenSuffixTree(encodingLength, instanceProfile.getEncodedTrace());
				suffixTree.LZDecomposition();
				loopAlphabetLoopPatternSetMap = suffixTree.getPrimitiveTandemRepeats();
				for(Set<String> trAlphabet : loopAlphabetLoopPatternSetMap.keySet()){
					loopAlphabetPatternSet = loopAlphabetLoopPatternSetMap.get(trAlphabet);
					tandemRepeatSet.addAll(loopAlphabetPatternSet);
					
					for(String tandemRepeat : loopAlphabetPatternSet){
						if(tandemRepeat.length()/encodingLength == trAlphabet.size()){
							baseTandemRepeatSet.add(tandemRepeat);
						}
					}
				}
			}
		}
		
		EquivalenceClass equivalenceClass = new EquivalenceClass();
		originalAlphabetFeatureSetMap.put(Feature.TRA, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, tandemRepeatSet));
		baseAlphabetFeatureSetMap.put(Feature.TRA, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, baseTandemRepeatSet));
		
		//Logger.println("No. Tandem Repeat Alphabet Features: "+originalAlphabetFeatureSetMap.get(Feature.TRA).size());
		//Logger.println("No. Tandem Repeat (Base) Alphabet Features: "+baseAlphabetFeatureSetMap.get(Feature.TRA).size());

		//Logger.printReturn("Returning FeatureExtraction->computeTandemRepeatAlphabetFeatureSet()");
	}
	
	private List<InstanceProfile> preprocessLogForTandemRepeats(){
		//Logger.printCall("Calling FeatureExtraction->preprocessLogForTandemRepeats()");
		
		List<InstanceProfile> processedInstanceProfileList = new ArrayList<InstanceProfile>();
		UkkonenSuffixTree suffixTree;
		Set<String> tandemRepeatSet = new HashSet<String>();
		Map<TreeSet<String>, TreeSet<String>> loopAlphabetLoopPatternSetMap;
		for(InstanceProfile instanceProfile : instanceProfileList){
			if(instanceProfile.getEncodedTrace().length() > 2*encodingLength){
				suffixTree = new UkkonenSuffixTree(encodingLength, instanceProfile.getEncodedTrace());
				suffixTree.LZDecomposition();
				loopAlphabetLoopPatternSetMap = suffixTree.getPrimitiveTandemRepeats();
				for(Set<String> trAlphabet : loopAlphabetLoopPatternSetMap.keySet())
					tandemRepeatSet.addAll(loopAlphabetLoopPatternSetMap.get(trAlphabet));
			}
		}
		
		EquivalenceClass equivalenceClass = new EquivalenceClass();
		Map<String, Set<String>> startSymbolEquivalenceClassMap = equivalenceClass.getStartSymbolEquivalenceClassMap(encodingLength, tandemRepeatSet, false);
		
		String encodedInstance, currentSymbol;
		int encodedInstanceLength;
		Set<String> startSymbolEquivalenceClassSet;
		
		Pattern pattern;
		Matcher matcher;
		StringBuilder modifiedCharStream = new StringBuilder();
		InstanceProfile modifiedInstanceProfile;
		boolean hasPattern;
		for(InstanceProfile instanceProfile : instanceProfileList){
			encodedInstance = instanceProfile.getEncodedTrace();
			encodedInstanceLength = encodedInstance.length()/encodingLength;
			modifiedCharStream.setLength(0);
			//System.out.println("Original Trace: "+encodedInstance+" @ "+encodedInstanceLength);
			for(int i = 0; i < encodedInstanceLength; i++){
				currentSymbol = encodedInstance.substring(i*encodingLength, (i+1)*encodingLength);
				if(startSymbolEquivalenceClassMap.containsKey(currentSymbol)){
					startSymbolEquivalenceClassSet = startSymbolEquivalenceClassMap.get(currentSymbol);
					hasPattern = false;
//					System.out.println(currentSymbol+"@"+startSymbolEquivalenceClassSet);
					for(String tandemRepeat : startSymbolEquivalenceClassSet){
						pattern = Pattern.compile("("+tandemRepeat+"){1,}");
						matcher = pattern.matcher(encodedInstance);
						if(matcher.find(i*encodingLength) && matcher.start() == i*encodingLength){
							modifiedCharStream.append(tandemRepeat);
							i = matcher.end()/encodingLength-1;
							hasPattern = true;
							break;
						}
					}
					if(!hasPattern){
						modifiedCharStream.append(currentSymbol);	
					}
				}else{
					modifiedCharStream.append(currentSymbol);
				}
			}
			
//			System.out.println("Modified Trace: "+modifiedCharStream);
			modifiedInstanceProfile = new InstanceProfile(instanceProfile.getName(), modifiedCharStream.toString(),instanceProfile.getLabel());
			processedInstanceProfileList.add(modifiedInstanceProfile);
		}
		
		//Logger.printReturn("Returning FeatureExtraction->preprocessLogForTandemRepeats()");
		return processedInstanceProfileList;
	}
	
	/*
	 * Compute features for MR,SMR,NSMR.
	 * This is on the whole log (all traces concatenated)
	 * encodingLength: length of encoded activity
	 * charStream: all traces concatenated into one string, separated with traceID
	 * repeatFeatureSet: either TR,MR,SMR,NSMR
	 * Output: 
	 * originalSequenceFeatureSetMap: set of features for each feature type
	 * baseSequenceFeatureSetMap: set of features for each feature type, these features contain no repetitive alphabet 
	 * 		components
	 */
	private void computeRepeatfeatureFeatureSetMap(int encodingLength, String charStream, Set<Feature> repeatFeatureSet){
		//Logger.printCall("Calling FeatureExtraction->computeRepeatfeatureFeatureSetMap");
		Logger.println(repeatFeatureSet);
	
		EquivalenceClass equivalenceClass = new EquivalenceClass();
		Map<Set<String>, Set<String>> alphabetPatternEquivalenceClassMap;
		Set<String> alphabetEquivalenceClassPatternSet;
		
		UkkonenSuffixTree suffixTree = new UkkonenSuffixTree(encodingLength, charStream);
		suffixTree.findLeftDiverseNodes();
		for(Feature feature : repeatFeatureSet){
			switch(feature){
			case MR:
				Set<String> maximalRepeatSet = suffixTree.getMaximalRepeats();
				
				/*
				 * Select base features based on alphabet components
				 * A set of component alphabet is created out of the feature component (activity)
				 * If a feature contains no repeated alphabet components, then it is base feature
				 * Example: alphabet component {a,b,c}.
				 * Base features: abc, bca, cab
				 * Non-base features: aabc, bcca, abccc, abbcc
				 */
				Set<String> baseMaximalRepeatSet = new HashSet<String>();
				alphabetPatternEquivalenceClassMap = equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, maximalRepeatSet);
				for(Set<String> alphabet : alphabetPatternEquivalenceClassMap.keySet()){
					alphabetEquivalenceClassPatternSet = alphabetPatternEquivalenceClassMap.get(alphabet);
					for(String pattern : alphabetEquivalenceClassPatternSet){
						if(pattern.length()/encodingLength == alphabet.size()){
							baseMaximalRepeatSet.add(pattern);
						}
					}
				}
				
				originalSequenceFeatureSetMap.put(feature, maximalRepeatSet);
				baseSequenceFeatureSetMap.put(feature, baseMaximalRepeatSet);
				
//				System.out.println("Sequence features-----");
//				for (String patternString : maximalRepeatSet) {
//					System.out.println(patternString);
//				}

				break;
			case SMR:
				Set<String> superMaximalRepeatSet = suffixTree.getSuperMaximalRepeats();
				Set<String> baseSuperMaximalRepeatSet = new HashSet<String>();
				alphabetPatternEquivalenceClassMap = equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, superMaximalRepeatSet);
				for(Set<String> alphabet : alphabetPatternEquivalenceClassMap.keySet()){
					alphabetEquivalenceClassPatternSet = alphabetPatternEquivalenceClassMap.get(alphabet);
					for(String pattern : alphabetEquivalenceClassPatternSet){
						if(pattern.length()/encodingLength == alphabet.size()){
							baseSuperMaximalRepeatSet.add(pattern);
						}
					}
				}
				originalSequenceFeatureSetMap.put(feature, superMaximalRepeatSet);
				baseSequenceFeatureSetMap.put(feature, baseSuperMaximalRepeatSet);
			
				break;
			case NSMR:
				Set<String> nearSuperMaximalRepeatSet = suffixTree.getSuperMaximalRepeats();
				Set<String> baseNearSuperMaximalRepeatSet = new HashSet<String>();
				alphabetPatternEquivalenceClassMap = equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, nearSuperMaximalRepeatSet);
				for(Set<String> alphabet : alphabetPatternEquivalenceClassMap.keySet()){
					alphabetEquivalenceClassPatternSet = alphabetPatternEquivalenceClassMap.get(alphabet);
					for(String pattern : alphabetEquivalenceClassPatternSet){
						if(pattern.length()/encodingLength == alphabet.size()){
							baseNearSuperMaximalRepeatSet.add(pattern);
						}
					}
				}
				originalSequenceFeatureSetMap.put(feature, nearSuperMaximalRepeatSet);
				baseSequenceFeatureSetMap.put(feature, baseNearSuperMaximalRepeatSet);
				
				break;
			}
		}
		
		//Logger.printReturn("Returning FeatureExtraction->computeRepeatfeatureFeatureSetMap");
	}
	
	/*
	 * Similar to computeRepeatfeatureFeatureSetMap
	 * Return: originalAlphabetFeatureSetMap, baseAlphabetFeatureSetMap
	 * For each feature type TR,MR...return the following map format
	 * Map<Set<String>, Set<String>>
	 * ("ab0", "ab1"), ("ab0ab1", "ab1ab0")
	 * ("cd0", "cd1", "cd2), ("cd0cd1cd2", "cd0cd2cd1", "cd1cd0cd2", "cd1cd2cd0")
	 * Key: set of alphabet 
	 * Value: set of equivalence classes of the key
	 */
	private void computeRepeatAlphabetfeatureFeatureSetMap(int encodingLength, String charStream, Set<Feature> repeatfeatureSet){
		//Logger.printCall("Calling FeatureExtraction->computeRepeatAlphabetfeatureFeatureSetMap");
		
		EquivalenceClass equivalenceClass = new EquivalenceClass();
		Map<Set<String>, Set<String>> alphabetPatternEquivalenceClassMap;
		Set<String> alphabetEquivalenceClassPatternSet;
		
		UkkonenSuffixTree suffixTree = new UkkonenSuffixTree(encodingLength, charStream);
		suffixTree.findLeftDiverseNodes();
		
		for(Feature feature : repeatfeatureSet){
			switch(feature){
			case MRA:
				Set<String> maximalRepeatSet = suffixTree.getMaximalRepeats();
				Set<String> baseMaximalRepeatSet = new HashSet<String>();
				alphabetPatternEquivalenceClassMap = equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, maximalRepeatSet);
				for(Set<String> alphabet : alphabetPatternEquivalenceClassMap.keySet()){
					alphabetEquivalenceClassPatternSet = alphabetPatternEquivalenceClassMap.get(alphabet);
					for(String pattern : alphabetEquivalenceClassPatternSet){
						if(pattern.length()/encodingLength == alphabet.size()){
							baseMaximalRepeatSet.add(pattern);
						}
					}
				}
				
				originalAlphabetFeatureSetMap.put(feature, alphabetPatternEquivalenceClassMap);
				baseAlphabetFeatureSetMap.put(feature, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, baseMaximalRepeatSet));
			
				break;
			case SMRA:
				Set<String> superMaximalRepeatSet = suffixTree.getSuperMaximalRepeats();
				Set<String> baseSuperMaximalRepeatSet = new HashSet<String>();
				alphabetPatternEquivalenceClassMap = equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, superMaximalRepeatSet);
				for(Set<String> alphabet : alphabetPatternEquivalenceClassMap.keySet()){
					alphabetEquivalenceClassPatternSet = alphabetPatternEquivalenceClassMap.get(alphabet);
					for(String pattern : alphabetEquivalenceClassPatternSet){
						if(pattern.length()/encodingLength == alphabet.size()){
							baseSuperMaximalRepeatSet.add(pattern);
						}
					}
				}
				
				originalAlphabetFeatureSetMap.put(feature, alphabetPatternEquivalenceClassMap);
				baseAlphabetFeatureSetMap.put(feature, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, baseSuperMaximalRepeatSet));
				
				break;
			case NSMRA:
				Set<String> nearSuperMaximalRepeatSet = suffixTree.getSuperMaximalRepeats();
				Set<String> baseNearSuperMaximalRepeatSet = new HashSet<String>();
				alphabetPatternEquivalenceClassMap = equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, nearSuperMaximalRepeatSet);
				for(Set<String> alphabet : alphabetPatternEquivalenceClassMap.keySet()){
					alphabetEquivalenceClassPatternSet = alphabetPatternEquivalenceClassMap.get(alphabet);
					for(String pattern : alphabetEquivalenceClassPatternSet){
						if(pattern.length()/encodingLength == alphabet.size()){
							baseNearSuperMaximalRepeatSet.add(pattern);
						}
					}
				}
				originalAlphabetFeatureSetMap.put(feature, alphabetPatternEquivalenceClassMap);
				baseAlphabetFeatureSetMap.put(feature, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, baseNearSuperMaximalRepeatSet));
				
				break;
			}
		}
		
		//Logger.printReturn("Returning FeatureExtraction->computeRepeatfeatureFeatureSetMap");
	}
	
		
	private void computeCombinationFeatureSets(){
		//Logger.printCall("Calling FeatureExtraction->computeCombinationFeatureSets()");
		/*
		 * First do it for pairs
		 */
		for(Feature feature : selectedFeatureSet){
			switch(feature){
				case IE_MR:
					generateSequenceCombinationFeatureSet(Feature.IE, Feature.MR, feature);
					break;
				case IE_TR:
					generateSequenceCombinationFeatureSet(Feature.IE, Feature.TR, feature);
					break;
				case TR_MR:
					generateSequenceCombinationFeatureSet(Feature.TR, Feature.MR, feature);
					break;
				case IE_MRA:
					generateAlphabetCombinationFeatureSet(Feature.IE, Feature.MRA, feature);
					break;
				case IE_TRA:
					generateAlphabetCombinationFeatureSet(Feature.IE, Feature.TRA, feature);
					break;
				case TRA_MRA:
					generateAlphabetCombinationFeatureSet(Feature.TRA, Feature.MRA, feature);
					break;
				default:
					break;
			}
		}
		
		/*
		 * Do it for combination of three feature types
		 */
		for(Feature feature : selectedFeatureSet){
			switch(feature){
				case IE_TR_MR:
					generateSequenceCombinationFeatureSet(Feature.IE_TR, Feature.MR, feature);
					break;
				case IE_TRA_MRA:
					generateAlphabetCombinationFeatureSet(Feature.IE_TRA, Feature.MRA, feature);
					break;
				default:
					break;
			}
		}
		
		//Logger.printReturn("Returning FeatureExtraction->computeCombinationFeatureSets()");
		
	}
	
	private void generateSequenceCombinationFeatureSet(Feature feature1, Feature feature2, Feature combinationFeature){
		//Logger.printCall("Calling FeatureExtraction->generateSequenceCombinationFeatureSet()->"+feature1+","+feature2);
		
		Set<String> combinationSequenceFeatureSet = new HashSet<String>();
		combinationSequenceFeatureSet.addAll(originalSequenceFeatureSetMap.get(feature1));
		combinationSequenceFeatureSet.addAll(originalSequenceFeatureSetMap.get(feature2));
		originalSequenceFeatureSetMap.put(combinationFeature, combinationSequenceFeatureSet);
		
		Set<String> baseCombinationSequenceFeatureSet = new HashSet<String>();
		baseCombinationSequenceFeatureSet.addAll(baseSequenceFeatureSetMap.get(feature1));
		baseCombinationSequenceFeatureSet.addAll(baseSequenceFeatureSetMap.get(feature2));
		baseSequenceFeatureSetMap.put(combinationFeature, baseCombinationSequenceFeatureSet);
		
		//Logger.printReturn("Returning FeatureExtraction->generateSequenceCombinationFeatureSet()->"+feature1+","+feature2);
	}
	
	private void generateAlphabetCombinationFeatureSet(Feature feature1, Feature feature2, Feature combinationFeature){
		//Logger.printCall("Calling FeatureExtraction->generateAlphabetCombinationFeatureSet()->"+feature1+","+feature2);
		
		Map<Set<String>, Set<String>> alphabetFeatureSetMap;
		Set<String> alphabetFeatureSet = new HashSet<String>();
		
		alphabetFeatureSetMap = originalAlphabetFeatureSetMap.get(feature1);
		for(Set<String> alphabet : alphabetFeatureSetMap.keySet())
			alphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));
		
		alphabetFeatureSetMap = originalAlphabetFeatureSetMap.get(feature2);
		for(Set<String> alphabet : alphabetFeatureSetMap.keySet())
			alphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));
		
		EquivalenceClass equivalenceClass = new EquivalenceClass();
		originalAlphabetFeatureSetMap.put(combinationFeature, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, alphabetFeatureSet));
		
		alphabetFeatureSet.clear();
		alphabetFeatureSetMap = baseAlphabetFeatureSetMap.get(feature1);
		for(Set<String> alphabet : alphabetFeatureSetMap.keySet())
			alphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));
		
		alphabetFeatureSetMap = baseAlphabetFeatureSetMap.get(feature2);
		for(Set<String> alphabet : alphabetFeatureSetMap.keySet())
			alphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));
		
		baseAlphabetFeatureSetMap.put(combinationFeature, equivalenceClass.getAlphabetEquivalenceClassMap(encodingLength, alphabetFeatureSet));
		
		//Logger.printReturn("Returning FeatureExtraction->generateAlphabetCombinationFeatureSet()->"+feature1+","+feature2);
	}
	
	private void computeUnionFeatureSet(){
		//Logger.printCall("Calling FeatureExtraction->computeUnionFeatureSet()");
		
		if(hasSequenceFeature){
			Set<String> mixFeatureSet = new HashSet<String>();
			Set<String> baseMixFeatureSet = new HashSet<String>();
			for(Feature feature : selectedFeatureSet){
				//System.out.println("feature: "+feature);
				if(originalSequenceFeatureSetMap.containsKey(feature)){
					mixFeatureSet.addAll(originalSequenceFeatureSetMap.get(feature));
					baseMixFeatureSet.addAll(baseSequenceFeatureSetMap.get(feature));
				}
			}
			if(mixFeatureSet.size() > 0){
				originalSequenceFeatureSetMap.put(Feature.MIX, mixFeatureSet);
				baseSequenceFeatureSetMap.put(Feature.MIX, baseMixFeatureSet);
			}
		}
		
		if(hasAlphabetFeature){
			Map<Set<String>, Set<String>> mixAlphabetFeatureSetMap = new HashMap<Set<String>, Set<String>>();
			Map<Set<String>, Set<String>> baseMixAlphabetFeatureSetMap = new HashMap<Set<String>, Set<String>>();

			Map<Set<String>, Set<String>> alphabetFeatureSetMap;
			Set<String> alphabetFeatureSet;
			for(Feature feature : selectedFeatureSet){
				if(originalAlphabetFeatureSetMap.containsKey(feature)){
					alphabetFeatureSetMap = originalAlphabetFeatureSetMap.get(feature);
					for(Set<String> alphabet : alphabetFeatureSetMap.keySet()){
						if(mixAlphabetFeatureSetMap.containsKey(alphabet)){
							alphabetFeatureSet = mixAlphabetFeatureSetMap.get(alphabet);
						}else{
							alphabetFeatureSet = new HashSet<String>();
						}
						alphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));
						mixAlphabetFeatureSetMap.put(alphabet, alphabetFeatureSet);
					}
					
					//Do it for Base Alphabet Now
					alphabetFeatureSetMap = baseAlphabetFeatureSetMap.get(feature);
					for(Set<String> alphabet : alphabetFeatureSetMap.keySet()){
						if(baseMixAlphabetFeatureSetMap.containsKey(alphabet)){
							alphabetFeatureSet = baseMixAlphabetFeatureSetMap.get(alphabet);
						}else{
							alphabetFeatureSet = new HashSet<String>();
						}
						alphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));
						baseMixAlphabetFeatureSetMap.put(alphabet, alphabetFeatureSet);
					}
				}
			}
			if(mixAlphabetFeatureSetMap.size() > 0){
				originalAlphabetFeatureSetMap.put(Feature.MIXA, mixAlphabetFeatureSetMap);
				baseAlphabetFeatureSetMap.put(Feature.MIXA, baseMixAlphabetFeatureSetMap);
			}
		}
		
		//Logger.printReturn("Returning FeatureExtraction->computeUnionFeatureSet()");
	}
	
	public void computeNonOverlapFeatureMetrics(){
		computeNonOverlapSequenceFeatureMetrics();
		computeNonOverlapAlphabetFeatureMetrics();
	}
	
	/*
	 * Return baseSequenceFeatureCountMap, baseSequenceFeatureInstanceCountPercentageMap
	 * (TR, [(ab0ab1,2), (cd1cd2cd3,3), (de1de2,1),...,(xy0xy1,2)])
	 * (TR, [(ab0ab1,0.2), (cd1cd2cd3,0.62), (de1de2,0.7),...,(xy0xy1,0.5)])
	 */
	private void computeNonOverlapSequenceFeatureMetrics(){
		//Logger.println("Computing nonOverlap Sequence Feature Metrics for Original Features");
		Map<Feature, Map<String,Integer>>[] tempMap = computeNonOverlapSequenceFeatureMetrics(originalSequenceFeatureSetMap); 
		originalSequenceFeatureCountMap = new HashMap<Feature, Map<String, Integer>>(tempMap[0]);
		originalSequenceFeatureInstanceCountPercentageMap = new HashMap<Feature, Map<String, Integer>>(tempMap[1]);
		
		//Logger.println("Computing nonOverlap Sequence Feature Metrics for Base Features");
		tempMap = computeNonOverlapSequenceFeatureMetrics(baseSequenceFeatureSetMap);
		baseSequenceFeatureCountMap = new HashMap<Feature, Map<String, Integer>>(tempMap[0]);
		baseSequenceFeatureInstanceCountPercentageMap = new HashMap<Feature, Map<String, Integer>>(tempMap[1]);
	}
	
	@SuppressWarnings("unchecked")
	private Map<Feature, Map<String,Integer>>[] computeNonOverlapSequenceFeatureMetrics(Map<Feature, Set<String>> featureSequenceFeatureMap){
		//Logger.printCall("Calling FeatureExtraction->computeNonOverlapSequenceFeatureMetrics()->"+featureSequenceFeatureMap.keySet());
		
		Map<Feature, Map<String,Integer>> featureSequenceFeatureNOCMap = new HashMap<Feature, Map<String,Integer>>();
		Map<Feature, Map<String,Integer>> featureSequenceInstanceCountMap = new HashMap<Feature, Map<String,Integer>>();
		Map<String, Integer> sequenceFeatureNOCMap;
		Map<String, Integer> sequenceFeatureInstanceCountMap;
		Set<String> sequenceFeatureSet;
		
		List<InstanceProfile> instanceProfileList = new ArrayList<InstanceProfile>();
		String encodedTrace, currentSymbol;
		int encodedTraceLength, count;
		EquivalenceClass equivalenceClass = new EquivalenceClass();
		Map<String, Set<String>> startSymbolEquivalenceClassMap;
		Set<String> startSymbolEquivalenceClassSet;
		Set<String> instanceSequenceFeatureSet = new HashSet<String>();
		
		for(Feature feature : featureSequenceFeatureMap.keySet()){
			//Logger.println(feature);
			instanceProfileList.clear();
			switch (feature) {
				case IE:
				case KGram:
				case TR:
				case IE_TR:
				case IE_TR_MR:
				case TR_MR:
				case TR_SMR:
				case TR_NSMR:
				case IE_MR:
				case IE_SMR:
				case IE_NSMR:
					instanceProfileList.addAll(this.instanceProfileList);
					break;
				case MR:
				case SMR:
				case NSMR:
				case MIX:
					instanceProfileList.addAll(this.modifiedInstanceProfileList);
					break;
				default:
					instanceProfileList.addAll(this.instanceProfileList);
					break;
			}
			sequenceFeatureSet = featureSequenceFeatureMap.get(feature);
			startSymbolEquivalenceClassMap = equivalenceClass.getStartSymbolEquivalenceClassMap(encodingLength, sequenceFeatureSet);
			sequenceFeatureNOCMap = new HashMap<String, Integer>();
			sequenceFeatureInstanceCountMap = new HashMap<String, Integer>();
			instanceSequenceFeatureSet.clear();
			for(InstanceProfile instanceProfile : instanceProfileList){
				encodedTrace = instanceProfile.getEncodedTrace();
//				System.out.println("H: "+encodedTrace);
				encodedTraceLength = encodedTrace.length()/encodingLength;
				for(int i = 0; i < encodedTraceLength; i++){
					currentSymbol = encodedTrace.substring(i*encodingLength, (i+1)*encodingLength);
					if(startSymbolEquivalenceClassMap.containsKey(currentSymbol)){
						startSymbolEquivalenceClassSet = startSymbolEquivalenceClassMap.get(currentSymbol);
//						System.out.println(currentSymbol+"@"+startSymbolEquivalenceClassSet);
						for(String pattern : startSymbolEquivalenceClassSet){
							if(encodedTrace.indexOf(pattern, i*encodingLength) == i*encodingLength){
								count = 1;
								if(sequenceFeatureNOCMap.containsKey(pattern)){
									count += sequenceFeatureNOCMap.get(pattern);
								}
								sequenceFeatureNOCMap.put(pattern, count);
								instanceSequenceFeatureSet.add(pattern);
							}
						}
					}
				}
				
				for(String pattern : instanceSequenceFeatureSet){
					count = 1;
					if(sequenceFeatureInstanceCountMap.containsKey(pattern))
						count += sequenceFeatureInstanceCountMap.get(pattern);
					sequenceFeatureInstanceCountMap.put(pattern, count);
				}
			}
			
			int noInstances = instanceProfileList.size();
			for(String pattern : sequenceFeatureInstanceCountMap.keySet()){
				count = (int)(sequenceFeatureInstanceCountMap.get(pattern)*100.0/noInstances);
				sequenceFeatureInstanceCountMap.put(pattern, count);
			}
			
			featureSequenceFeatureNOCMap.put(feature, sequenceFeatureNOCMap);
			featureSequenceInstanceCountMap.put(feature, sequenceFeatureInstanceCountMap);
		}
		
		//Logger.printReturn("Returning FeatureExtraction->computeNonOverlapSequenceFeatureMetrics()->"+featureSequenceFeatureMap.keySet());
		return new Map[]{featureSequenceFeatureNOCMap, featureSequenceInstanceCountMap};
	}
	
	/*
	 * encodingLength
	 * encodedTrace: ab0ab1ab2cd0cd1ef0ef1ef2ef3...
	 * featureSet: (ab0ab1ab2, ab0ab3, cd0cd1, cd0cd2, de0de1, ef0ef1ef2ef3...)
	 * Return a map of those features in the featureSet which are found in the encoded trace
	 * Key: a feature (as pattern) in the featureSet and found in the encoded trace
	 * Value: frequency count of feature in the encoded trace (count all repeats)
	 * Only contains those features with count > 0.
	 */
	public Map<String, Integer> computeNonOverlapSequenceFeatureCountMap(int encodingLength, String encodedTrace, Set<String> featureSet){
//		Logger.printCall("Calling FeatureExtraction->computeNonOverlapSequenceFeatureCountMap()-> feature set size: "+featureSet.size());
		
		/*
		 * Contains those features in the featureSet found in the encoded trace
		 * Those feature in the featureSet but not found in the encoded trace will not be put here
		 * Key: a feature (as pattern) in the featureSet and found in the encoded trace
		 * Value: frequency count of feature in the encoded trace
		 * ("ab0ab1", 2)
		 * ("cd0cd1cd2", 3)
		 * ("de0de1", 4)
		 */
		Map<String, Integer> encodedTraceSequenceFeatureCountMap = new HashMap<String, Integer>();
		
		EquivalenceClass equivalenceClass = new EquivalenceClass();
		
		/* Get a structure below from the feature set, 
		 * The key is the first activity in the feature
		 * The value is a set of features which commonly have the key as prefix
		 * The set is sorted from the longest to the shortest feature
		 * ab0, {ab0ab1ab2, ab0ab3}
		 * cd0, {cd0cd1, cd0cd2}
		 * de0, {de0de1, de0de2de3}
		 * ef0, {ef0ef1ef2ef3, ef0ef4, ef0ef5}
		 */
		Map<String, Set<String>> startSymbolEquivalenceClassMap = equivalenceClass.getStartSymbolEquivalenceClassMap(encodingLength, featureSet);
		
		Set<String> startSymbolEquivalenceClassSet;
		
		int encodedTraceLength = encodedTrace.length()/encodingLength;
		
		String currentSymbol;
		int count;
		/*
		 * Scan every activity in the trace
		 */
		for(int i = 0; i < encodedTraceLength; i++){
			currentSymbol = encodedTrace.substring(i*encodingLength, (i+1)*encodingLength);
			
			//Consider the activity if it is found as prefix of any feature
			if(startSymbolEquivalenceClassMap.containsKey(currentSymbol)){
				startSymbolEquivalenceClassSet = startSymbolEquivalenceClassMap.get(currentSymbol);
//				System.out.println(currentSymbol+"@"+startSymbolEquivalenceClassSet);
				
				//Fore each feature, search for its occurrence in the trace
				for(String pattern : startSymbolEquivalenceClassSet){
					if(encodedTrace.indexOf(pattern, i*encodingLength) == i*encodingLength){
						count = 1;
						if(encodedTraceSequenceFeatureCountMap.containsKey(pattern)){
							count += encodedTraceSequenceFeatureCountMap.get(pattern);
						}
						encodedTraceSequenceFeatureCountMap.put(pattern, count);
					}
				}
			}
		}
		
//		Logger.printReturn("Returning FeatureExtraction->computeNonOverlapSequenceFeatureCountMap()-> feature set size: "+featureSet.size());
		return encodedTraceSequenceFeatureCountMap;
	}

	/*
	 * Similar to computeNonOverlapSequenceFeatureCountMap, but for alphabet feature
	 * encodingLength: length of encoded activity
	 * encodedTrace: the current trace
	 * alphabetFeatureSetMap: set of alphabet feature, every line below is called alphabet feature
	 * 		("ab0", "ab1"), ("ab0ab1", "ab1ab0", "ab0ab0ab1", "ab0ab1ab0ab1")
	 * 		("cd0", "cd1", "cd2), ("cd0cd1cd2", "cd0cd2cd1", "cd1cd0cd0cd0cd2")
	 * in which: 	("ab0", "ab1") is called alphabet, 
	 * 				("ab0ab1", "ab1ab0", "ab0ab0ab1", "ab0ab1ab0ab1") is called equivalence classes of the alphabet
	 * Return: map of alphabet pattern and the its count in the encoded trace 
	 * Key: the alphabet
	 * Value: count of the alphabet in the trace
	 * The count is the total no. of occurrence of its equivalence classes in the trace
	 * Those alphabet without any occurrence is not stored in the returned map
	 */
	public Map<Set<String>, Integer> computeNonOverlapAlphabetFeatureCountMap(int encodingLength, String encodedTrace, Map<Set<String>, Set<String>> alphabetFeatureSetMap){
//		Logger.printCall("Calling FeatureExtraction->computeNonOverlapAlphabetFeatureCountMap()-> feature set size: "+alphabetFeatureSetMap.size());
		
		Map<Set<String>, Integer> encodedTraceAlphabetCountMap = new HashMap<Set<String>, Integer>();
		
		TreeSet<String> alphabetFeatureSet;
		Set<String> allAlphabetFeatureSet = new HashSet<String>();
		
		for (Set<String> alphabet : alphabetFeatureSetMap.keySet())
			allAlphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));

		boolean encodedTraceHasPattern;
		int encodedTraceLength = encodedTrace.length()/encodingLength;
		Pattern pattern;
		Matcher matcher;
		int maxCount, repeatLength, patternCount, noMatches;
		String maxCountPattern;
		for (Set<String> alphabet : alphabetFeatureSetMap.keySet()) {
			alphabetFeatureSet = new TreeSet<String>();
			alphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));

			encodedTraceHasPattern = false;
			for (String repeatPattern : alphabetFeatureSet) {
				if (encodedTrace.contains(repeatPattern)) {
					encodedTraceHasPattern = true;
					break;
				}
			}
			if (encodedTraceHasPattern) {
				for (int i = 0; i < encodedTraceLength; i++) {
					maxCount = 0;
					maxCountPattern = "";
					for (String repeatPattern : alphabetFeatureSet) {
						// First check if this repeat pattern exists
						// starting at this index; only if it exists then
						// use the pattern matcher
						if (encodedTrace.indexOf(repeatPattern, i
								* encodingLength) == i * encodingLength) {
							// w.start();
							pattern = Pattern.compile("(" + repeatPattern
									+ "){1,}");
							matcher = pattern.matcher(encodedTrace);
							if (matcher.find(i * encodingLength)
									&& matcher.start() == i
											* encodingLength) {
								repeatLength = repeatPattern.length()
										/ encodingLength;
								noMatches = (matcher.end() - matcher
										.start())
										/ (repeatLength * encodingLength);
								if (noMatches > maxCount) {
									maxCount = noMatches;
									maxCountPattern = repeatPattern;
								}
							}
							// System.out.println("Took "+w.msecs()+" msecs for pattern matching");
						}
					}
					if (maxCount > 0) {
						// No need to actually compute the counts again as
						// we have already stored the maxCount and
						// maxCountPattern information
						repeatLength = maxCountPattern.length()
								/ encodingLength;
						i += repeatLength * maxCount - 1;
						patternCount = 0;
						if (encodedTraceAlphabetCountMap.containsKey(alphabet)) {
							patternCount = encodedTraceAlphabetCountMap.get(alphabet);
						}

						encodedTraceAlphabetCountMap.put((TreeSet<String>) alphabet,
								maxCount + patternCount);
					}
				}
			}
		}
		
//		Logger.printReturn("Returning FeatureExtraction->computeNonOverlapAlphabetFeatureCountMap()-> feature set size: "+alphabetFeatureSetMap.size());
		return encodedTraceAlphabetCountMap;
	}
	
	private void computeNonOverlapAlphabetFeatureMetrics(){
		Map<Feature, Map<Set<String>,Integer>>[] tempMap = computeNonOverlapAlphabetFeatureMetrics(originalAlphabetFeatureSetMap);
		originalAlphabetFeatureCountMap = new HashMap<Feature, Map<Set<String>,Integer>>(tempMap[0]);
		originalAlphabetFeatureInstanceCountPercentageMap = new HashMap<Feature, Map<Set<String>, Integer>>(tempMap[1]);
	
		tempMap = computeNonOverlapAlphabetFeatureMetrics(baseAlphabetFeatureSetMap);
		baseAlphabetFeatureCountMap = new HashMap<Feature, Map<Set<String>, Integer>>(tempMap[0]);
		baseAlphabetFeatureInstanceCountPercentageMap = new HashMap<Feature, Map<Set<String>, Integer>>(tempMap[1]);
	}
	
	@SuppressWarnings("unchecked")
	private Map<Feature, Map<Set<String>,Integer>>[] computeNonOverlapAlphabetFeatureMetrics(Map<Feature, Map<Set<String>, Set<String>>> featureAlphabetFeatureSetMap){
		//Logger.printCall("Calling FeatureExtraction->computeNonOverlapAlphabetFeatureMetrics()-> "+featureAlphabetFeatureSetMap.size());
		
		Map<Feature, Map<Set<String>,Integer>> featureAlphabetFeatureNOCMap = new HashMap<Feature, Map<Set<String>,Integer>>();
		Map<Feature, Map<Set<String>,Integer>> featureAlphabetInstanceCountMap = new HashMap<Feature, Map<Set<String>,Integer>>();

		List<InstanceProfile> instanceProfileList = new ArrayList<InstanceProfile>();
		
		Map<Set<String>, Set<String>> alphabetFeatureSetMap;
		Map<Set<String>, Integer>[] tempMap;
		for(Feature feature : featureAlphabetFeatureSetMap.keySet()){
			//Logger.println(feature);
			instanceProfileList.clear();
			switch (feature) {
				case IE:
				case TRA:
				case IE_TRA:
				case IE_TRA_MRA:
				case TRA_MRA:
					instanceProfileList.addAll(this.instanceProfileList);
					break;
				case MRA:
				case SMRA:
				case NSMRA:
					instanceProfileList.addAll(this.modifiedInstanceProfileList);
					break;
				default:
					instanceProfileList.addAll(this.instanceProfileList);
					break;
			}
			alphabetFeatureSetMap = featureAlphabetFeatureSetMap.get(feature);
			
			tempMap = findNonOverlapPatternCountOptimized(instanceProfileList, alphabetFeatureSetMap);
			featureAlphabetFeatureNOCMap.put(feature, tempMap[0]);
			featureAlphabetInstanceCountMap.put(feature, tempMap[1]);
		}
		
		//Logger.printReturn("Returning FeatureExtraction->computeNonOverlapAlphabetFeatureMetrics()-> "+featureAlphabetFeatureSetMap.size());
		return new Map[]{featureAlphabetFeatureNOCMap, featureAlphabetInstanceCountMap};
	}
	
	@SuppressWarnings("unchecked")
	private Map<Set<String>,Integer>[] findNonOverlapPatternCountOptimized(List<InstanceProfile> instanceProfileList, Map<Set<String>, Set<String>> alphabetFeatureSetMap) {
		//Logger.printCall("Calling FeatureExtraction->findNonOverlapPatternCountOptimized()-> feature set size: "+alphabetFeatureSetMap.size());

		Map<Set<String>, Integer> nonOverlapAlphabetCountMap = new HashMap<Set<String>, Integer>();
		Map<Set<String>, Integer> nonOverlapAlphabetInstanceCountMap = new HashMap<Set<String>, Integer>();

		Map<TreeSet<String>, Integer> alphabetCountMap = new HashMap<TreeSet<String>, Integer>();
		Map<TreeSet<String>, Integer> alphabetInstanceCountMap = new HashMap<TreeSet<String>, Integer>();
		Map<String, Integer> patternCountMap = new HashMap<String, Integer>();

		int patternCount;

		TreeSet<String> alphabetFeatureSet;

		Set<String> allAlphabetFeatureSet = new HashSet<String>();
		
		for (Set<String> alphabet : alphabetFeatureSetMap.keySet())
			allAlphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));

		int encodedTraceLength, count;
		boolean encodedTraceHasPattern;
		Pattern pattern;
		Matcher matcher;
		String maxCountPattern;
		int maxCount, noMatches, repeatLength;
		Set<TreeSet<String>> encodedTraceContributingAlphabetSet = new HashSet<TreeSet<String>>();
		
		String encodedTrace;
		for (InstanceProfile instanceProfile : instanceProfileList) {
			encodedTrace = instanceProfile.getEncodedTrace();
			encodedTraceContributingAlphabetSet.clear();
			encodedTraceLength = encodedTrace.length() / encodingLength;

			// For each alphabet, check first whether this subtrace contains the
			// repeat under the alphabet; if so, get the non-overlapping count
			// of that repeat alphabet
			for (Set<String> alphabet : alphabetFeatureSetMap.keySet()) {
				alphabetFeatureSet = new TreeSet<String>();
				alphabetFeatureSet.addAll(alphabetFeatureSetMap.get(alphabet));

				encodedTraceHasPattern = false;
				for (String repeatPattern : alphabetFeatureSet) {
					if (encodedTrace.contains(repeatPattern)) {
						encodedTraceHasPattern = true;
						break;
					}
				}
				if (encodedTraceHasPattern) {
					for (int i = 0; i < encodedTraceLength; i++) {
						maxCount = 0;
						maxCountPattern = "";
						for (String repeatPattern : alphabetFeatureSet) {
							// First check if this repeat pattern exists
							// starting at this index; only if it exists then
							// use the pattern matcher
							if (encodedTrace.indexOf(repeatPattern, i
									* encodingLength) == i * encodingLength) {
								// w.start();
								pattern = Pattern.compile("(" + repeatPattern
										+ "){1,}");
								matcher = pattern.matcher(encodedTrace);
								if (matcher.find(i * encodingLength)
										&& matcher.start() == i
												* encodingLength) {
									repeatLength = repeatPattern.length()
											/ encodingLength;
									noMatches = (matcher.end() - matcher
											.start())
											/ (repeatLength * encodingLength);
									if (noMatches > maxCount) {
										maxCount = noMatches;
										maxCountPattern = repeatPattern;
									}
								}
								// System.out.println("Took "+w.msecs()+" msecs for pattern matching");
							}
						}
						if (maxCount > 0) {
							// No need to actually compute the counts again as
							// we have already stored the maxCount and
							// maxCountPattern information
							repeatLength = maxCountPattern.length()
									/ encodingLength;
							i += repeatLength * maxCount - 1;
							patternCount = 0;
							if (patternCountMap.containsKey(maxCountPattern))
								patternCount = patternCountMap
										.get(maxCountPattern);
							patternCountMap.put(maxCountPattern, maxCount
									+ patternCount);
							patternCount = 0;
							if (alphabetCountMap.containsKey(alphabet)) {
								patternCount = alphabetCountMap.get(alphabet);
							}

							alphabetCountMap.put((TreeSet<String>) alphabet,
									maxCount + patternCount);

							encodedTraceContributingAlphabetSet
									.add((TreeSet<String>) alphabet);
						}
					}
				}
			}

			for (TreeSet<String> alphabet : encodedTraceContributingAlphabetSet) {
				count = 0;
				if (alphabetInstanceCountMap.containsKey(alphabet)) {
					count = alphabetInstanceCountMap.get(alphabet);
				}
				alphabetInstanceCountMap.put(alphabet, count + 1);
			}
		}
	
		for (Set<String> alphabet : alphabetFeatureSetMap.keySet()) {
			if(alphabetCountMap.containsKey(alphabet))
				nonOverlapAlphabetCountMap.put(alphabet, alphabetCountMap.get(alphabet));
			else
				nonOverlapAlphabetCountMap.put(alphabet, 0);

			if(alphabetInstanceCountMap.containsKey(alphabet))
				nonOverlapAlphabetInstanceCountMap.put(alphabet,(int) (alphabetInstanceCountMap.get(alphabet) * 100.0 / this.instanceProfileList.size()));
			else
				nonOverlapAlphabetInstanceCountMap.put(alphabet, 0);
		}
		
		//Logger.printReturn("Returning FeatureExtraction->findNonOverlapPatternCountOptimized()-> feature set size: "+alphabetFeatureSetMap.size());
		
		return new Map[]{nonOverlapAlphabetCountMap, nonOverlapAlphabetInstanceCountMap};
	}
	
	public void printFeatureSets(){
		FileIO io = new FileIO();
		String property = "java.io.tmpdir";
		String dir = System.getProperty(property)+"\\SignatureDiscovery\\FeatureExtraction";
		String delim = "\\^";
		List<String> encodedTraceList = new ArrayList<String>();
		for(InstanceProfile instanceProfile : instanceProfileList)
			encodedTraceList.add(instanceProfile.getEncodedTrace());
		io.writeToFile(dir, "encodedTraceList.txt", encodedTraceList);
		if(hasSequenceFeature){
			for(Feature feature : originalSequenceFeatureSetMap.keySet()){
				io.writeToFile(dir, feature+".txt", originalSequenceFeatureSetMap.get(feature));
				io.writeToFile(dir, feature+"NOC.txt", originalSequenceFeatureCountMap.get(feature), delim);
				io.writeToFile(dir, feature+"InstanceCount.txt", originalSequenceFeatureInstanceCountPercentageMap.get(feature), delim);
			}
			
			for(Feature feature : baseSequenceFeatureSetMap.keySet()){
				io.writeToFile(dir, "base_"+feature+".txt", baseSequenceFeatureSetMap.get(feature));
				io.writeToFile(dir, "base_"+feature+"NOC.txt", baseSequenceFeatureCountMap.get(feature), delim);
				io.writeToFile(dir, "base_"+feature+"InstanceCount.txt", baseSequenceFeatureInstanceCountPercentageMap.get(feature), delim);
			}
		}
		
		if(hasAlphabetFeature){
			for(Feature feature : originalAlphabetFeatureSetMap.keySet()){
				io.writeToFile(dir, feature+".txt", originalAlphabetFeatureSetMap.get(feature), delim);
				io.writeToFile(dir, feature+"NOC.txt", originalAlphabetFeatureCountMap.get(feature), delim);
				io.writeToFile(dir, feature+"InstanceCount.txt", originalAlphabetFeatureInstanceCountPercentageMap.get(feature), delim);
			}
			
			for(Feature feature : baseAlphabetFeatureSetMap.keySet()){
				io.writeToFile(dir, "base_"+feature+".txt", baseAlphabetFeatureSetMap.get(feature), delim);
				io.writeToFile(dir, "base_"+feature+"NOC.txt", baseAlphabetFeatureCountMap.get(feature), delim);
				io.writeToFile(dir, "base_"+feature+"InstanceCount.txt", baseAlphabetFeatureInstanceCountPercentageMap.get(feature), delim);
			}
		}
	}

	/*
	 * Bruce: return set of TandemRepeat feature
	 */
	public Map<Feature, Set<String>> getOriginalSequenceFeatureSetMap() {
		return originalSequenceFeatureSetMap;
	}

	public Map<Feature, Map<Set<String>, Set<String>>> getOriginalAlphabetFeatureSetMap() {
		return originalAlphabetFeatureSetMap;
	}

	/*
	 * Bruce: return set of TandemRepeat feature (base)
	 */
	public Map<Feature, Set<String>> getBaseSequenceFeatureSetMap() {
		return baseSequenceFeatureSetMap;
	}

	public Map<Feature, Map<Set<String>, Set<String>>> getBaseAlphabetFeatureSetMap() {
		return baseAlphabetFeatureSetMap;
	}

	public Map<Feature, Map<String, Integer>> getOriginalSequenceFeatureNOCMap() {
		return originalSequenceFeatureCountMap;
	}

	public Map<Feature, Map<String, Integer>> getOriginalSequenceFeatureInstanceCountPercentageMap() {
		return originalSequenceFeatureInstanceCountPercentageMap;
	}

	public Map<Feature, Map<Set<String>, Integer>> getOriginalAlphabetFeatureNOCMap() {
		return originalAlphabetFeatureCountMap;
	}

	public Map<Feature, Map<Set<String>, Integer>> getOriginalAlphabetFeatureInstanceCountPercentageMap() {
		return originalAlphabetFeatureInstanceCountPercentageMap;
	}

	public Map<Feature, Map<String, Integer>> getBaseSequenceFeatureNOCMap() {
		return baseSequenceFeatureCountMap;
	}

	public Map<Feature, Map<String, Integer>> getBaseSequenceFeatureInstanceCountPercentageMap() {
		return baseSequenceFeatureInstanceCountPercentageMap;
	}

	public Map<Feature, Map<Set<String>, Integer>> getBaseAlphabetFeatureNOCMap() {
		return baseAlphabetFeatureCountMap;
	}

	public Map<Feature, Map<Set<String>, Integer>> getBaseAlphabetFeatureInstanceCountPercentageMap() {
		return baseAlphabetFeatureInstanceCountPercentageMap;
	}
}	