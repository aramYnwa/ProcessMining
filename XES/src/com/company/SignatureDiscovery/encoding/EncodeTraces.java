package com.company.SignatureDiscovery.encoding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

/**
 * 
 * @author R. P. Jagadeesh Chandra Bose (JC)
 * @email  j.c.b.rantham.prabhakara@tue.nl
 * @date   02 July 2009
 * @version 1.0
 */
public class EncodeTraces {
	private List<String> charStreamList;
	private List<InstanceProfile> instanceProfileList;
	
	public EncodeTraces(Map<String, String> activityCharMap, XLog log) throws EncodingNotFoundException{
		charStreamList = new ArrayList<String>();
		instanceProfileList = new ArrayList<InstanceProfile>();

		StringBuilder charStreamBuilder = new StringBuilder();
		StringBuilder activityBuilder = new StringBuilder();
		
		String classLabel;
		
		XAttributeMap attributeMap;

		for(XTrace trace : log){
			charStreamBuilder.setLength(0);
			classLabel = trace.getAttributes().get("Label").toString();
			for(XEvent event : trace){
				attributeMap = event.getAttributes();
				activityBuilder.setLength(0);
				activityBuilder.append(attributeMap.get("concept:name").toString()).append("-").append(attributeMap.get("lifecycle:transition").toString());
				
				
				
				if(activityCharMap.containsKey(activityBuilder.toString())){
					charStreamBuilder.append(activityCharMap.get(activityBuilder.toString()));
				}else{
					throw new EncodingNotFoundException(activityBuilder.toString());
				}
			}
			charStreamList.add(charStreamBuilder.toString());
			instanceProfileList.add(new InstanceProfile(trace.getAttributes().get("concept:name").toString(), charStreamBuilder.toString(), classLabel));
		}
	}

	/*
	 * Bruce: CharStreamList is list of all encoded traces in the log
	 */
	public List<String> getCharStreamList() {
		return charStreamList;
	}
	
	/*
	 * Bruce: InstanceProfileList is list of all instance profile (case) in the event log 
	 */
	public List<InstanceProfile> getInstanceProfileList(){
		return instanceProfileList;
	}
}
