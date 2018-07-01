package com.company.SignatureDiscovery.learningalgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * 
 * @author R. P. Jagadeesh Chandra 'JC' Bose
 * @date 27 May 2010 (last modified)
 * @since 01 July 2009
 * @version 1.0 
 * @(c)R. P. Jagadeesh Chandra 'JC' Bose, Deparment of Mathematics
 *          and Computer Science, TU/e
 * 
 * This class computes the Hasse diagram for a given set of pattern alphabets
 */

public class AbstractionSetTheory {
	SimpleDirectedGraph<Set<String>, DefaultEdge> sg;
	Set<Set<String>> alphabetSet;
	Map<Integer, Set<Set<String>>> sizeAlphabetSetMap;
	Map<Set<String>, Set<Set<String>>> alphabetMaximalElementSetMap;
	Map<Set<String>, Set<Set<String>>> maximalElementSubsumedAlphabetSetMap;
	
	/*
	 * Store the sizes of the alphabet; It would be required when constructing
	 * the Hasse diagram; We try to put a link between two alphabets only if
	 * there is no path between them
	 */
	int[] sortedSizeArray;
	
	public AbstractionSetTheory(Set<Set<String>> alphabetSet){
		this.alphabetSet = alphabetSet;
		sizeAlphabetSetMap = new HashMap<Integer, Set<Set<String>>>();
		
		int alphabetSize;
		Set<Set<String>> sizeAlphabetSet;
		for(Set<String> alphabet : alphabetSet){
			alphabetSize = alphabet.size();
			if(sizeAlphabetSetMap.containsKey(alphabetSize)){
				sizeAlphabetSet = sizeAlphabetSetMap.get(alphabetSize);
			}else{
				sizeAlphabetSet = new HashSet<Set<String>>();
			}
			sizeAlphabetSet.add(alphabet);
			sizeAlphabetSetMap.put(alphabetSize, sizeAlphabetSet);
		}
		
		sortedSizeArray = new int[sizeAlphabetSetMap.size()];
		int index = 0;
		for(Integer size : sizeAlphabetSetMap.keySet())
			sortedSizeArray[index++] = size;
		
		Arrays.sort(sortedSizeArray);
		
		sg = new SimpleDirectedGraph<Set<String>, DefaultEdge>(DefaultEdge.class);
		for(Set<String> alphabet : alphabetSet)
			sg.addVertex(alphabet);
		
		buildLayeredGraph();
	}

	protected void buildGraph(){
		for(Set<String> alphabetI : alphabetSet){
			for(Set<String> alphabetJ : alphabetSet){
				if(!alphabetI.equals(alphabetJ)){
					if (alphabetJ.containsAll(alphabetI)) {
						DijkstraShortestPath<Set<String>, DefaultEdge> d = new DijkstraShortestPath<Set<String>, DefaultEdge>(
								sg, alphabetI, alphabetJ);
						if (d.getPathEdgeList() == null)
							sg.addEdge(alphabetI, alphabetJ);
					}else if(alphabetI.containsAll(alphabetJ)){
						DijkstraShortestPath<Set<String>, DefaultEdge> d = new DijkstraShortestPath<Set<String>, DefaultEdge>(
								sg, alphabetJ, alphabetI);
						if (d.getPathEdgeList() == null)
							sg.addEdge(alphabetJ, alphabetI);
					}
				}
			}
		}
	}
	
	protected void buildLayeredGraph(){
		int maxStepSize = sortedSizeArray.length-1;
		int lowerLayerAlphabetSize, upperLayerAlphabetSize;
		
		Set<Set<String>> lowerLayerAlphabetSet, upperLayerAlphabetSet;
		for(int step = 1; step <= maxStepSize; step++){
			for(int i = 0; i < sortedSizeArray.length-step; i++){
				lowerLayerAlphabetSize = sortedSizeArray[i];
				upperLayerAlphabetSize = sortedSizeArray[i+step];
				
				lowerLayerAlphabetSet = sizeAlphabetSetMap.get(lowerLayerAlphabetSize);
				upperLayerAlphabetSet = sizeAlphabetSetMap.get(upperLayerAlphabetSize);
				
				for(Set<String> alphabetI : lowerLayerAlphabetSet){
					for(Set<String> alphabetJ : upperLayerAlphabetSet){
						if (alphabetJ.containsAll(alphabetI)) {
							DijkstraShortestPath<Set<String>, DefaultEdge> d = new DijkstraShortestPath<Set<String>, DefaultEdge>(
									sg, alphabetI, alphabetJ);
							if (d.getPathEdgeList() == null)
								sg.addEdge(alphabetI, alphabetJ);
						}
					}
				}
			}
		}
	}
	
	public List<Set<String>> getMaximalElements(){
		List<Set<String>> maximalElementList = new ArrayList<Set<String>>();
		
		for(Set<String> alphabet : sg.vertexSet()){
			if(sg.outDegreeOf(alphabet) == 0)
				maximalElementList.add(alphabet);
		}
		
		alphabetMaximalElementSetMap = new HashMap<Set<String>, Set<Set<String>>>();
		maximalElementSubsumedAlphabetSetMap = new HashMap<Set<String>, Set<Set<String>>>();
		
		Set<Set<String>> alphabetMaximalElementSet;
		Set<Set<String>> maximalElementSubsumedAlphabetSet;
		boolean maximalElementSubsumesAlphabet;
		
		for(Set<String> alphabet : alphabetSet){
			for(Set<String> maximalElement : maximalElementList){
				maximalElementSubsumesAlphabet = false;
				if (!alphabet.equals(maximalElement)) {
					DijkstraShortestPath<Set<String>, DefaultEdge> d = new DijkstraShortestPath<Set<String>, DefaultEdge>(
							sg, alphabet, maximalElement);
					if (d.getPathEdgeList() != null) {
						if (alphabetMaximalElementSetMap.containsKey(alphabet))
							alphabetMaximalElementSet = alphabetMaximalElementSetMap.get(alphabet);
						else
							alphabetMaximalElementSet = new HashSet<Set<String>>();
						alphabetMaximalElementSet.add(maximalElement);
						alphabetMaximalElementSetMap.put(alphabet, alphabetMaximalElementSet);
						
						maximalElementSubsumesAlphabet = true;
					}
				}else{
					alphabetMaximalElementSet = new HashSet<Set<String>>();
					alphabetMaximalElementSet.add(maximalElement);
					alphabetMaximalElementSetMap.put(alphabet, alphabetMaximalElementSet);
					maximalElementSubsumesAlphabet = true;
				}
				
				if(maximalElementSubsumesAlphabet){
					if(maximalElementSubsumedAlphabetSetMap.containsKey(maximalElement))
						maximalElementSubsumedAlphabetSet = maximalElementSubsumedAlphabetSetMap.get(maximalElement);
					else
						maximalElementSubsumedAlphabetSet = new HashSet<Set<String>>();
					
					maximalElementSubsumedAlphabetSet.add(alphabet);
					maximalElementSubsumedAlphabetSetMap.put(maximalElement, maximalElementSubsumedAlphabetSet);
				}
			}
		}
		return maximalElementList;
	}
	
	public void printGraph(){
		System.out.println("AlphabetSet for Abstration");
		/*
		 * Print the alphabetSet First
		 */
		for(Integer size : sizeAlphabetSetMap.keySet()){
			System.out.println("Size "+size+": "+sizeAlphabetSetMap.get(size).size()+" : "+sizeAlphabetSetMap.get(size));
		}
		
		System.out.println("\nPrinting Graph\n");
		Set<Set<String>> sizeAlphabetSet;
		for(Integer size : sizeAlphabetSetMap.keySet()){
			System.out.println(size);
			System.out.println("----------------");
			sizeAlphabetSet = sizeAlphabetSetMap.get(size);
			for(Set<String> alphabet : sizeAlphabetSet){
				System.out.println(alphabet+" Outdegree: "+sg.outDegreeOf(alphabet));
				
				Set<DefaultEdge> outgoingEdgeSet = sg.outgoingEdgesOf(alphabet);
				for(DefaultEdge edge : outgoingEdgeSet)
					System.out.println("\t"+sg.getEdgeTarget(edge));
			}
		}
	}

	public Map<Set<String>, Set<Set<String>>> getAlphabetMaximalElementSetMap() {
		return alphabetMaximalElementSetMap;
	}

	public Map<Set<String>, Set<Set<String>>> getMaximalElementSubsumedAlphabetSetMap() {
		return maximalElementSubsumedAlphabetSetMap;
	}
	
	public Set<Set<String>> getSubsumedAlphabetSet(Set<String> maximalElement){
		if(maximalElementSubsumedAlphabetSetMap.containsKey(maximalElement))
			return maximalElementSubsumedAlphabetSetMap.get(maximalElement);
		else
			return null;
	}
}
