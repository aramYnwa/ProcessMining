package com.company.feature_extraction.SignatureDiscovery.metrics;

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

public class Metrics {
	int TP;
	int FP;
	int TN;
	int FN;
	int noInstances;

	public Metrics(int TP, int FP, int TN, int FN, int noInstances){
		this.TP = TP;
		this.FP = FP;
		this.TN = TN;
		this.FN = FN;
		this.noInstances = noInstances;
	}
	
	public void add(int TP, int FP, int TN, int FN, int noInstances){
		this.TP += TP;
		this.FP += FP;
		this.TN += TN;
		this.FN += FN;
		this.noInstances += noInstances;
	}
	
	/*
	 * True Positive Rate is also called as Sensitivity
	 */
	public double truePositiveRate(){
		return (TP*1.0)/(TP+FN);
	}
	
	/*
	 * True Negative Rate is also called as specificity
	 */
	public double trueNegativeRate(){
		return (TN * 1.0)/(FP + TN);
	}
	
	public double getF1Score(){
		return (2.0*TP) /(2*TP+FP+FN);
	}
	
	public double precision(){
		return TP * 1.0/(TP+FP);
	}
	
	public double precisionTP(){
		return TP * TP * 1.0/(TP+FP);
	}

	public int getTP() {
		return TP;
	}

	public int getFP() {
		return FP;
	}

	public int getTN() {
		return TN;
	}

	public int getFN() {
		return FN;
	}

	public int getNoInstances() {
		return noInstances;
	}
}
