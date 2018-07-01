package com.company.SignatureDiscovery.encoding;

/**
 * Represent one class label
 * @author Bruce, 26 May 2015
 *
 */
public class ClassProfile {
	private String label;
	private int instanceCount;
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public int getInstanceCount() {
		return instanceCount;
	}
	
	public void setInstanceCount(int count) {
		this.instanceCount = count;
	}
}
