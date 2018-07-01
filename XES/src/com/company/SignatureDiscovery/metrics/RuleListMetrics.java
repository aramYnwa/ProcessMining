package com.company.SignatureDiscovery.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

public class RuleListMetrics {
	String optionsString;
	List<String> ruleList;
	Map<String, Metrics> classMetricsMap;

	public RuleListMetrics(String optionsString, List<String> ruleList, Map<String, Metrics> classEvaluationMetricsMap){
		this.optionsString = optionsString;
		this.ruleList = new ArrayList<String>();
		this.ruleList.addAll(ruleList);
		this.classMetricsMap = classEvaluationMetricsMap;
	}

	public String getOptionsString() {
		return optionsString;
	}

	public List<String> getRuleList() {
		return ruleList;
	}

	public Map<String, Metrics> getClassMetricsMap() {
		return classMetricsMap;
	}
}
