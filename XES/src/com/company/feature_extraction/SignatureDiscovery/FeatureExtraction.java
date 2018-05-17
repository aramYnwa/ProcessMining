package com.company.feature_extraction.SignatureDiscovery;

import com.company.feature_extraction.SignatureDiscovery.encoding.InstanceProfile;
import com.company.feature_extraction.SignatureDiscovery.types.Feature;
import com.company.feature_extraction.SignatureDiscovery.util.EquivalenceClass;
import com.company.feature_extraction.SignatureDiscovery.util.FileIO;
import com.company.feature_extraction.SignatureDiscovery.util.Logger;
import com.company.feature_extraction.SignatureDiscovery.util.UkkonenSuffixTree;
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

	Set<Feature> selectedFeatureSet;
	List<InstanceProfile> instanceProfileList;
	List<InstanceProfile> modifiedInstanceProfileList;
	Map<String, String> activityCharMap;
	Map<String, String> charActivityMap;
	
	Map<Feature, Set<String>> originalSequenceFeatureSetMap;
	Map<Feature, Map<Set<String>, Set<String>>> originalAlphabetFeatureSetMap;

	Map<Feature, Map<String, Integer>> originalSequenceFeatureCountMap;
	Map<Feature, Map<String, Integer>> originalSequenceFeatureInstanceCountPercentageMap;
	Map<Feature, Map<Set<String>, Integer>> originalAlphabetFeatureCountMap;
	Map<Feature, Map<Set<String>, Integer>> originalAlphabetFeatureInstanceCountPercentageMap;
	
	Map<Feature, Set<String>> baseSequenceFeatureSetMap;
	Map<Feature, Map<Set<String>, Set<String>>> baseAlphabetFeatureSetMap;
	Map<Feature, Map<String, Integer>> baseSequenceFeatureCountMap;
	Map<Feature, Map<String, Integer>> baseSequenceFeatureInstanceCountPercentageMap;

	Map<Feature, Map<Set<String>, Integer>> baseAlphabetFeatureCountMap;
	Map<Feature, Map<Set<String>, Integer>> baseAlphabetFeatureInstanceCountPercentageMap;
	
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
		Logger.printCall("Calling FeatureExtraction->computeFeatureSets()");
		
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
				case IE:
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
					System.out.println("kGramSet Size: "+kGramSet.size());
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
		}
		
		if (hasRepeatFeature || hasRepeatAlphabetFeature) {
			StringBuilder combinedStringBuilder = new StringBuilder();
			Set<String> charStreamSet = new HashSet<String>();

			modifiedInstanceProfileList = preprocessLogForTandemRepeats();
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
		
		Logger.printReturn("Returning FeatureExtraction->computeFeatureSets()");
	}
	
	private Set<String> getIndividualEventFeatures(){
		Logger.printCall("Calling FeatureExtraction->getIndividualEventFeatures()");
		
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
		
		Logger.printReturn("Returning FeatureExtraction->getIndividualEventFeatures() "+individualEventFeatureSet.size());

		return individualEventFeatureSet;
	}
	
	private Set<String> getKGrams(){
		Logger.printCall("Calling FeatureExtraction->getKGrams()");
		
		Set<String> kGramFeatureSet = new HashSet<String>();
		String encodedTrace;
		int encodedTraceLength;
		for(InstanceProfile instanceProfile : instanceProfileList){
			encodedTrace = instanceProfile.getEncodedTrace();
			encodedTraceLength = encodedTrace.length()/encodingLength;
			for(int i = 0; i < encodedTraceLength-kGramValue; i++)
				kGramFeatureSet.add(encodedTrace.substring(i*encodingLength, (i+kGramValue)*encodingLength));
		}
		
		Logger.printReturn("Returning FeatureExtraction->getKGrams() "+kGramFeatureSet.size());
		
		return kGramFeatureSet;
	}

	private void computeTandemRepeatFeatureSet(){
		Logger.printCall("Calling FeatureExtraction->computeTandemRepeatFeatureSet()");
		
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
		baseSequenceFeatureSetMap.put(Feature.TR, baseTandemRepeatSet);
		
		Logger.println("No. Tandem Repeat Sequence Features: "+tandemRepeatSet.size());
		Logger.println("No. Tandem Repeat (Base) Sequence Features: "+baseTandemRepeatSet.size());
		
		Logger.printReturn("Returning FeatureExtraction->computeTandemRepeatFeatureSet()");
	}
	
	private void computeTandemRepeatAlphabetFeatureSet(){
		Logger.printCall("Calling FeatureExtraction->computeTandemRepeatAlphabetFeatureSet()");
		
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
		
		Logger.println("No. Tandem Repeat Alphabet Features: "+originalAlphabetFeatureSetMap.get(Feature.TRA).size());
		Logger.println("No. Tandem Repeat (Base) Alphabet Features: "+baseAlphabetFeatureSetMap.get(Feature.TRA).size());

		Logger.printReturn("Returning FeatureExtraction->computeTandemRepeatAlphabetFeatureSet()");
	}
	
	private List<InstanceProfile> preprocessLogForTandemRepeats(){
		Logger.printCall("Calling FeatureExtraction->preprocessLogForTandemRepeats()");
		
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
			System.out.println("Original Trace: "+encodedInstance+" @ "+encodedInstanceLength);
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
		
		Logger.printReturn("Returning FeatureExtraction->preprocessLogForTandemRepeats()");
		return processedInstanceProfileList;
	}
	
	
	private void computeRepeatfeatureFeatureSetMap(int encodingLength, String charStream, Set<Feature> repeatFeatureSet){
		Logger.printCall("Calling FeatureExtraction->computeRepeatfeatureFeatureSetMap");
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
		
		Logger.printReturn("Returning FeatureExtraction->computeRepeatfeatureFeatureSetMap");
	}
	
	private void computeRepeatAlphabetfeatureFeatureSetMap(int encodingLength, String charStream, Set<Feature> repeatfeatureSet){
		Logger.printCall("Calling FeatureExtraction->computeRepeatAlphabetfeatureFeatureSetMap");
		
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
		
		Logger.printReturn("Returning FeatureExtraction->computeRepeatfeatureFeatureSetMap");
	}
	
		
	private void computeCombinationFeatureSets(){
		Logger.printCall("Calling FeatureExtraction->computeCombinationFeatureSets()");
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
		
		Logger.printReturn("Returning FeatureExtraction->computeCombinationFeatureSets()");
		
	}
	
	private void generateSequenceCombinationFeatureSet(Feature feature1, Feature feature2, Feature combinationFeature){
		Logger.printCall("Calling FeatureExtraction->generateSequenceCombinationFeatureSet()->"+feature1+","+feature2);
		
		Set<String> combinationSequenceFeatureSet = new HashSet<String>();
		combinationSequenceFeatureSet.addAll(originalSequenceFeatureSetMap.get(feature1));
		combinationSequenceFeatureSet.addAll(originalSequenceFeatureSetMap.get(feature2));
		originalSequenceFeatureSetMap.put(combinationFeature, combinationSequenceFeatureSet);
		
		Set<String> baseCombinationSequenceFeatureSet = new HashSet<String>();
		baseCombinationSequenceFeatureSet.addAll(baseSequenceFeatureSetMap.get(feature1));
		baseCombinationSequenceFeatureSet.addAll(baseSequenceFeatureSetMap.get(feature2));
		baseSequenceFeatureSetMap.put(combinationFeature, baseCombinationSequenceFeatureSet);
		
		Logger.printReturn("Returning FeatureExtraction->generateSequenceCombinationFeatureSet()->"+feature1+","+feature2);
	}
	
	private void generateAlphabetCombinationFeatureSet(Feature feature1, Feature feature2, Feature combinationFeature){
		Logger.printCall("Calling FeatureExtraction->generateAlphabetCombinationFeatureSet()->"+feature1+","+feature2);
		
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
		
		Logger.printReturn("Returning FeatureExtraction->generateAlphabetCombinationFeatureSet()->"+feature1+","+feature2);
	}
	
	private void computeUnionFeatureSet(){
		Logger.printCall("Calling FeatureExtraction->computeUnionFeatureSet()");
		
		if(hasSequenceFeature){
			Set<String> mixFeatureSet = new HashSet<String>();
			Set<String> baseMixFeatureSet = new HashSet<String>();
			for(Feature feature : selectedFeatureSet){
				System.out.println("feature: "+feature);
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
		
		Logger.printReturn("Returning FeatureExtraction->computeUnionFeatureSet()");
	}
	
	public void computeNonOverlapFeatureMetrics(){
		computeNonOverlapSequenceFeatureMetrics();
		computeNonOverlapAlphabetFeatureMetrics();
	}
	
	private void computeNonOverlapSequenceFeatureMetrics(){
		Logger.println("Computing nonOverlap Sequence Feature Metrics for Original Features");
		Map<Feature, Map<String,Integer>>[] tempMap = computeNonOverlapSequenceFeatureMetrics(originalSequenceFeatureSetMap); 
		originalSequenceFeatureCountMap = new HashMap<Feature, Map<String, Integer>>(tempMap[0]);
		originalSequenceFeatureInstanceCountPercentageMap = new HashMap<Feature, Map<String, Integer>>(tempMap[1]);
		
		Logger.println("Computing nonOverlap Sequence Feature Metrics for Base Features");
		tempMap = computeNonOverlapSequenceFeatureMetrics(baseSequenceFeatureSetMap);
		baseSequenceFeatureCountMap = new HashMap<Feature, Map<String, Integer>>(tempMap[0]);
		baseSequenceFeatureInstanceCountPercentageMap = new HashMap<Feature, Map<String, Integer>>(tempMap[1]);
	}
	
	@SuppressWarnings("unchecked")
	private Map<Feature, Map<String,Integer>>[] computeNonOverlapSequenceFeatureMetrics(Map<Feature, Set<String>> featureSequenceFeatureMap){
		Logger.printCall("Calling FeatureExtraction->computeNonOverlapSequenceFeatureMetrics()->"+featureSequenceFeatureMap.keySet());
		
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
			Logger.println(feature);
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
		
		Logger.printReturn("Returning FeatureExtraction->computeNonOverlapSequenceFeatureMetrics()->"+featureSequenceFeatureMap.keySet());
		return new Map[]{featureSequenceFeatureNOCMap, featureSequenceInstanceCountMap};
	}
	
	public Map<String, Integer> computeNonOverlapSequenceFeatureCountMap(int encodingLength, String encodedTrace, Set<String> featureSet){
//		Logger.printCall("Calling FeatureExtraction->computeNonOverlapSequenceFeatureCountMap()-> feature set size: "+featureSet.size());
		
		Map<String, Integer> encodedTraceSequenceFeatureCountMap = new HashMap<String, Integer>();
		
		EquivalenceClass equivalenceClass = new EquivalenceClass();
		Map<String, Set<String>> startSymbolEquivalenceClassMap = equivalenceClass.getStartSymbolEquivalenceClassMap(encodingLength, featureSet);
		Set<String> startSymbolEquivalenceClassSet;
		int encodedTraceLength = encodedTrace.length()/encodingLength;
		String currentSymbol;
		int count;
		for(int i = 0; i < encodedTraceLength; i++){
			currentSymbol = encodedTrace.substring(i*encodingLength, (i+1)*encodingLength);
			if(startSymbolEquivalenceClassMap.containsKey(currentSymbol)){
				startSymbolEquivalenceClassSet = startSymbolEquivalenceClassMap.get(currentSymbol);
//				System.out.println(currentSymbol+"@"+startSymbolEquivalenceClassSet);
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
		Logger.printCall("Calling FeatureExtraction->computeNonOverlapAlphabetFeatureMetrics()-> "+featureAlphabetFeatureSetMap.size());
		
		Map<Feature, Map<Set<String>,Integer>> featureAlphabetFeatureNOCMap = new HashMap<Feature, Map<Set<String>,Integer>>();
		Map<Feature, Map<Set<String>,Integer>> featureAlphabetInstanceCountMap = new HashMap<Feature, Map<Set<String>,Integer>>();

		List<InstanceProfile> instanceProfileList = new ArrayList<InstanceProfile>();
		
		Map<Set<String>, Set<String>> alphabetFeatureSetMap;
		Map<Set<String>, Integer>[] tempMap;
		for(Feature feature : featureAlphabetFeatureSetMap.keySet()){
			Logger.println(feature);
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
		
		Logger.printReturn("Returning FeatureExtraction->computeNonOverlapAlphabetFeatureMetrics()-> "+featureAlphabetFeatureSetMap.size());
		return new Map[]{featureAlphabetFeatureNOCMap, featureAlphabetInstanceCountMap};
	}
	
	@SuppressWarnings("unchecked")
	private Map<Set<String>,Integer>[] findNonOverlapPatternCountOptimized(List<InstanceProfile> instanceProfileList, Map<Set<String>, Set<String>> alphabetFeatureSetMap) {
		Logger.printCall("Calling FeatureExtraction->findNonOverlapPatternCountOptimized()-> feature set size: "+alphabetFeatureSetMap.size());

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
		
		Logger.printReturn("Returning FeatureExtraction->findNonOverlapPatternCountOptimized()-> feature set size: "+alphabetFeatureSetMap.size());
		
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

	public Map<Feature, Set<String>> getOriginalSequenceFeatureSetMap() {
		return originalSequenceFeatureSetMap;
	}

	public Map<Feature, Map<Set<String>, Set<String>>> getOriginalAlphabetFeatureSetMap() {
		return originalAlphabetFeatureSetMap;
	}

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