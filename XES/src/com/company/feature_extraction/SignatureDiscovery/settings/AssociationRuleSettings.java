package com.company.feature_extraction.SignatureDiscovery.settings;

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

public class AssociationRuleSettings {
	public boolean isClassAssociationRules;
	public String minSupportValueStr, maxSupportValueStr;
	public String sortRulesMetricStr;
	public String sortRulesMetricValueStr;
	
	public AssociationRuleSettings(boolean isClassAssociationRules, String sortRulesMetricStr, String sortRulesMetricValueStr, String minSupportValueStr, String maxSupportValueStr){
		this.isClassAssociationRules = isClassAssociationRules;
		this.sortRulesMetricStr = sortRulesMetricStr;
		this.sortRulesMetricValueStr = sortRulesMetricValueStr;
		this.minSupportValueStr = minSupportValueStr;
		this.maxSupportValueStr = maxSupportValueStr;
	}

	public String getMinSupportValueStr() {
		return minSupportValueStr;
	}

	public String getSortRulesMetricValueStr() {
		return sortRulesMetricValueStr;
	}
}
