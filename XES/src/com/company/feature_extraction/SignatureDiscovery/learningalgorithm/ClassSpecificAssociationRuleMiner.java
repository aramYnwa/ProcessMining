package com.company.feature_extraction.SignatureDiscovery.learningalgorithm;

import com.company.feature_extraction.SignatureDiscovery.metrics.Metrics;
import com.company.feature_extraction.SignatureDiscovery.metrics.RuleListMetrics;
import com.company.feature_extraction.SignatureDiscovery.types.Feature;
import com.company.feature_extraction.SignatureDiscovery.util.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

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

public class ClassSpecificAssociationRuleMiner {
	float minSupport;
	float minConfidence;
	Set<String> generateRulesForClassLabelSet;
	Map<Feature, RuleListMetrics> featureRuleListMetrics;
	public ClassSpecificAssociationRuleMiner(Map<Feature, Instances> featureInstancesMap, Set<String> generateRulesForClassLabelSet, float minSupport, float minConfidence){
		this.generateRulesForClassLabelSet = generateRulesForClassLabelSet;
		this.minSupport = minSupport;
		this.minConfidence = minConfidence;
//		System.out.println(minSupport+" @ "+minConfidence);
		findRules(featureInstancesMap);
	}
	
	private void findRules(Map<Feature, Instances> featureInstancesMap){
		Instances instances;
		List<Instance> interestedClassInstanceList;
		
		List<Instance> filteredInstanceList;
//		Set<Map<Attribute, String>> attributeValueMapSet;
		List<Map<Attribute, String>> attributeValueMapList;
		
		Map<String, Set<Map<Attribute, String>>> classValueAttributeValuePairSetMap;
		Map<String, Set<Map<Attribute, String>>> tempClassValueCombinationAttributeValuePairSetMap;
		Map<String, Set<Map<Attribute, String>>> classValueCombinationAttributeValuePairSetMap;
		Set<Map<Attribute, String>> classValueCombinationAttributeValuePairSet;
		
//		Set<Map<Attribute, String>> classValueAttributeValuePairSet;
		featureRuleListMetrics = new HashMap<Feature, RuleListMetrics>();
		for(Feature feature : featureInstancesMap.keySet()){
			instances = featureInstancesMap.get(feature);
			interestedClassInstanceList = getInstancesForRequiredClasses(instances);
			filteredInstanceList = interestedClassInstanceList;
//			System.out.println("Feature: "+feature+" No. Features: "+(instances.numAttributes()-1));
//			System.out.println("No. Instances of interested classes: "+interestedClassInstanceList.size());
			
			attributeValueMapList = getAttributeValueMapList(filteredInstanceList);
//			System.out.println("No. Attributes satisfying min Support: "+attributeValueMapList.size());
//			for(Map<Attribute, String> attributeValueMap : attributeValueMapList){
//				for(Attribute attribute : attributeValueMap.keySet()){
//					System.out.println(attribute.name()+" @ "+attributeValueMap.get(attribute)+" @ "+getSupport(attribute, attributeValueMap.get(attribute),filteredInstanceList));
//				}
//			}

			classValueAttributeValuePairSetMap = getClassValueAttributeValuePairSetMap(attributeValueMapList, filteredInstanceList);
			
			tempClassValueCombinationAttributeValuePairSetMap = new HashMap<String, Set<Map<Attribute,String>>>(classValueAttributeValuePairSetMap);
			classValueCombinationAttributeValuePairSetMap = new HashMap<String, Set<Map<Attribute,String>>>(classValueAttributeValuePairSetMap);
			
//			for(String classValue : classValueAttributeValuePairSetMap.keySet()){
//				classValueAttributeValuePairSet = classValueAttributeValuePairSetMap.get(classValue);
//				for(Map<Attribute, String> attributeValueMap : classValueAttributeValuePairSet){
//					for(Attribute attribute : attributeValueMap.keySet()){
//						System.out.print(attribute.name()+" = "+attributeValueMap.get(attribute)+" , ");
//					}
//					
//					System.out.println(" Then "+classValue);
//				}
//			}
			
			int noIterations = 0;
			do{
				tempClassValueCombinationAttributeValuePairSetMap = getCombinationAssociations(tempClassValueCombinationAttributeValuePairSetMap, classValueAttributeValuePairSetMap, filteredInstanceList);
				if(tempClassValueCombinationAttributeValuePairSetMap.size() > 0){
//					System.out.println("Got Combinations: ");
//					for(String classValue : tempClassValueCombinationAttributeValuePairSetMap.keySet()){
//						attributeValueMapSet = tempClassValueCombinationAttributeValuePairSetMap.get(classValue);
//						for(Map<Attribute, String> attributeValueMap : attributeValueMapSet){
//							for(Attribute attribute : attributeValueMap.keySet())
//								System.out.print(attribute.name()+" = "+attributeValueMap.get(attribute)+", ");
//							System.out.println(" Then "+classValue);
//						}
//					}
					for(String classValue : tempClassValueCombinationAttributeValuePairSetMap.keySet()){
						if(classValueCombinationAttributeValuePairSetMap.containsKey(classValue)){
							classValueCombinationAttributeValuePairSet = classValueCombinationAttributeValuePairSetMap.get(classValue);
							classValueCombinationAttributeValuePairSet.addAll(tempClassValueCombinationAttributeValuePairSetMap.get(classValue));
							classValueCombinationAttributeValuePairSetMap.put(classValue, classValueCombinationAttributeValuePairSet);
						}else{
							classValueCombinationAttributeValuePairSetMap.put(classValue, tempClassValueCombinationAttributeValuePairSetMap.get(classValue));
						}
					}
				}
				noIterations++;
			}while(tempClassValueCombinationAttributeValuePairSetMap.size() > 0);
			
			List<String> ruleList = getMaximalAssociations(classValueCombinationAttributeValuePairSetMap);
			featureRuleListMetrics.put(feature, evaluateRuleList(ruleList, instances));
		}
	}
	
	private List<Instance> getInstancesForRequiredClasses(Instances instances){
		List<Instance> interestedClassInstanceList = new ArrayList<Instance>();
		Attribute classAttribute = instances.classAttribute();
		for(Instance instance : instances){
			if(generateRulesForClassLabelSet.contains(instance.stringValue(classAttribute))){
				interestedClassInstanceList.add(instance);
			}
		}
		return interestedClassInstanceList;
	}
	
	private List<Map<Attribute, String>> getAttributeValueMapList(List<Instance> instanceList){
		List<Map<Attribute, String>> attributeValueMapList = new ArrayList<Map<Attribute,String>>();
		Map<Attribute, String> attributeValueMap;
		
		Instance instance = instanceList.get(0);
		int noAttributes = instance.numAttributes();
		int noAttributeValues;
		Attribute currentAttribute, classAttribute;
		classAttribute = instance.classAttribute();
		for(int i = 0; i < noAttributes; i++){
			currentAttribute = instance.attribute(i);
			if(!currentAttribute.equals(classAttribute)){
				noAttributeValues = currentAttribute.numValues();
				for(int j = 0; j < noAttributeValues; j++){
					if(currentAttribute.value(j).equalsIgnoreCase("0"))
						continue;
					if(getSupport(currentAttribute, currentAttribute.value(j), instanceList) > minSupport){
						attributeValueMap = new HashMap<Attribute, String>();
						attributeValueMap.put(currentAttribute, currentAttribute.value(j));
						attributeValueMapList.add(attributeValueMap);
					}
				}
				
			}
		}
		return attributeValueMapList;
	}
	
	private Map<String, Set<Map<Attribute, String>>> getClassValueAttributeValuePairSetMap(List<Map<Attribute, String>> attributeValueMapList, List<Instance> instanceList){
		Map<String, Set<Map<Attribute, String>>> classValueAttributeValuePairSetMap = new HashMap<String, Set<Map<Attribute,String>>>();
		Set<Map<Attribute, String>> classValueAttributeValuePairSet;
		
		Instance instance = instanceList.get(0);
		Attribute classAttribute = instance.classAttribute();
		float confidence;
		List<Instance> filteredInstanceList;
		for(Map<Attribute, String> attributeValueMap : attributeValueMapList){
			filteredInstanceList = getFilteredInstanceList(attributeValueMap, instanceList);
			for(String classValue : generateRulesForClassLabelSet){
				confidence =getConfidence(classAttribute, classValue, filteredInstanceList); 
//				System.out.println("confidence "+confidence);
				if(confidence > minConfidence){
					if(classValueAttributeValuePairSetMap.containsKey(classValue)){
						classValueAttributeValuePairSet = classValueAttributeValuePairSetMap.get(classValue);
					}else{
						classValueAttributeValuePairSet = new HashSet<Map<Attribute,String>>();
					}
					classValueAttributeValuePairSet.add(attributeValueMap);
					classValueAttributeValuePairSetMap.put(classValue, classValueAttributeValuePairSet);
				}
			}
		}
		
		return classValueAttributeValuePairSetMap;
	}
	
	private Map<String, Set<Map<Attribute, String>>> getCombinationAssociations(Map<String, Set<Map<Attribute, String>>> classValueAttributeValuePairSetMapA, Map<String, Set<Map<Attribute, String>>> classValueAttributeValuePairSetMapB, List<Instance> instanceList){
//		System.out.println("In getCombination");
		
		Set<Map<Attribute, String>> classValueAttributeValuePairSet;
		List<Map<Attribute, String>> classValueAttributeValuePairListA = new ArrayList<Map<Attribute,String>>();
		List<Map<Attribute, String>> classValueAttributeValuePairListB = new ArrayList<Map<Attribute,String>>();
		int noAttributeValuePairMapsA, noAttributeValuePairMapsB;
		Map<Attribute, String> tempAttributeValueMap;
		
		Set<Attribute> attributeSetA = new HashSet<Attribute>();
		Set<Attribute> attributeSetB = new HashSet<Attribute>();
		Set<Attribute> tempAttributeSet = new HashSet<Attribute>();
		Set<Set<Attribute>> attributeCombinationSet = new HashSet<Set<Attribute>>();
		
		Attribute classAttribute = instanceList.get(0).classAttribute();
		List<Instance> filteredInstanceList;
		
		Map<String, Set<Map<Attribute, String>>> classValueCombinationAttributeValuePairSetMap = new HashMap<String, Set<Map<Attribute,String>>>();
		Set<Map<Attribute, String>> classValueCombinationAttributeValuePairSet;
		
		for(String classValue : classValueAttributeValuePairSetMapA.keySet()){
			if(!classValueAttributeValuePairSetMapB.containsKey(classValue))
				continue;
//			System.out.println("Class Value: "+classValue);
			classValueAttributeValuePairSet = classValueAttributeValuePairSetMapA.get(classValue);
			classValueAttributeValuePairListA.clear();
			classValueAttributeValuePairListA.addAll(classValueAttributeValuePairSet);
			noAttributeValuePairMapsA = classValueAttributeValuePairListA.size();
			
			classValueAttributeValuePairSet = classValueAttributeValuePairSetMapB.get(classValue);
			classValueAttributeValuePairListB.clear();
			classValueAttributeValuePairListB.addAll(classValueAttributeValuePairSet);
			noAttributeValuePairMapsB = classValueAttributeValuePairListB.size();
			
//			System.out.println("No. Attribute Value Pair Maps A: "+noAttributeValuePairMapsA);
//			System.out.println("No. Attribute Value Pair Maps B: "+noAttributeValuePairMapsB);
			
			for(int i = 0; i < noAttributeValuePairMapsA; i++){
				attributeSetA.clear();
				attributeSetA.addAll(classValueAttributeValuePairListA.get(i).keySet());
				for(int j = 0; j < noAttributeValuePairMapsB; j++){
					attributeSetB.clear();
					attributeSetB.addAll(classValueAttributeValuePairListB.get(j).keySet());
					
					tempAttributeSet.clear();
					tempAttributeSet.addAll(attributeSetA);
					tempAttributeSet.retainAll(attributeSetB);
					
					if(tempAttributeSet.size() == 0 && !attributeCombinationSet.contains(tempAttributeSet)){
						tempAttributeValueMap  = new HashMap<Attribute, String>(classValueAttributeValuePairListA.get(i));
						tempAttributeValueMap.putAll(classValueAttributeValuePairListB.get(j));
//						System.out.println(attributeSetA+" ^ "+attributeSetB+" ^ "+tempAttributeValueMap.keySet());
						attributeCombinationSet.add(tempAttributeValueMap.keySet());
						if(getSupport(tempAttributeValueMap, instanceList) > minSupport){
							filteredInstanceList = getFilteredInstanceList(tempAttributeValueMap, instanceList);
							if(getConfidence(classAttribute, classValue, filteredInstanceList) > minConfidence){
								if(classValueCombinationAttributeValuePairSetMap.containsKey(classValue)){
									classValueCombinationAttributeValuePairSet = classValueCombinationAttributeValuePairSetMap.get(classValue);
								}else{
									classValueCombinationAttributeValuePairSet = new HashSet<Map<Attribute,String>>();
								}
								classValueCombinationAttributeValuePairSet.add(tempAttributeValueMap);
								classValueCombinationAttributeValuePairSetMap.put(classValue, classValueCombinationAttributeValuePairSet);
								
//								for(Attribute attribute : tempAttributeValueMap.keySet()){
//									System.out.print(attribute.name()+" = "+tempAttributeValueMap.get(attribute)+" , ");
//								}
//								
//								System.out.println(" Then "+classValue);
							}
						}
					}
				}
			}
			
		}
		
		return classValueCombinationAttributeValuePairSetMap;
	}
	
	private List<String> getMaximalAssociations(Map<String, Set<Map<Attribute, String>>> classValueAttributeValuePairSetMap){
		List<String> ruleList = new ArrayList<String>();
//		System.out.println("In getMaximalAssociations()");
		Set<Map<Attribute, String>> classValueAttributeValuePairSet;
		Set<Set<String>> associationCombinationSet = new HashSet<Set<String>>();
		Set<String> associationSet;
		AbstractionSetTheory ast;
		List<Set<String>> maximalAssociationList;
		Iterator<String> it;
		String antecedant;
		for(String classValue : classValueAttributeValuePairSetMap.keySet()){
			classValueAttributeValuePairSet = classValueAttributeValuePairSetMap.get(classValue);
			associationCombinationSet.clear();
//			System.out.println(classValue+" @ No. AttributeValuePairSets: "+classValueAttributeValuePairSet.size());
			for(Map<Attribute, String> attributeValueMap : classValueAttributeValuePairSet){
				associationSet = new HashSet<String>();
				for(Attribute attribute : attributeValueMap.keySet()){
					associationSet.add(attribute.name()+"="+attributeValueMap.get(attribute));
				}
				associationCombinationSet.add(associationSet);
			}
//			System.out.println("Association Combination Set Size: "+associationCombinationSet.size());
			ast = new AbstractionSetTheory(associationCombinationSet);
			maximalAssociationList = ast.getMaximalElements();
			String rule;
			for(Set<String> association : maximalAssociationList){
				rule = "IF ";
				it = association.iterator();
				while(it.hasNext()){
					antecedant = it.next();
					rule += antecedant;
					if(it.hasNext())
						rule += " AND ";
				}
				rule += " THEN "+classValue;
				ruleList.add(rule);
//				System.out.println(rule);
			}
		}
		return ruleList;
	}
	
	@SuppressWarnings("unused")
	private void findRules1(Map<Feature, Instances> featureInstancesMap){
		Instances instances;
		List<Instance> filteredInstanceList = new ArrayList<Instance>();
		String classValue;

		String antecedant;
		int noAttributeValues;
		float support, confidence;
		Attribute attribute;
		String attributeValue;
		int noAttributes;
		int noInstances, noFilteredInstances;
		String attributeClassValuePair;
		int count;
		Map<String, Integer> attributeValueInstanceCountMap = new HashMap<String, Integer>();
		Map<String, Integer> attributeClassValuePairInstanceCountMap = new HashMap<String, Integer>();
		for(Feature feature : featureInstancesMap.keySet()){
//			System.out.println(feature);
			instances = featureInstancesMap.get(feature);
			noInstances = instances.numInstances();
			noAttributes = instances.numAttributes()-1;
			filteredInstanceList.clear();
			
			for(Instance instance : instances){
				classValue = instance.stringValue(instance.classAttribute());
				if(generateRulesForClassLabelSet.contains(classValue))
					filteredInstanceList.add(instance);
			}
			
			noFilteredInstances = filteredInstanceList.size();
			
			for(int i = 0; i < noAttributes; i++){
				attribute = instances.attribute(i);
				noAttributeValues = attribute.numValues();
				
				for(int j = 0; j < noAttributeValues; j++){
					attributeValue = instances.attribute(i).value(j);
					antecedant = instances.attribute(i).name()+" = "+attributeValue;
					// get the support of this antecedant
					support = 0;
					for(Instance instance : filteredInstanceList){
						if(instance.stringValue(attribute).equals(attributeValue))
							support++;
					}
					attributeClassValuePairInstanceCountMap.clear();
					if(support/noFilteredInstances > minSupport){
//						System.out.println(antecedant+" has support "+support/noFilteredInstances);
						attributeValueInstanceCountMap.clear();
						for(Instance instance : filteredInstanceList){
							if(instance.stringValue(attribute).equals(attributeValue)){
								classValue = instance.stringValue(instance.classAttribute());
								attributeClassValuePair = antecedant+"@"+classValue;
								count = 1;
								if(attributeValueInstanceCountMap.containsKey(attributeValue))
									count += attributeValueInstanceCountMap.get(attributeValue);
								attributeValueInstanceCountMap.put(attributeValue, count);
								
								count = 1;
								if(attributeClassValuePairInstanceCountMap.containsKey(attributeClassValuePair)){
									count += attributeClassValuePairInstanceCountMap.get(attributeClassValuePair);
								}
								attributeClassValuePairInstanceCountMap.put(attributeClassValuePair, count);
							}
						}
						
						for(String acPair : attributeClassValuePairInstanceCountMap.keySet()){
							count = attributeClassValuePairInstanceCountMap.get(acPair);
							confidence = (((float)count)/attributeValueInstanceCountMap.get(attributeValue));
//							System.out.print(acPair+" has confidence "+confidence);
//							if(confidence > minConfidence){
//								System.out.println(" above min confidence "+minConfidence);
//							}
						}
					}
				}
			}
			
			
//			System.out.println("No. Filtered Instances: "+filteredInstanceList.size());
		}
	}
	
	private float getSupport(Attribute attribute, String attributeValue, List<Instance> instanceList){
//		System.out.println("Computing support for: "+attribute.name()+"@"+attributeValue+":");
		float support = 0;
		for(Instance instance : instanceList){
			if(instance.stringValue(attribute).equals(attributeValue))
				support++;
		}
		return support/instanceList.size();
	}
	
	private float getSupport(Map<Attribute, String> attributeValueMap, List<Instance> instanceList){
		float support = 0;
		boolean allSatisfied;
		//If all attribute values are zero, return support = 0
		boolean allZero = true;
		for(Attribute attribute : attributeValueMap.keySet()){
			if(!attributeValueMap.get(attribute).equalsIgnoreCase("0")){
				allZero = false;
				break;
			}
		}
		if(allZero)
			return support;
		for(Instance instance : instanceList){
			allSatisfied = true;
			for(Attribute attribute : attributeValueMap.keySet()){
				if(!instance.stringValue(attribute).equals(attributeValueMap.get(attribute))){
					allSatisfied = false;
					break;
				}
			}
			if(allSatisfied)
				support++;
		}
		return support/instanceList.size();
	}
	
	private List<Instance> getFilteredInstanceList(Map<Attribute, String> attributeValueMap, List<Instance> instanceList){
		List<Instance> filteredInstanceList = new ArrayList<Instance>();
		boolean allSatisfied;
		for(Instance instance : instanceList){
			allSatisfied = true;
			for(Attribute attribute : attributeValueMap.keySet()){
				if(!instance.stringValue(attribute).equals(attributeValueMap.get(attribute))){
					allSatisfied = false;
					break;
				}
			}
			if(allSatisfied)
				filteredInstanceList.add(instance);
		}
		
		return filteredInstanceList;
	}
	
	private float getConfidence(Attribute classAttribute, String classValue, List<Instance> instanceList){
		float confidence = 0;
		for(Instance instance : instanceList){
			if(instance.stringValue(classAttribute).equals(classValue))
				confidence++;
		}
		return confidence/instanceList.size();
	}
	
	
	private RuleListMetrics evaluateRuleList(List<String> ruleList, Instances data){
		Logger.printCall("Calling evaluateRuleList()");
		RuleListMetrics ruleListMetrics;
		Map<String, Metrics> classMetricsMap = new HashMap<String, Metrics>();
		
		int noInstances = data.numInstances();
		Instance instance;
		
		String[] ruleSplit;
		String[] antecedantSplit, antecedantContraintSplit;

		int noAntecedants;
		List<String> antecedantAttributeList = new ArrayList<String>();
		List<String> antecedantConstraintList = new ArrayList<String>();
		String nominalValue;
		boolean isAntecedantConstraintsSatisfied;
		List<Integer> ruleSatisifyingInstanceList = new ArrayList<Integer>();
		List<Integer> constraintSatisifyingInstanceList = new ArrayList<Integer>();
		
		Logger.println("No. Instances: "+data.numInstances()+" @ No.Rules: "+ruleList.size());
		
		int noInstancesWithRuleClassValue;
		int tp, fp, tn, fn;
		for(String classValue : generateRulesForClassLabelSet){
			tp = fp = tn = fn = 0;
			noInstancesWithRuleClassValue = 0;
			for(String rule : ruleList){
				ruleSplit = rule.replaceAll("IF ", "").split(" THEN ");
				if(!ruleSplit[1].trim().equals(classValue))
					continue;
				antecedantSplit = ruleSplit[0].split(" AND ");
				noAntecedants = antecedantSplit.length;
				
				Logger.println("Rule: "+rule);
				Logger.println("No. Antecedants: "+noAntecedants);
				
				antecedantAttributeList.clear();
				antecedantConstraintList.clear();
				for(String antecedant : antecedantSplit){
					antecedantContraintSplit = antecedant.split("=");
	//				Logger.println(antecedant+"@("+antecedantContraintSplit[0].trim()+","+data.attribute(antecedantContraintSplit[0].trim()).toString()+")@"+antecedantContraintSplit[1]);
					antecedantAttributeList.add(antecedantContraintSplit[0].trim());
					antecedantConstraintList.add(antecedantContraintSplit[1].trim());
				}
				
				Logger.println("Consequent: "+ruleSplit[1]);
				
				
				constraintSatisifyingInstanceList.clear();
				ruleSatisifyingInstanceList.clear();

				for(int i = 0; i < noInstances; i++){
					instance = data.instance(i);
		
					if(instance.stringValue(instance.classAttribute()).equals(ruleSplit[1].trim()))
						noInstancesWithRuleClassValue++;
					
					isAntecedantConstraintsSatisfied = true;
					for(int j = 0; j < noAntecedants; j++){
						//Nominal attributes have only constraints as =
						nominalValue = instance.stringValue(data.attribute(antecedantAttributeList.get(j)));
	//						System.out.println(antecedantAttributeList.get(j)+"@"+data.attribute(antecedantAttributeList.get(j))+"@ Nominal value: "+nominalValue+"@ Cons: "+antecedantConstraintList.get(j));
						if(!nominalValue.equals(antecedantConstraintList.get(j))){
							isAntecedantConstraintsSatisfied = false;
							break;
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
			}
			classMetricsMap.put(classValue, new Metrics(tp, fp, tn, fn, noInstancesWithRuleClassValue));
		}
		ruleListMetrics = new RuleListMetrics("ARM minSupport "+minSupport+" minConfidence "+minConfidence, ruleList, classMetricsMap);
		Logger.printReturn("Returning evaluateRuleList()");
		return ruleListMetrics;
	}
	
	public Map<Feature, RuleListMetrics> getFeatureRuleListMetrics(){
		return featureRuleListMetrics;
	}
}
