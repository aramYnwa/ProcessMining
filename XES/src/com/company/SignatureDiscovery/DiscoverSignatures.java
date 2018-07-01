package com.company.SignatureDiscovery;

import com.company.SignatureDiscovery.encoding.ActivityOverFlowException;
import com.company.SignatureDiscovery.encoding.EncodeActivitySet;
import com.company.SignatureDiscovery.encoding.EncodeTraces;
import com.company.SignatureDiscovery.encoding.EncodingNotFoundException;
import com.company.SignatureDiscovery.encoding.InstanceProfile;
import com.company.SignatureDiscovery.encoding.InstanceVector;
import com.company.SignatureDiscovery.featureextraction.FeatureExtraction;
import com.company.SignatureDiscovery.learningalgorithm.ClassSpecificAssociationRuleMiner;
import com.company.SignatureDiscovery.metrics.Metrics;
import com.company.SignatureDiscovery.metrics.RuleListMetrics;
import com.company.SignatureDiscovery.tool.ConnectionManager;
import com.company.SignatureDiscovery.types.EvaluationOptionType;
import com.company.SignatureDiscovery.types.Feature;
import com.company.SignatureDiscovery.types.FeatureType;
import com.company.SignatureDiscovery.types.LearningAlgorithmType;
import com.company.SignatureDiscovery.util.FileIO;
import com.company.SignatureDiscovery.util.Logger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.UniqueHashCode;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;


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

public class DiscoverSignatures {
	XLog log;
	com.company.SignatureDiscovery.SignatureDiscoveryInput input;
	
	/*
	 * Number of characters used for encoded name of activity name.
	 * This is used to recognize the activity in the encoded trace
	 */
	int encodingLength;
	
	/*
	 * List of all traces from the log in form of encoded string
	 * (ab0ab1ab2cd0cd1cd2de0de1ef0ef1ef2..., ab0cd0cd1de0de1fg0fg1fg2, cd0cd1ab0ab1ab2ab3ab4cd2vf2, ....)
	 */
	List<String> encodedTraceList;
	

	List<String> loopReductEncodedTraceList;
	
	com.company.SignatureDiscovery.featureextraction.FeatureExtraction featureExtraction;
	
	/*
	 * List of all instance profile (ID, encoded trace, label) from the event log
	 * ID		EncodedTrace						Label
	 * 0001		ab0ab1ab2cd0cd1ab0de0de1hd0hd1 		Quick
	 * 0002		cd0cd1cd2de0de1df0de1hg1hd1hdd 		Slow
	 * 0003		cd0cd1ab0ab1cdeow019dd0191019d 		Quick
	 */	
	List<InstanceProfile> instanceProfileList;

	/*
	 * (TR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 * (MR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))	 * 
	 */
	Map<Feature, Set<String>> actualFeatureSequenceFeatureSetMap;
	
	/*
	 * (TR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 * (MR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))	 * 
	 */	
	Map<Feature, Map<String, Integer>> actualFeatureSequenceNOCMap;
	
	
	Map<Feature, Map<String, Integer>> actualFeatureSequenceInstanceCountPercentageMap;
	Map<Feature, Map<Set<String>, Integer>> actualFeatureAlphabetNOCMap;
	Map<Feature, Map<Set<String>, Integer>> actualFeatureAlphabetInstanceCountPercentageMap;
	
	/* For each feature type (ATR,AMR), map to the following format
	 * Map<Set<String>, Set<String>>
	 * ("ab0", "ab1"), ("ab0ab1", "ab1ab0")
	 * ("cd0", "cd1", "cd2), ("cd0cd1cd2", "cd0cd2cd1", "cd1cd0cd2", "cd1cd2cd0")	
	 * Key: set of alphabet 
	 * Value: set of equivalence classes of the key 
	 */	
	Map<Feature, Map<Set<String>, Set<String>>> actualFeatureAlphabetFeatureSetMap;
	
	/*
	 * (TR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))
	 * (MR, (ab0ab1, cd0cd1cd2, de0de1, ...,xy0xy1xy2))	 * 
	 */	
	Map<Feature, Set<String>> filteredActualFeatureSequenceFeatureSetMap;
	

	/* For each feature type (ATR,AMR), map to the following format
	 * Map<Set<String>, Set<String>>
	 * ("ab0", "ab1"), ("ab0ab1", "ab1ab0")
	 * ("cd0", "cd1", "cd2), ("cd0cd1cd2", "cd0cd2cd1", "cd1cd0cd2", "cd1cd2cd0")
	 * In the second map:
	 * 		- Key: the first key set is the alphabet feature in form of set of alphabet
	 * 		- Value: set of equivalence classes of the key (they are repeat feature)
	 */	
	Map<Feature, Map<Set<String>, Set<String>>> filteredActualFeatureAlphabetFeatureSetMap;
	
	/**
	 * Result of feature selection based on Fisher score
	 * Key: feature type, e.g. TR/MR/MRA
	 * Value: mapping from feature to the computed fisher score
	 */
	Map<Feature, Map<String, Double>> filterFeatureSelectionMap = new HashMap<Feature, Map<String, Double>>();
	
	
	/*
	 * Contains map from feature type (TR, MR) to list of instance vectors
	 * Note: those features with zero frequency in the trace is not stored in the instance vector
	 * Example list of instance vector (sequence feature)
	 * (ab0ab1, 2), (cd0cd1cd2, 3), (de0de1, 4)
	 * (ab0ab1, 3), (cd0cd1cd2, 5), (de0de1, 2)
	 * (ab0ab1, 5), (cd0cd1cd2, 2), (de0de1, 2)
	 * 
	 * OR for alphabet features
	 * {"ab0","ab1","ab2"},5
	 * {"ab0","ab3"},3
	 * {"cd0","cd1","cd2","cd3"},8 
	 */
	Map<Feature, List<InstanceVector>> featureInstanceVectorListMap; //Bruce: from 1 June 2015, THIS IS FOR TRAINING DATA ONLY
	
	/*
	 * Contains map from feature type (TR,MR) to a list of features
	 * Note: here contains all features, including those with zero frequency in certain traces
	 * Example for sequence feature: ("ab0ab1", "cd0cd1cd2", "de0de1", "ef0ef1ef2"...)
	 * For alphabet features: it is list of string converted from a Set, so it has the following format
	 * 		("[ab0],[ab1],[ab2]", "[cd0],[cd1],[cd2]"}, "[de0],[de1]",...), each is an alphabet.
	 */
	Map<Feature, List<String>> featureAttributeNameListMap;
	
	/*
	 * Map from feature type (TR, MR...) to Weka instance containing data set for analysis
	 * This data set is read from a file with naming format: <featuretype>_Nominal.arff
	 */
	Map<Feature, Instances> featureNominalWekaInstancesMap = new HashMap<Feature, Instances>();
	
	/*
	 * Map from feature type (TR, MR...) to Weka instance containing data set for analysis
	 * This data set is read from a file with naming format: <featuretype>_Numeric.arff
	 */	
	Map<Feature, Instances> featureNumericWekaInstancesMap = new HashMap<Feature, Instances>();
	
	/*
	 * Mapping from encoded activity name to real activity name
	 * ("ab0","Arrive_Start-complete")
	 * ("ab1","Triage_Request-complete")
	 * ("ab2","29060537")
	 * ("ab3","Nursing Assessment-complete")
	 * ("ab4","29062349")...
	 */
	Map<String, String> charActivityMap;
	
	/*
	 * Mapping from real activity name to encoded name
	 * Example:
	 * ("Arrive_Start-complete","ab0")
	 * ("Triage_Request-complete","ab1")
	 * ("29060537","ab2")
	 * ("Nursing Assessment-complete","ab3")
	 * ("29062349","ab4")...
	 */
	Map<String, String> activityCharMap;
	

	Set<String> attributesInRuleSet;
	float threshold = 0.3f;
	List<String> finalRuleList;
	Map<String, Metrics> finalRuleListMetricsMap;
	Map<String, String> encodedDecodedRuleMap;
	
	String maxOptionsString;
	String maxClass;
	com.company.SignatureDiscovery.types.Feature maxFeature;
	double maxF1Score;
	double precisionPerF1Score;
	double accuracyPerF1Score;
	double recallPerF1Score;
	
	boolean hasSignatures;
	
	boolean featureCreation; //Bruce added to signal this is feature creation or plug-in run
	
	/*
	 * Main class of signature discovery after having event log and all user-defined input parameters
	 */
	public DiscoverSignatures(XLog trainLog, XLog testLog, double min_sup, int coverage_thres, SignatureDiscoveryInput input,
								boolean featureCreation){
		
		//Merge trainLog and testLog into one log
		XLog mergeLog = (XLog) trainLog.clone();
		if (testLog != null && testLog.size() > 0) {
			for (XTrace trace : testLog) {
				mergeLog.add((XTrace)trace.clone());
			}
		}
		
		this.log = mergeLog;
		this.input = input;
		hasSignatures = true;
		this.featureCreation = featureCreation;
		
		System.out.println("Encode activity names");
		encodeLog(); //generate instanceProfileList, encoding is done on merge log to cover both training and testing data
		
		//--------------------------------------------------------
		// Remove testing traces out of instanceProfileList so 
		// that feature mining is only done on the training data
		//--------------------------------------------------------
		Set<String> testTraceIDs = new HashSet<String>();
		if (testLog != null && testLog.size() > 0) {
			for (XTrace trace : testLog) {
				testTraceIDs.add(XConceptExtension.instance().extractName(trace));
			}
		}
		
		List<InstanceProfile> testInstanceProfileList = new ArrayList<InstanceProfile>();
		Iterator<InstanceProfile> iterator = instanceProfileList.iterator();
		InstanceProfile instance;
		while (iterator.hasNext()) {
			instance = iterator.next();
			if (testTraceIDs.contains(instance.getName())) {
				testInstanceProfileList.add(instance);
				iterator.remove();
			}
		}
		
		// FROM THIS POINT FORWARD, all class variables only contain training data
		
		System.out.println("Generate all possible features");
		computeFeatureSets(); // use instanceProfileList (contain only training traces) to generate features
		
		setActualFeatureSet(); 

		filterFeatureSet();

		System.out.println("Transform log into a feature vector table");
		createInstanceVector(); //transform the log into feature space with feature support count computed for each instance
		List<InstanceVector> testInstanceVectorList = this.createInstanceVector(testInstanceProfileList);
		
		/*
		 * Bruce 27.05.2014
		 * Bruce 27.05.2015
		 */		
		if (featureCreation) {
			
			//JOptionPane.showMessageDialog(new JFrame(), "Please check DB parameters in properties.xml file and MS Access tables are ready!");
			
			try {
				Map.Entry<Feature, List<InstanceVector>> entry = featureInstanceVectorListMap.entrySet().iterator().next();
				List<InstanceVector> trainInstanceVectorList = entry.getValue();
				
				//createAttributeMappingTable();
				System.out.println("Write activity mapping to mapping.txt");
				writeActivityMappingToFile(System.getProperty("user.dir") + "\\mapping.txt");
				
				System.out.println("Write encoded traces to training_testingTraceEncoding.txt");
				writeTraceEncodingToFile(trainInstanceVectorList, System.getProperty("user.dir") + "\\trainingTraceEncoding.txt");
				if (testInstanceVectorList.size() > 0) {
					writeTraceEncodingToFile(testInstanceVectorList, System.getProperty("user.dir") + "\\testingTraceEncoding.txt");
				}
				
				System.out.println("Filter features by support level");
				filterFeatureSetBySupportLevel(min_sup); //requires instance vector list to calculate support
				 
				//createFeatureSupportTable();
				System.out.println("Create feature support count in pattrain.txt and pattest.txt");
				writeFeaturesToFile(trainInstanceVectorList, System.getProperty("user.dir") + "\\pattrain.txt");
				if (testInstanceVectorList.size() > 0) {
					writeFeaturesToFile(testInstanceVectorList, System.getProperty("user.dir") + "\\pattest.txt");
				}
			
				System.out.println("Select features based on Fisher score");
				filterFeatureSetByFeatureSelection(coverage_thres); //requires instance vector list to calculate support and Fisher score
				
				//createFeatureSelectionTable();
				System.out.println("Write feature selection to features.txt");
				writeFeatureSelectionToFile(System.getProperty("user.dir") + "\\features.txt");
				
				//createInstanceVectorTable2();		
				System.out.println("Write case tables for classification to crosstrain.txt and crosstest.txt");
				
				Map<String, Map<String, Integer>> indiEventMap = computeIndividualEventCountMap(trainLog);
				writeInstanceVectorToFile(trainInstanceVectorList, indiEventMap, System.getProperty("user.dir") + "\\crosstrain.csv");
				
				if (testInstanceVectorList.size() > 0) {
					indiEventMap = computeIndividualEventCountMap(testLog);
					writeInstanceVectorToFile(testInstanceVectorList, indiEventMap, System.getProperty("user.dir") + "\\crosstest.csv");
				}
				
				System.out.println("FINISHED!");	
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}				
			
		} else {
			prepareWekaData();
			findSignatures();
		}

	}
	
	private Map<String, Map<String, Integer>> computeIndividualEventCountMap(XLog log) {
		List<String> eventNameList = new ArrayList<String>(); //list of distinct event names
		//Each list item is a trace. Key: traceID, value: key is event name, value: frequency
		Map<String, Map<String, Integer>> eventCountMap = new HashMap<String, Map<String, Integer>>(); 	
		
	    for (XTrace trace : log) {
	    	String traceId = LogUtilites.getConceptName(trace); //TraceID
	    	
	    	//Important to use TreeMap to ensure all traces will return similar event name order when retrieving
	    	//for writing to a data file. See writeInstanceVectorToFile method.
	    	Map<String, Integer> traceEventCountMap = new TreeMap<String, Integer>(); //for the current trace
	    	
	    	for (XEvent event : trace) {
	    		String eventName = LogUtilites.getConceptName(event); //Event Name
	    		
	    		if (!eventNameList.contains(eventName)) eventNameList.add(eventName);
	    		
	    		if (!traceEventCountMap.containsKey(eventName)) {
	    			traceEventCountMap.put(eventName, 1);
	    		}
	    		else {
	    			traceEventCountMap.put(eventName, traceEventCountMap.get(eventName) + 1);
	    		}
	    	}
	    	eventCountMap.put(traceId, traceEventCountMap);
	    }	
	    
	    // Add other event names not present in a trace to eventCountMap with zero frequency
	    for (Map<String, Integer> map : eventCountMap.values()) {
	    	for (String eventName : eventNameList) {
	    		if (!map.containsKey(eventName)) {
	    			map.put(eventName, 0);
	    		}
	    	}
	    }
	    
	    return eventCountMap;
	}
	
	/*
	 * Bruce 27 May 2014
	 * Write attribute name - activity name mapping to table
	 * Input: activityCharMap
	 * Database table: SIGNATURE_DISCOVERY_ATTR_MAPPING
	 * Example
	 * Arrive_Start			ab0
	 * Triage_Request		ab1
	 * Triage_Start			ab2
	 * Nursing Assessment	ab3
	 * RN Assign_Request	ab4
	 * SOCPathology			ab5
	 * SOCECG				ab6
	 * 26.05.2015: change the table name to SIGNATURE_DISCOVERY_ATTR_MAPPING_MR/TR/MRA...
	 */
	private void createAttributeMappingTable() {
		String table;
		String sql;

		ConnectionManager.initParametersFromFile();
		
		for (Feature feature : featureInstanceVectorListMap.keySet()) {
			
			table 	= "SIGNATURE_DISCOVERY_" + feature.toString() + "_MAPPING";
			
	        /*
	         * Drop table first
	         */
	    	sql = "DROP TABLE " + table;
	    	System.out.println(sql);
	        try {
	            ConnectionManager.executeStatement(sql);
	        } catch (Exception e) {
	//        	e.printStackTrace();
	//        	ConnectionManager.close();
	//        	return;
	        }		
			
			/*
			 * Create table
			 */
	        sql 	= "CREATE TABLE " + table + " ";
	        sql 	+= "(";
	        sql 	+= "Code VARCHAR,";
	        sql 	+= "Activity VARCHAR";
	        sql 	+= ")";
	        
	        System.out.println(sql);
	        
	        try {
	            ConnectionManager.executeStatement(sql);
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	ConnectionManager.close();
	        	return;
	        }
	        
	        /*
	         * Insert data into table
	         */
	        System.out.println("INSERT INTO " + table);
	        
	        for (String activityName : activityCharMap.keySet()) {
	        	// only take activity name, not traceID
	        	if (activityName.contains("-")) {
		            sql = "INSERT INTO " + table + " VALUES('" + activityCharMap.get(activityName).trim() + "','" + activityName.trim();
		            sql += "')";
	//	            System.out.println(sql);
		            try {
		                ConnectionManager.executeStatement(sql);
		            } catch (Exception e) {
		            	e.printStackTrace();
		            }	            
	        	}  
	        }
		}
        
        ConnectionManager.close();
	}
	
	private void writeActivityMappingToFile(String targetFile) throws IOException {
		List<String[]> activityMappingList = new ArrayList<String[]>();
		String[] activityMapping;
        for (String activityName : activityCharMap.keySet()) {
        	activityMapping = new String[2];
        	// only take activity name, not traceID
        	if (activityName.contains("-")) {
        		activityMapping[0] = activityCharMap.get(activityName).trim();
        		activityMapping[1] = activityName.trim();
        		activityMappingList.add(activityMapping);
        	}  
        }	
		
        ICsvListWriter listWriter = null;
        try {
        	CsvPreference pref = (new CsvPreference.Builder('\"', ';', "\n")).build(); // delimiter is a semicolon
            listWriter = new CsvListWriter(new FileWriter(targetFile), pref);
            final String[] header = new String[] { "activityCode", "activityName"};
            
            // write the header
            listWriter.writeHeader(header);
            
            // write the customer lists
            for (String[] mapping : activityMappingList) {
            	listWriter.write(mapping);
            }
                
        }
        finally {
            if( listWriter != null ) {
            	listWriter.close();
            }
        }		
	}
	
	private CellProcessor[] getProcessors() {
        final CellProcessor[] processors = new CellProcessor[] { 
            new UniqueHashCode(), // activity code
            new NotNull(), // activity name
        };
        
        return processors;
	}
	
	/*
	 * Bruce 27 May 2014
	 * Write instanceVector to database
	 * Precondition: instance vector has been created
	 * Input 1: featureInstanceVectorListMap contains all instance vectors 
	 * Input 2: featureAttributeNameListMap contains all feature names (encoded)
	 * Input 3: filteredFeatureSelectionMap: has been created from feature selection
	 * Database connection: file properties.xml contains database connection string
	 * Database table name to write to: SIGNATURE_DISCOVERY_<feature_type>_INSTANCE_VECTOR
	 * Each row of table is one instance, each field is one feature, the first field is Case ID
	 * Example:
	 * Case_ID		ab0ab1	cd0cd1	de0de1de2	
	 * 0000001		2		3		0	
	 * 0000002		0		3		0	
	 * 0000003		2		3		4	
	 * 0000004		2		0		0	
	 * 0000005		2		3		0	
	 */
	private void createInstanceVectorTable() {
		String table, sql, attributeName;
		List<InstanceVector> instanceVectorList;
		List<String> featureAttributeNameList;
		boolean hasFeatures=true;
		int[] numericVector;	
		String msg;
		
        ConnectionManager.initParametersFromFile();
        
        for (Feature feature : featureInstanceVectorListMap.keySet()) {
        	
    		/* 
        	 * Check no features found or too many features
        	 * Skip database processing if no features or too many
        	 */
        	featureAttributeNameList = featureAttributeNameListMap.get(feature);
        	if (featureAttributeNameList != null) {
        		if (featureAttributeNameList.size() == 0) {
        			JOptionPane.showMessageDialog(new JFrame(),"Feature " + feature.toString() + ": No features found!");
        			System.out.println("Feature " + feature.toString() + ": No features found!");
        			hasFeatures = false;
        		}
        		else if (featureAttributeNameList.size() > 250) {
        			msg = "There are " + featureAttributeNameList.size() +
					" " + feature.toString() + " features, exceeding limit of 250 database fields.";
        			JOptionPane.showMessageDialog(new JFrame(), msg);
        			System.out.println(msg);
        			hasFeatures = false;
        		}
        	} 
        	else {
        		JOptionPane.showMessageDialog(new JFrame(),"Feature " + feature.toString() + ": No features found!");
        		System.out.println("Feature " + feature.toString() + ": No features found!");
        		hasFeatures = false;
        	}
        	
        	if (!hasFeatures) continue;
        	
        	

        	table = "SIGNATURE_DISCOVERY_" + feature.toString() + "_INSTANCE_VECTOR";
        	
            /*
             * Drop table first
             */
        	sql = "DROP TABLE " + table;
        	System.out.println(sql);
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
//            	e.printStackTrace();
//            	ConnectionManager.close();
//            	return;
            } 
            
        	/*
        	 * Create table
        	 */
            featureAttributeNameList = featureAttributeNameListMap.get(feature);
            
        	sql = "CREATE TABLE " + table + " ";
        	sql += "(";
        	sql += "Case_ID VARCHAR, TraceEncoding LONGTEXT, Label VARCHAR,";
        	
        	for (int i=0; i<=featureAttributeNameList.size()-1;i++) {
        		attributeName = featureAttributeNameList.get(i);
        		
        		/*
        		 * In case of alphabet feature, the attribute name is converted 
        		 * from set of string, which would have format, e.g.: "[ab0, ab1, ab2]"
        		 * Need to remove special characters for valid SQL statement -> "ab0,ab1,ab2"
        		 */
        		attributeName = attributeName.replace("[", "");
        		attributeName = attributeName.replace("]", "");
        		attributeName = attributeName.replace(",", "");
        		attributeName = attributeName.replace(" ", "");
        		
//        		System.out.println("Pattern: " + attributeName);
        		
        		/*
        		 * Process weird ".." pattern name 
        		 * due to some error of Signature Discovery plug-in 
        		 * in suffixTree.getMaximalRepeats();
        		 */
        		if (!attributeName.contains("..")) {
	        		sql += attributeName.trim() + " NUMBER";
	        		if (i < featureAttributeNameList.size()-1) {
	        			sql += ",";
	        		}
	        		else {
	        			sql += ")";
	        		}
        		}
        	}
        	
        	System.out.println(sql);
	            
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
            	e.printStackTrace();
            	ConnectionManager.close();
            	return;

            }
            
            /*
             * Insert data into table
             */
            System.out.println("INSERT INTO " + table);
            
            instanceVectorList = featureInstanceVectorListMap.get(feature);
             
            for (InstanceVector instanceVector : instanceVectorList) {
            	
            	System.out.println("TraceID " + instanceVector.getName() + " " + instanceVector.getEncodedTrace());
            	
	        	sql = "INSERT INTO " + table + " VALUES(";
	        	sql += "'" + instanceVector.getName().trim() + "',";
	        	sql += "'" + instanceVector.getEncodedTrace().trim() + "',";
	        	sql += "'" + instanceVector.getLabel().trim() + "',";
	        	
	        	numericVector = instanceVector.getStandardizedNumericVector();
	        	for (int i=0; i <= numericVector.length-1;i++) {
	        		sql += numericVector[i];
	        		if (i < numericVector.length-1) {
	        			sql += ",";
	        		}
	        		else {
	        			sql += ")";
	        		}
	        	}
	        	
//	        	System.out.println(sql);
	            
	            try {
	                ConnectionManager.executeStatement(sql);
	            } catch (Exception e) {
	            	e.printStackTrace();
	            } 
            }
            
            
            
        }
        
        ConnectionManager.close();
        	
	}
	
	/**
	 * @throws IOException 
	 * @input: filterFeatureSelectionMap
	 * @input: featureInstanceVectorListMap
	 * @return: table created with the following fields
	 * Case_ID
	 * EncodedTrace
	 * Label
	 * and all features after filtering by support level and fisher score
	 */
	private void createInstanceVectorTable2() throws IOException {
		String table, sql;
		List<InstanceVector> instanceVectorList;
		Map<String, Double> featureScoreMap; //feature -> score
		boolean hasFeatures=true;
		String msg;
		
        for (Feature featureType : filterFeatureSelectionMap.keySet()) {
    		/* 
        	 * Check no features found or too many features
        	 * Skip database processing if no features or too many
        	 */
        	featureScoreMap = filterFeatureSelectionMap.get(featureType);
        	if (featureScoreMap != null) {
        		if (featureScoreMap.size() == 0) {
        			JOptionPane.showMessageDialog(new JFrame(),"Feature " + featureType.toString() + ": No features found!");
        			System.out.println("Feature " + featureType.toString() + ": No features found!");
        			hasFeatures = false;
        		}
        		else if (featureScoreMap.size() > 250) {
        			msg = "There are " + featureScoreMap.size() +
					" " + featureType.toString() + " features, exceeding limit of 250 database fields.";
        			JOptionPane.showMessageDialog(new JFrame(), msg);
        			System.out.println(msg);
        			hasFeatures = false;
        		}
        	} 
        	else {
        		JOptionPane.showMessageDialog(new JFrame(),"Feature " + featureType.toString() + ": No features found!");
        		System.out.println("Feature " + featureType.toString() + ": No features found!");
        		hasFeatures = false;
        	}
        	if (!hasFeatures) continue;
        	
        	ConnectionManager.initParametersFromFile();
        	table = "SIGNATURE_DISCOVERY_" + featureType.toString() + "_INSTANCE_VECTOR";
        	
            /***************************
             * Drop table first
             ***************************/
        	sql = "DROP TABLE " + table;
        	System.out.println(sql);
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
//            	e.printStackTrace();
//            	ConnectionManager.close();
//            	return;
            } 
            
        	/**************************
        	 * Create table
        	 **************************/
            Set<String> featureSet = featureScoreMap.keySet();
            
        	sql = "CREATE TABLE " + table + " ";
        	sql += "(";
        	sql += "Case_ID VARCHAR, TraceEncoding LONGTEXT, Label VARCHAR,";
        	
        	int i=0;
        	for (String featureName : featureSet) {
        		i++;
        		sql += featureName.trim() + " NUMBER";
        		if (i < featureSet.size()) {
        			sql += ",";
        		} else {
        			sql += ")";
        		}
        	}
        	
        	System.out.println(sql);
	            
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
            	e.printStackTrace();
            	ConnectionManager.close();
            	return;
            }
            
            /*****************************
             * Insert data into table
             *****************************/
            System.out.println("INSERT INTO " + table);
            
            instanceVectorList = featureInstanceVectorListMap.get(featureType);
            Map<String,Integer> instanceFeatureCountMap = new HashMap<String,Integer>(); 
            for (InstanceVector instance : instanceVectorList) {
            	System.out.println("TraceID " + instance.getName() + " " + instance.getEncodedTrace());
	        	sql = "INSERT INTO " + table + " VALUES(";
	        	sql += "'" + instance.getName().trim() + "',";
	        	sql += "'" + instance.getEncodedTrace().trim() + "',";
	        	sql += "'" + instance.getLabel().trim() + "',";
	        	
	        	if (input.selectedFeatureSet.contains(
                Feature.MR) || input.selectedFeatureSet.contains(
                Feature.TR)) {
	        		instanceFeatureCountMap = instance.getSequenceFeatureCountMap();
	        	}
	        	else if (input.selectedFeatureSet.contains(
                Feature.MRA) || input.selectedFeatureSet.contains(
                Feature.TRA)) {
					//Need to convert the set-based key to string-based key
	        		instanceFeatureCountMap.clear();
					for (Set<String> setFeature : instance.getAlphabetFeatureCountMap().keySet()) {
						instanceFeatureCountMap.put(this.extractStringFromAlphabetFeature(setFeature), instance.getAlphabetFeatureCountMap().get(setFeature));
					}	        		
	        	}
	        	else {
	        		throw new IOException("Wrong feature selection by users!");
	        	}

	        	int featureInstanceCount; 
	        	int k = 0;
	        	for (String featureName : featureSet) {
	        		k++;
	        		featureInstanceCount = 0;
	        		if (instanceFeatureCountMap.containsKey(featureName)) {
	        			featureInstanceCount = instanceFeatureCountMap.get(featureName);
	        		}
	        		sql += featureInstanceCount;
	        		if (k < featureSet.size()) {
	        			sql += ",";
	        		}
	        		else {
	        			sql += ")";
	        		}
	        	}
//	        	System.out.println(sql);
	            
	            try {
	                ConnectionManager.executeStatement(sql);
	            } catch (Exception e) {
	            	e.printStackTrace();
	            } 
            }
        }
        
        ConnectionManager.close();
        	
	}	
	
	
	/**
	 * Write instance vector representation to file, used for both testing and training data
	 * @param instanceVectorList: contain features for every trace with support count
	 * @param indiEventMap: contain the individual events for every trace with support count, key: traceID, value: key: eventname, value: support count
	 * @throws IOException
	 */
	public void writeInstanceVectorToFile(List<InstanceVector> instanceVectorList, Map<String, Map<String, Integer>> indiEventMap, String targetFile) throws IOException {
		
		//------------------------------------------
		// Prepare List
		//------------------------------------------	
		List<ArrayList<String>> instanceInfoList = new ArrayList<ArrayList<String>>();
		ArrayList<String> instanceInfo = null;	
			
		Map.Entry<Feature, Map<String, Double>> entry2 = filterFeatureSelectionMap.entrySet().iterator().next();
		Map<String, Double> featureMap = entry2.getValue();
		
		//------------------------------------------------
		// Build the header: <Case_ID> <Label> <every feature name is a header column>
		//------------------------------------------------
        List<String> header = new ArrayList<String>();
        header.add("Case_ID");
        header.add("Label");
        
        // Add individual event name
        if (!indiEventMap.isEmpty()) {
        	Set<String> eventNameList = indiEventMap.values().iterator().next().keySet();
        	for (String eventName : eventNameList) {
        		header.add(eventName);
        	}
        }
        
        // Add individual feature name
        for (String featureName : featureMap.keySet()) {
        	header.add(featureName);
        }
		
        //------------------------------------------------
        // Build the instance info list
        //------------------------------------------------
        Map<String,Integer> instanceFeatureCountMap = new HashMap<String,Integer>();
		for (InstanceVector instance : instanceVectorList) {
        	if (input.selectedFeatureSet.contains(
              Feature.MR) || input.selectedFeatureSet.contains(
              Feature.TR)) {
        		instanceFeatureCountMap = instance.getSequenceFeatureCountMap();
        	}
        	else if (input.selectedFeatureSet.contains(
              Feature.MRA) || input.selectedFeatureSet.contains(
              Feature.TRA)) {
				//Need to convert the set-based key to string-based key
        		instanceFeatureCountMap.clear();
				for (Set<String> setFeature : instance.getAlphabetFeatureCountMap().keySet()) {
					instanceFeatureCountMap.put(extractStringFromAlphabetFeature(setFeature), instance.getAlphabetFeatureCountMap().get(setFeature));
				}	        		
        	}
        	else {
        		throw new IOException("Wrong feature selection by users!");
        	}			
        	
        	instanceInfo = new ArrayList<String>();
        	instanceInfo.add(instance.getName()); // Case_ID
        	instanceInfo.add(instance.getLabel()); // The case label
        	
        	// Add frequency count for individual event
        	Map<String, Integer> traceEventMap = indiEventMap.get(instance.getName()); 
        	for (String eventName : traceEventMap.keySet()) { //this is TreeMap, so event names are always returned in the same sorting order
        		instanceInfo.add(String.valueOf(traceEventMap.get(eventName)));
        	}
        	
        	// Add frequency count in the instance for every feature
        	int featureInstanceCount;
			for (String featureName : featureMap.keySet()) {
        		featureInstanceCount = 0;
        		if (instanceFeatureCountMap.containsKey(featureName)) {
        			featureInstanceCount = instanceFeatureCountMap.get(featureName);
        		}
        		instanceInfo.add(String.valueOf(featureInstanceCount));				
			}
			
			instanceInfoList.add(instanceInfo);
		}
		
	
		
		//------------------------------------------
		// Write list to file
		//------------------------------------------
        ICsvListWriter listWriter = null;
        try {
        	CsvPreference pref = (new CsvPreference.Builder('\"', ';', "\n")).build(); // delimiter is a semicolon
            listWriter = new CsvListWriter(new FileWriter(targetFile), pref);
            
            // write the header
            String[] headerArray = new String[header.size()];
            headerArray = header.toArray(headerArray);
            listWriter.writeHeader(headerArray);
            
            // write the customer lists
            for (List<String> info : instanceInfoList) {
            	listWriter.write(info);
            }
                
        }
        finally {
            if( listWriter != null ) {
            	listWriter.close();
            }
        }		
	}	
	
	/*
	 * Bruce 18.05.2014
	 * Create table mapping from feature to activity with the support information
	 * Three fields
	 * Feature: VARCHAR
	 * ActivityTrace: LONGTEXT
	 * Support: NUMBER
	 */
	private void createFeatureSupportTable() {
		String table, sql;
		
        ConnectionManager.initParametersFromFile();
        
        /*********************************
         * FOR SEQUENCE FEATURES
         *********************************/
        for (Feature feature : filteredActualFeatureSequenceFeatureSetMap.keySet()) {
        	
        	table = "SIGNATURE_DISCOVERY_" + feature.toString() + "_FEATURE_SUPPORT";
        	
            /*
             * Drop table first
             */
        	sql = "DROP TABLE " + table;
        	System.out.println(sql);
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
//            	e.printStackTrace();
//            	ConnectionManager.close();
//            	return;
            } 
            
    		/*
    		 * Create table
    		 */
            sql 	= "CREATE TABLE " + table + " ";
            sql 	+= "(";
            sql 	+= "Feature VARCHAR,";
            sql 	+= "ActivityTrace LONGTEXT,";
            sql 	+= "Support NUMBER";
            sql 	+= ")";
            
            System.out.println(sql);
            
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
            	e.printStackTrace();
            	ConnectionManager.close();
            	return;
            }
            
            /*
             * Insert data into table
             * For sequence feature
             */
            System.out.println("INSERT INTO " + table);
            Set<String> featureSet;
            featureSet = filteredActualFeatureSequenceFeatureSetMap.get(feature);
            Map <String,Integer> featureCountMap = computeTotalSequenceFeatureSupportCount(featureInstanceVectorListMap.get(feature));            
            String activityTrace; 
            System.out.println(featureCountMap.toString());
            for (String pattern : featureSet) {
            	activityTrace = getRealActivityTrace(pattern);
            	
	        	sql = "INSERT INTO " + table + " VALUES(";
	        	sql += "'" + pattern.trim() + "',";
	        	sql += "'" + activityTrace.trim() + "',";
	        	sql += "" + 1.0*featureCountMap.get(pattern)/featureInstanceVectorListMap.get(feature).size() + ")";
	        	
//	        	System.out.println(sql);
	            
	            try {
	                ConnectionManager.executeStatement(sql);
	            } catch (Exception e) {
	            	e.printStackTrace();
	            } 
            } 
        }
        
        /************************************************
         * FOR ALPHABET FEATURES 
         * Note that an alphabet feature is in form of set of encoded activity names
         * rather than a string of encoded activity names like sequence features
         ***********************************************/        
        
        for (Feature feature : filteredActualFeatureAlphabetFeatureSetMap.keySet()) {
        	
			table = "SIGNATURE_DISCOVERY_" + feature.toString() + "_FEATURE_SUPPORT";
        	
            /*
             * Drop table first
             */
        	sql = "DROP TABLE " + table;
        	System.out.println(sql);
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
//            	e.printStackTrace();
//            	ConnectionManager.close();
//            	return;
            } 
            
    		/*
    		 * Create table
    		 */
            sql 	= "CREATE TABLE " + table + " ";
            sql 	+= "(";
            sql 	+= "Feature VARCHAR,";
            sql 	+= "EquivalenceClasses VARCHAR,";            
            sql 	+= "ActivityTrace LONGTEXT,";
            sql 	+= "Support NUMBER";
            sql 	+= ")";
            
            System.out.println(sql);
            
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
            	e.printStackTrace();
            	ConnectionManager.close();
            	return;
            }
            
            /*
             * Insert data into table
             */
            System.out.println("INSERT INTO " + table);
            Map<Set<String>, Set<String>> alphabetFeatureSet;
            alphabetFeatureSet = filteredActualFeatureAlphabetFeatureSetMap.get(feature);
            Map <Set<String>,Integer> featureCountMap = computeTotalAlphabetFeatureSupportCount(featureInstanceVectorListMap.get(feature));
            String activityTrace; 
            String patternString;
            for (Set<String> pattern : alphabetFeatureSet.keySet()) {
        		/*
        		 * In case of alphabet feature, the attribute name is converted 
        		 * from set of string, which would have format, e.g.: "[ab0],[ab1],[ab2]"
        		 * Need to remove special characters -> "ab0ab1ab2"
        		 */
            	patternString = this.extractStringFromAlphabetFeature(pattern); // now "ab0ab1ab2"
            	activityTrace = getRealActivityTrace(patternString);
            	
	        	sql = "INSERT INTO " + table + " VALUES(";
	        	sql += "'" + patternString.trim() + "',";
	        	sql += "'" + alphabetFeatureSet.get(pattern).toString() + "',";  //equivalent classes
	        	sql += "'" + activityTrace.trim() + "',";
	        	sql += "" + 1.0*featureCountMap.get(pattern)/featureInstanceVectorListMap.get(feature).size() + ")";
		        	
//		        System.out.println(sql);
	            
	            try {
	                ConnectionManager.executeStatement(sql);
	            } catch (Exception e) {
	            	e.printStackTrace();
	            } 
            	
            } 
        }        
        
       ConnectionManager.close();
        			
	}
	
	private void writeFeaturesToFile(List<InstanceVector> instanceVectorList, String targetFile) throws IOException {
		Object[] featureTypes = null;
		if (filteredActualFeatureSequenceFeatureSetMap.size() > 0) {
			featureTypes = filteredActualFeatureSequenceFeatureSetMap.keySet().toArray();
		}
		else if (filteredActualFeatureAlphabetFeatureSetMap.size() > 0) {
			featureTypes = filteredActualFeatureAlphabetFeatureSetMap.keySet().toArray();
		}
		Feature featureType = (Feature)(featureTypes[0]);
		
		//------------------------------------------
		// Prepare List
		//------------------------------------------		
		List<ArrayList<String>> featureInfoList = new ArrayList<ArrayList<String>>();
		ArrayList<String> featureInfo = null;		
		if (featureType.equals(Feature.MR) || featureType.equals(
        Feature.TR)) {
			Set<String> featureSet = filteredActualFeatureSequenceFeatureSetMap.get(featureType);
			Map<String, Integer> featureCountMap;
	        for (String feature : featureSet) { // for every feature (1 line): <Name> <Count> <Support> (1:3) (2:5) (5:1)...
	        	featureInfo = new ArrayList<String>();
	        	featureInfo.add(feature.trim()); // name
	        	Integer supCount = 0;
	        	Integer totalCount = 0;
	        	int counter = 1;
	            for (InstanceVector instanceVector : instanceVectorList) {
	            	featureCountMap = instanceVector.getSequenceFeatureCountMap();
	            	if (featureCountMap.containsKey(feature)) {
		            	supCount = featureCountMap.get(feature);
		            	totalCount += supCount;
	            	} else {
	            		supCount = 0;
	            	}
					if (supCount > 0) { // sparse format: zero frequency not need to show
						featureInfo.add("(" + counter + "," + String.valueOf(Double.valueOf(supCount).intValue()) + ")");
					}
					counter++;            	
	            }
	            featureInfo.add(1, String.valueOf(Double.valueOf(totalCount).intValue())); //insert count after the feature name
	            featureInfo.add(2, String.valueOf(1.0*totalCount/instanceVectorList.size())); // insert support after the feature count
	            featureInfoList.add(featureInfo);	            
	        }			
		}
		else if (featureType.equals(Feature.MRA) || featureType.equals(
        Feature.TRA)) {
			// Every feature consists of a <feature set> and a set of equivalence classes
			Map<Set<String>, Set<String>> featureSet = filteredActualFeatureAlphabetFeatureSetMap.get(featureType);
			Map<Set<String>, Integer> featureCountMap;
	        for (Set<String> feature : featureSet.keySet()) { // for every feature (1 line): <Name> <EquivalenceClasses> <Count> <Support> (1:3) (2:5) (5:1)...
	        	featureInfo = new ArrayList<String>();
	        	featureInfo.add(extractStringFromAlphabetFeature(feature)); //name
	        	featureInfo.add(featureSet.get(feature).toString()); //equivalence classes
	        	Integer supCount = 0;
	        	Integer totalCount = 0;
	        	int counter = 1;
	            for (InstanceVector instanceVector : instanceVectorList) {
	            	featureCountMap = instanceVector.getAlphabetFeatureCountMap();
	            	if (featureCountMap.containsKey(feature)) {
		            	supCount = featureCountMap.get(feature);
		            	totalCount += supCount;
	            	} else {
	            		supCount = 0;
	            	}
					if (supCount > 0) { // sparse format: zero frequency not need to show
						featureInfo.add("(" + counter + "," + String.valueOf(Double.valueOf(supCount).intValue()) + ")");
					}
					counter++;            	
	            }
	            featureInfo.add(2, String.valueOf(Double.valueOf(totalCount).intValue())); //insert count after the feature name
	            featureInfo.add(3, String.valueOf(1.0*totalCount/instanceVectorList.size())); // insert support after the feature count
	            featureInfoList.add(featureInfo);	            
	        }			
		}
        
		//------------------------------------------
		// Write list to file
		//------------------------------------------	        
        ICsvListWriter listWriter = null;
        try {
        	CsvPreference pref = (new CsvPreference.Builder('\"', ' ', "\n")).build(); // delimiter is a space
            listWriter = new CsvListWriter(new FileWriter(targetFile), pref);
            
            for (List<String> info : featureInfoList) {
            	listWriter.write(info);
            }
                
        }
        finally {
            if( listWriter != null ) {
            	listWriter.close();
            }
        }		
	}
	
	private void writeTraceEncodingToFile(List<InstanceVector> instanceVectorList, String targetFile) throws IOException {
		
		//------------------------------------------
		// Prepare List
		//------------------------------------------			
		List<ArrayList<String>> traceInfoList = new ArrayList<ArrayList<String>>();
		ArrayList<String> traceInfo = null;			
		for (InstanceVector instance : instanceVectorList) {
			traceInfo = new ArrayList<String>();
			traceInfo.add(instance.getName()); // case ID
			traceInfo.add(instance.getLabel()); // trace label
			traceInfo.add(instance.getEncodedTrace()); // encoded trace
			traceInfoList.add(traceInfo);
		}
		
		//------------------------------------------
		// Write list to file
		//------------------------------------------	        
        ICsvListWriter listWriter = null;
        try {
        	CsvPreference pref = (new CsvPreference.Builder('\"', ' ', "\n")).build(); // delimiter is a space
            listWriter = new CsvListWriter(new FileWriter(targetFile), pref);
            
            for (List<String> info : traceInfoList) {
            	listWriter.write(info);
            }
                
        }
        finally {
            if( listWriter != null ) {
            	listWriter.close();
            }
        }		
	}
	
	/*
	 * Bruce 27.05.2015
	 * Create table storing the result of feature selection based on Fisher score
	 * @Input: filterFeatureSelectionMap contains feature and fisher score 
	 * Two fields
	 * Feature: VARCHAR
	 * FisherScore: NUMBER
	 */
	private void createFeatureSelectionTable() throws NumberFormatException, FileNotFoundException, IOException {
		String table, sql;
		
        ConnectionManager.initParametersFromFile();
        
        /*********************************
         * FOR SEQUENCE FEATURES
         *********************************/
        for (Feature feature : filterFeatureSelectionMap.keySet()) {
        	
        	table = "SIGNATURE_DISCOVERY_" + feature.toString() + "_FEATURE_SELECTION";
        	
            /*
             * Drop table first
             */
        	sql = "DROP TABLE " + table;
        	System.out.println(sql);
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
//            	e.printStackTrace();
//            	ConnectionManager.close();
//            	return;
            } 
            
    		/*
    		 * Create table
    		 */
            sql 	= "CREATE TABLE " + table + " ";
            sql 	+= "(";
            sql 	+= "Feature VARCHAR,";
            sql 	+= "FisherScore NUMBER";
            sql 	+= ")";
            
            System.out.println(sql);
            
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
            	e.printStackTrace();
            	ConnectionManager.close();
            	return;
            }
            
            /*
             * Insert data into table
             */
            System.out.println("INSERT INTO " + table);
            Map<String, Double> featureSet;
            featureSet = filterFeatureSelectionMap.get(feature);
            for (String pattern : featureSet.keySet()) {
	        	sql = "INSERT INTO " + table + " VALUES(";
	        	sql += "'" + pattern.trim() + "',";
	        	sql += "" + 1.0*featureSet.get(pattern) + ")";
//	        	System.out.println(sql);
	            try {
	                ConnectionManager.executeStatement(sql);
	            } catch (Exception e) {
	            	e.printStackTrace();
	            } 
            } 
        }
        
        /******************************************************
         * FOR ALPHABET FEATURES 
         ******************************************************/        
        
        for (Feature feature : filteredActualFeatureAlphabetFeatureSetMap.keySet()) {
			table = "SIGNATURE_DISCOVERY_" + feature.toString() + "_FEATURE_SELECTION";
        	
            /*
             * Drop table first
             */
        	sql = "DROP TABLE " + table;
        	System.out.println(sql);
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
//            	e.printStackTrace();
//            	ConnectionManager.close();
//            	return;
            } 
            
    		/*
    		 * Create table
    		 */
            sql 	= "CREATE TABLE " + table + " ";
            sql 	+= "(";
            sql 	+= "Feature VARCHAR,";
            sql 	+= "FisherScore NUMBER";
            sql 	+= ")";
            
            System.out.println(sql);
            
            try {
                ConnectionManager.executeStatement(sql);
            } catch (Exception e) {
            	e.printStackTrace();
            	ConnectionManager.close();
            	return;
            }
            
            /*
             * Insert data into table
             */
            System.out.println("INSERT INTO " + table);
            Map<String, Double> featureSet;
            featureSet = filterFeatureSelectionMap.get(feature);
            for (String pattern : featureSet.keySet()) {
	        	sql = "INSERT INTO " + table + " VALUES(";
	        	sql += "'" + pattern.trim() + "',";
	        	sql += "" + 1.0*featureSet.get(pattern) + ")";
//	        	System.out.println(sql);
	            try {
	                ConnectionManager.executeStatement(sql);
	            } catch (Exception e) {
	            	e.printStackTrace();
	            } 
            } 
        }        
        
       ConnectionManager.close();
        			
	}
	
	private void writeFeatureSelectionToFile(String targetFile) throws IOException {
		if (filterFeatureSelectionMap.size() > 0) {
			
			//------------------------------------------
			// Prepare list
			//------------------------------------------
			Object[] featureCodes = filterFeatureSelectionMap.keySet().toArray();
			Feature featureType = (Feature)featureCodes[0]; //get the first item only since only one pattern at a time
			Map<String, Double> featureSet = filterFeatureSelectionMap.get(featureType);
	        List<String[]> featureScoreList = new ArrayList<String[]>();
	        String[] featureScores;
	        for (String pattern : featureSet.keySet()) {
	        	featureScores = new String[2];
	        	featureScores[0] = pattern.trim();
	        	featureScores[1] = String.valueOf(1.0*featureSet.get(pattern));
	        	featureScoreList.add(featureScores);
	        } 	
	        
			//------------------------------------------
			// Write list to file
			//------------------------------------------	        
	        ICsvListWriter listWriter = null;
	        try {
	        	CsvPreference pref = (new CsvPreference.Builder('\"', ';', "\n")).build(); // delimiter is a semicolon
	            listWriter = new CsvListWriter(new FileWriter(targetFile), pref);
	            final String[] header = new String[] { "Feature", "FisherScore"};
	            
	            // write the header
	            listWriter.writeHeader(header);
	            
	            // write the customer lists
	            for (String[] featureScore : featureScoreList) {
	            	listWriter.write(featureScore);
	            }
	                
	        }
	        finally {
	            if( listWriter != null ) {
	            	listWriter.close();
	            }
	        }	        
		}
		
	}
	
	/*
	 * Bruce 28.05.2014
	 */
	private String getRealActivityTrace(String encodedFeature) {
		
		String activityTrace="";
		
		int encodedTraceLength = encodedFeature.length()/encodingLength;
		String encodedActivity;
		for(int i = 0; i < encodedTraceLength; i++){
			encodedActivity = encodedFeature.substring(i*encodingLength, (i+1)*encodingLength);
			activityTrace += charActivityMap.get(encodedActivity);
			if (i < encodedTraceLength-1) {
				activityTrace += ",";
			}
		}
		
		return activityTrace;
	}	
	
	/**
	 * This method encodes the given log into character streams
	 */
	private void encodeLog(){
		/*
		 * activitySet accumulates the set of distinct
		 * activities/events in the event log; it doesn't store the trace
		 * identifier for encoding; Encoding trace identifier is required only
		 * when any of the maximal repeat (alphabet) features is selected
		 */

		Set<String> activitySet = new HashSet<String>();
		XAttributeMap attributeMap;
		Set<String> eventTypeSet = new HashSet<String>();
		
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				attributeMap = event.getAttributes();
				activitySet.add(attributeMap.get("concept:name").toString() + "-"
						+ attributeMap.get("lifecycle:transition").toString());
				eventTypeSet.add(attributeMap.get("lifecycle:transition").toString());
			}
			activitySet.add(trace.getAttributes().get("concept:name").toString()); //used for trace delimiter in computation of repeats
		}

		
		try {
			EncodeActivitySet encodeActivitySet = new EncodeActivitySet(activitySet);
			encodingLength = encodeActivitySet.getEncodingLength();

			activityCharMap = encodeActivitySet.getActivityCharMap();
			charActivityMap = encodeActivitySet.getCharActivityMap();
//			System.out.println("Encoding Length: "+encodingLength);
//			System.out.println("activityCharMap size: "+activityCharMap.size());
			/*
			 * Encode each trace to a charStream
			 */
			EncodeTraces encodeTraces = new EncodeTraces(activityCharMap, log);
			encodedTraceList = encodeTraces.getCharStreamList();
			instanceProfileList = encodeTraces.getInstanceProfileList();
		}catch(ActivityOverFlowException e){
			e.printStackTrace();
		}catch(EncodingNotFoundException e){
			e.printStackTrace();
		}
	}
	
	private void computeFeatureSets(){
		// Get combination Features
		Set<Feature> selectedFeatureSet = input.selectedFeatureSet;
		Set<Feature> expandedSelectedFeatureSet = input.selectedFeatureSet;
		
		if(selectedFeatureSet.contains(Feature.IE)){
			if(selectedFeatureSet.contains(Feature.TR))
				expandedSelectedFeatureSet.add(Feature.IE_TR);
			if(selectedFeatureSet.contains(Feature.MR))
				expandedSelectedFeatureSet.add(Feature.IE_MR);
			if(selectedFeatureSet.contains(Feature.SMR))
				expandedSelectedFeatureSet.add(Feature.IE_SMR);
			if(selectedFeatureSet.contains(Feature.NSMR))
				expandedSelectedFeatureSet.add(Feature.IE_NSMR);
			
			if(selectedFeatureSet.contains(Feature.TRA))
				expandedSelectedFeatureSet.add(Feature.IE_TRA);
			if(selectedFeatureSet.contains(Feature.MRA))
				expandedSelectedFeatureSet.add(Feature.IE_MRA);
			if(selectedFeatureSet.contains(Feature.SMRA))
				expandedSelectedFeatureSet.add(Feature.IE_SMRA);
			if(selectedFeatureSet.contains(Feature.NSMRA))
				expandedSelectedFeatureSet.add(Feature.IE_NSMRA);
		}
		if(selectedFeatureSet.contains(Feature.TR)){
			if(selectedFeatureSet.contains(Feature.MR))
				expandedSelectedFeatureSet.add(Feature.TR_MR);
			if(selectedFeatureSet.contains(Feature.SMR))
				expandedSelectedFeatureSet.add(Feature.TR_SMR);
			if(selectedFeatureSet.contains(Feature.NSMR))
				expandedSelectedFeatureSet.add(Feature.TR_NSMR);
		}
		
		if(selectedFeatureSet.contains(Feature.TRA)){
			if(selectedFeatureSet.contains(Feature.MRA))
				expandedSelectedFeatureSet.add(Feature.TRA_MRA);
			if(selectedFeatureSet.contains(Feature.SMRA))
				expandedSelectedFeatureSet.add(Feature.TRA_SMRA);
			if(selectedFeatureSet.contains(Feature.NSMRA))
				expandedSelectedFeatureSet.add(Feature.TRA_NSMRA);
		}
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(
        Feature.TR) && selectedFeatureSet.contains(
        Feature.MR))
			expandedSelectedFeatureSet.add(Feature.IE_TR_MR);
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(
        Feature.TR) && selectedFeatureSet.contains(
        Feature.SMR))
			expandedSelectedFeatureSet.add(Feature.IE_TR_SMR);
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(
        Feature.TR) && selectedFeatureSet.contains(
        Feature.NSMR))
			expandedSelectedFeatureSet.add(Feature.IE_TR_NSMR);
		
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(
        Feature.TRA) && selectedFeatureSet.contains(
        Feature.MRA))
			expandedSelectedFeatureSet.add(Feature.IE_TRA_MRA);
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(
        Feature.TRA) && selectedFeatureSet.contains(
        Feature.SMRA))
			expandedSelectedFeatureSet.add(Feature.IE_TRA_SMRA);
		
		if(selectedFeatureSet.contains(Feature.IE) && selectedFeatureSet.contains(
        Feature.TRA) && selectedFeatureSet.contains(
        Feature.NSMRA))
			expandedSelectedFeatureSet.add(Feature.IE_TRA_NSMRA);
		
		input.selectedFeatureSet = expandedSelectedFeatureSet;
		featureExtraction = new FeatureExtraction(encodingLength, activityCharMap, charActivityMap, instanceProfileList, expandedSelectedFeatureSet, input.kGramValue);
		featureExtraction.computeNonOverlapFeatureMetrics();
	}

	private void setActualFeatureSet(){
		Logger.printCall("Calling Discover Signatures -> setActualFeatureSet()");
		if(input.isBaseFeatures || input.featureType == FeatureType.Best){
			actualFeatureSequenceFeatureSetMap = featureExtraction.getBaseSequenceFeatureSetMap();
			actualFeatureAlphabetFeatureSetMap = featureExtraction.getBaseAlphabetFeatureSetMap();
			actualFeatureSequenceNOCMap = featureExtraction.getBaseSequenceFeatureNOCMap();
			actualFeatureSequenceInstanceCountPercentageMap = featureExtraction.getBaseSequenceFeatureInstanceCountPercentageMap();
			
			actualFeatureAlphabetNOCMap = featureExtraction.getBaseAlphabetFeatureNOCMap();
			actualFeatureAlphabetInstanceCountPercentageMap = featureExtraction.getBaseAlphabetFeatureInstanceCountPercentageMap();
			
			if(input.featureType == FeatureType.Best){
				actualFeatureAlphabetNOCMap = featureExtraction.getOriginalAlphabetFeatureNOCMap();
				actualFeatureAlphabetInstanceCountPercentageMap = featureExtraction.getOriginalAlphabetFeatureInstanceCountPercentageMap();
			}
		}else if(!input.isBaseFeatures){
			Logger.println("Here !isBaseFeature");
			actualFeatureSequenceFeatureSetMap = featureExtraction.getOriginalSequenceFeatureSetMap();
			actualFeatureSequenceNOCMap = featureExtraction.getOriginalSequenceFeatureNOCMap();
			actualFeatureSequenceInstanceCountPercentageMap = featureExtraction.getOriginalSequenceFeatureInstanceCountPercentageMap();

			actualFeatureAlphabetFeatureSetMap = featureExtraction.getOriginalAlphabetFeatureSetMap();
			actualFeatureAlphabetNOCMap = featureExtraction.getOriginalAlphabetFeatureNOCMap();
			actualFeatureAlphabetInstanceCountPercentageMap = featureExtraction.getOriginalAlphabetFeatureInstanceCountPercentageMap();
		}
		//Logger.printReturn("Returning Discover Signatures -> setActualFeatureSet()");
	}
	
	/*
	 * Bruce 28.05.2014: this function to replace the filterFeatureSet
	 * Precondition: feature sets have been computed and actual feature sets have been set
	 * Particularly for maximal repeats which are extracted on the whole log, thus the total number is very large
	 * Filter features with count figure over 10% of the total count of traces 
	 */
	private void filterFeatureSet2(){
		
		System.out.println("Filtering Feature Set...");
		
		/*
		 * Trace stream is a string concatenating all traces
		 * Delimiter between traces are spaces, number of spaces equal encodingLength
		 * Example: "ab0ab1ab2cd0cd1cd2   ab0ab0ab2ab1cd0cd1cd1cd1   ab0ab1ab2ab2ab1cd1cd2..."
		 */
		StringBuilder traceStream = new StringBuilder();
		for (String trace : encodedTraceList) {
			traceStream.append(trace);
			for (int i=1;i<=encodingLength;i++) {
				traceStream.append(" ");
			}
		}
		
//		System.out.println("All traces encoded and concatenated");
//		System.out.println(traceStream.toString());
		
		/*
		 * Filter sequence features
		 */
		Set<String> featureSet;
		Set<String> filteredFeatureSet = new HashSet<String>();
		Map <String,Integer> featureCountMap;
		
		int traceCount = encodedTraceList.size();
		filteredActualFeatureSequenceFeatureSetMap = new HashMap<Feature, Set<String>>();
		
		for (Feature feature : actualFeatureSequenceFeatureSetMap.keySet()) {
			featureSet = actualFeatureSequenceFeatureSetMap.get(feature);
			featureCountMap = featureExtraction.computeNonOverlapSequenceFeatureCountMap(encodingLength, traceStream.toString(), featureSet);
			for (String pattern : featureCountMap.keySet()) {
				if (featureCountMap.get(pattern)/traceCount >= 0.05) {
					filteredFeatureSet.add(pattern);
				}
			}
			filteredActualFeatureSequenceFeatureSetMap.put(feature, filteredFeatureSet);
			
			System.out.println("Filtered Feature Set: " + feature.toString() + " " + filteredFeatureSet.toString());
		}

		/*
		 * Filter alphabet sequence features
		 */
		Map<Set<String>, Set<String>> alphabetFeatureSet;
		Map<Set<String>, Set<String>> filteredAlphabetFeatureSet = new HashMap<Set<String>, Set<String>>();
		Map<Set<String>, Integer> alphabetFeatureCountMap;
		filteredActualFeatureAlphabetFeatureSetMap = new HashMap<Feature, Map<Set<String>, Set<String>>>();
		
		for (Feature feature : actualFeatureAlphabetFeatureSetMap.keySet()) {
			alphabetFeatureSet = actualFeatureAlphabetFeatureSetMap.get(feature);
			alphabetFeatureCountMap = featureExtraction.computeNonOverlapAlphabetFeatureCountMap(encodingLength, traceStream.toString(), alphabetFeatureSet);
			for (Set<String> pattern : alphabetFeatureCountMap.keySet()) {
				if (alphabetFeatureCountMap.get(pattern)/traceCount >= 0.05) {
					filteredAlphabetFeatureSet.put(pattern,alphabetFeatureSet.get(pattern));
				}
			}
			filteredActualFeatureAlphabetFeatureSetMap.put(feature, filteredAlphabetFeatureSet);
			
			System.out.println("Filtered Feature Set: " + feature.toString() + " " + filteredAlphabetFeatureSet.toString());
		}
	}
	
	/*
	 * Bruce 29.05.2014
	 * This filter is similar to filterFeatureSet2 but reuses the createInstanceVector method
	 * to count the frequency of features, rather than compute on the concatenated traces of whole log
	 * Precondition: instance vector has been created
	 */
	private void filterFeatureSet3(){
		
		double supportCount = 0.1;
		boolean accept, inputAgain;
		int optionSelected;
		String inputSupportCount;
		
		System.out.println("Filtering Feature Set...");
		
		/*
		 * SEQUENCE FEATURES
		 */
		List<InstanceVector> instanceVectorList;
		Set<String> filteredFeatureSet = new HashSet<String>();
		Map <String,Integer> featureCountMap;
		
		int traceCount = encodedTraceList.size();
		filteredActualFeatureSequenceFeatureSetMap = new HashMap<Feature, Set<String>>();
		
		for (Feature feature : actualFeatureSequenceFeatureSetMap.keySet()) {
			
			/*
			 * Special case.
			 * Break for IE feature because IE is processed as alphabet feature (maybe a code mistake)
			 * 
			 */
			if (feature == Feature.IE) continue;
			
				
			instanceVectorList = featureInstanceVectorListMap.get(feature);
			featureCountMap = computeTotalSequenceFeatureSupportCount(instanceVectorList);
			
			System.out.println(featureCountMap.toString());
			
			/*
			 * Select features
			 */
			accept = false;
			inputAgain = false;
			while (!accept) {
				
				inputSupportCount = JOptionPane.showInputDialog("Total no. of features: " + featureCountMap.size() + 
											". Enter a support count (frequency) threshold to select features.",
											"0.1");
				try {
					inputAgain = false;
					supportCount = Double.valueOf(inputSupportCount);
				} catch (Exception ex) {
					inputAgain = true;
				}
			
				if (!inputAgain) {
					filteredFeatureSet.clear();
					for (String pattern : featureCountMap.keySet()) {
						if ((double)featureCountMap.get(pattern)/traceCount >= supportCount) {
							filteredFeatureSet.add(pattern);
						}
					}
					optionSelected = JOptionPane.showConfirmDialog(new JFrame(), "Number of features selected: " + 
												filteredFeatureSet.size() + 
												". Feature support count (frequency) = " + supportCount,
												"Review feature count",
												JOptionPane.YES_NO_OPTION);
					accept = (optionSelected == JOptionPane.YES_OPTION);
				}
			}
			
			filteredActualFeatureSequenceFeatureSetMap.put(feature, filteredFeatureSet);
			
			System.out.println("Filtered Feature Set: " + feature.toString() + " " + filteredFeatureSet.toString());
		}

		/*
		 * ALPHABET FEATURES
		 */
		Map<Set<String>, Set<String>> alphabetFeatureSet;
		Map<Set<String>, Set<String>> filteredAlphabetFeatureSet = new HashMap<Set<String>, Set<String>>();
		Map<Set<String>, Integer> alphabetFeatureCountMap;
		filteredActualFeatureAlphabetFeatureSetMap = new HashMap<Feature, Map<Set<String>, Set<String>>>();
		
		for (Feature feature : actualFeatureAlphabetFeatureSetMap.keySet()) {
			alphabetFeatureSet = actualFeatureAlphabetFeatureSetMap.get(feature);
			instanceVectorList = featureInstanceVectorListMap.get(feature);
			alphabetFeatureCountMap = computeTotalAlphabetFeatureSupportCount(instanceVectorList);
			
			System.out.println(alphabetFeatureCountMap.toString());
			
			/*
			 * Select features
			 */
			accept = false;
			inputAgain = false;
			while (!accept) {
				
				inputSupportCount = JOptionPane.showInputDialog("Total no. of features: " + alphabetFeatureCountMap.size() + 
											". Enter a support count (frequency) threshold to select features.",
											"0.1");
				try {
					inputAgain = false;
					supportCount = Double.valueOf(inputSupportCount);
				} catch (Exception ex) {
					inputAgain = true;
				}
			
				if (!inputAgain) {
					filteredAlphabetFeatureSet.clear();
					for (Set<String> pattern : alphabetFeatureCountMap.keySet()) {
						if ((double)alphabetFeatureCountMap.get(pattern)/traceCount >= supportCount) {
							filteredAlphabetFeatureSet.put(pattern,alphabetFeatureSet.get(pattern));
						}
					}
					optionSelected = JOptionPane.showConfirmDialog(new JFrame(), "Number of features selected: " + 
												filteredAlphabetFeatureSet.size() + 
												". Feature support count (frequency) = " + supportCount,
												"Review feature count",
												JOptionPane.YES_NO_OPTION);
					accept = (optionSelected == JOptionPane.YES_OPTION);
				}
			}			
			
			
			filteredActualFeatureAlphabetFeatureSetMap.put(feature, filteredAlphabetFeatureSet);
			
			System.out.println("Filtered Feature Set: " + feature.toString() + " " + filteredAlphabetFeatureSet.toString());
		}
	}	
	
	/**
	 * Bruce: 27 May 2015
	 * Filter features with minimum support
	 * Min support is read from the property file 
	 * @Input: featureInstanceVectorListMap
	 * @Return: filteredActualFeatureSequenceFeatureSetMap and filteredActualFeatureAlphabetFeatureSetMap
	 */
	private void filterFeatureSetBySupportLevel(double min_sup) throws FileNotFoundException, IOException, NumberFormatException {
		//double supportCount = ConnectionManager.getMinSup();
		System.out.println("Filtering Feature Set...");
		
		/*******************************
		 * SEQUENCE FEATURES
		 *******************************/
		List<InstanceVector> instanceVectorList;
		Set<String> filteredFeatureSet = new HashSet<String>();
		Map <String,Integer> featureCountMap;
		
		int traceCount = encodedTraceList.size();
		filteredActualFeatureSequenceFeatureSetMap = new HashMap<Feature, Set<String>>();
		
		for (Feature feature : actualFeatureSequenceFeatureSetMap.keySet()) {
			
			/*
			 * Special case.
			 * Break for IE feature because IE is processed as alphabet feature (maybe a code mistake)
			 * 
			 */
			if (feature == Feature.IE) continue;
				
			instanceVectorList = featureInstanceVectorListMap.get(feature);
			featureCountMap = computeTotalSequenceFeatureSupportCount(instanceVectorList);
			//System.out.println(featureCountMap.toString());
			
			/*
			 * Select features
			 */
			filteredFeatureSet.clear();
			for (String pattern : featureCountMap.keySet()) {
				if ((double)featureCountMap.get(pattern)/traceCount >= min_sup) {
					filteredFeatureSet.add(pattern);
				}
			}
			
			filteredActualFeatureSequenceFeatureSetMap.put(feature, filteredFeatureSet);
			System.out.println("Number of features selected: " + filteredFeatureSet.size());
			System.out.println(filteredFeatureSet.toString());
			
			//System.out.println("Filtered Feature Set: " + feature.toString() + " " + filteredFeatureSet.toString());
		}

		/*******************************
		 * ALPHABET FEATURES
		 *******************************/
		Map<Set<String>, Set<String>> alphabetFeatureSet;
		Map<Set<String>, Set<String>> filteredAlphabetFeatureSet = new HashMap<Set<String>, Set<String>>();
		Map<Set<String>, Integer> alphabetFeatureCountMap;
		filteredActualFeatureAlphabetFeatureSetMap = new HashMap<Feature, Map<Set<String>, Set<String>>>();
		
		for (Feature feature : actualFeatureAlphabetFeatureSetMap.keySet()) {
			alphabetFeatureSet = actualFeatureAlphabetFeatureSetMap.get(feature);
			instanceVectorList = featureInstanceVectorListMap.get(feature);
			alphabetFeatureCountMap = computeTotalAlphabetFeatureSupportCount(instanceVectorList);
			//System.out.println(alphabetFeatureCountMap.toString());
			
			/*
			 * Select features
			 */
			filteredAlphabetFeatureSet.clear();
			for (Set<String> pattern : alphabetFeatureCountMap.keySet()) {
				if ((double)alphabetFeatureCountMap.get(pattern)/traceCount >= min_sup) {
					filteredAlphabetFeatureSet.put(pattern,alphabetFeatureSet.get(pattern));
				}
			}			
			
			filteredActualFeatureAlphabetFeatureSetMap.put(feature, filteredAlphabetFeatureSet);
			System.out.println("Number of features selected: " + filteredAlphabetFeatureSet.size());
			System.out.println(filteredAlphabetFeatureSet.toString());			
			//System.out.println("Filtered Feature Set: " + feature.toString() + " " + filteredAlphabetFeatureSet.toString());
		}
	}		
	
	/**
	 * Filter features by using a feature selection measure, such as fisher score
	 * @throws Exception 
	 * @Input: featureInstanceVectorListMap
	 * @Return: filterFeatureSelectionMap
	 */
	private void filterFeatureSetByFeatureSelection(int coverage_thres) throws Exception {
		Map<String, Double> featureSelectionMap = null;
		
		for (Feature feature : filteredActualFeatureSequenceFeatureSetMap.keySet()) {
			featureSelectionMap = this.featureSelection(coverage_thres);
			filterFeatureSelectionMap.put(feature, featureSelectionMap);
		}
		
		for (Feature feature : filteredActualFeatureAlphabetFeatureSetMap.keySet()) {
			featureSelectionMap = this.featureSelection(coverage_thres);
			filterFeatureSelectionMap.put(feature, featureSelectionMap);
		}		
		
		System.out.println("Number of features selected: " + featureSelectionMap.size());
		System.out.println(featureSelectionMap.toString());
	}
	
	/**
	 * Scan the instance vector list and compute fisher score for every feature
	 * The attribute values and the label values of every instance must not contain comma character (":")
	 * Input: 
	 * 		- featureInstanceVectorListMap: instance vector in the feature space
	 * 		- filteredActualFeatureSequenceFeatureSetMap: filtered sequence features by min support
	 * 		- filteredActualFeatureAlphabetFeatureSetMap: filtered alphabet features by min support
	 * @return Map from Feature value to Fisher score, in descreasing order of Fisher score
	 * @throws Exception 
	 */
	private Map<String, Double> computeFisherScoreForSequenceFeatures() throws Exception {
		
		Map<String, Double> featureFisherScoreMap = new HashMap<String, Double>(); //key is feature, value is fisher score
		Map<String, Double> featureClassAvgValueMap = new HashMap<String, Double>(); //key is feature + ":" + class, value is the feature avg value in the class
		Map<String, Double> featureClassStdDevValueMap = new HashMap<String, Double>(); //key is feature + ":" + class, value is the feature std value in the class
		Map<String, Double> featureOverallAvgValueMap = new HashMap<String, Double>(); //key is feature, value is the feature avg value in the whole instance vector list
		Map<String, Integer> classInstanceCountMap = new HashMap<String, Integer>(); //key is class label, value is the number of instances in the class
		
		List<InstanceVector> instanceVectorList;
		Map<String,Integer> instanceFeatureCountMap;
		String featureClassKey = "";
		Double doubleValue;
		
		for (Feature featureType : actualFeatureSequenceFeatureSetMap.keySet()) {
			instanceVectorList = featureInstanceVectorListMap.get(featureType);
			
			for (InstanceVector instance : instanceVectorList) {
				instanceFeatureCountMap = instance.getSequenceFeatureCountMap(); // feature and its support count in a trace, not present if support = 0
				
				/*************************************
				 * Calculate class instance count for every class i: Ni
				 *************************************/
				if (!classInstanceCountMap.containsKey(instance.getLabel())) {
					classInstanceCountMap.put(instance.getLabel(), 1);
				} 
				else {
					classInstanceCountMap.put(instance.getLabel(), classInstanceCountMap.get(instance.getLabel()) + 1);
				}
				
				//--------------------------
				// For every feature
				//--------------------------
				for (String feature : filteredActualFeatureSequenceFeatureSetMap.get(featureType)) {
					double frequency = 0.0;
					
					if (instanceFeatureCountMap.containsKey(feature)) {
						frequency = instanceFeatureCountMap.get(feature);
					}
					else {
						frequency = 0;
					}
					
					/*************************************
					 * Calculate overall avg feature value: SUMiSUMj(Xij), for all feature j and class i
					 * Avoid dividing for the total number of instance for not losing the numeric precision
					 *************************************/					
					if (!featureOverallAvgValueMap.containsKey(feature)) {
						doubleValue = 1.0*frequency;
					} 
					else {
						doubleValue = featureOverallAvgValueMap.get(feature) + 1.0*(frequency);
					}
					featureOverallAvgValueMap.put(feature, doubleValue);
				
					/*************************************
					 * Calculate average feature value by class with the count only: SUMij(Xij), all feature value of feature j for class i
					 * The average value would be update later by dividing the total number of instances in each class
					 *************************************/					
					featureClassKey = feature + ":" + instance.getLabel(); //feature + ":" + label
					if (!featureClassAvgValueMap.containsKey(featureClassKey)) {
						doubleValue = frequency;
					} 
					else {
						doubleValue = featureClassAvgValueMap.get(featureClassKey) + 1.0*frequency;
					}
					featureClassAvgValueMap.put(featureClassKey, doubleValue);	//count only	
				}				
			}
			
			/*************************************
			 * Update the total average feature value in the whole dataset
			 * by dividing by the total number of instances 
			 *************************************/
			for (String feature : featureOverallAvgValueMap.keySet()) {
				featureOverallAvgValueMap.put(feature, 1.0*featureOverallAvgValueMap.get(feature)/instanceVectorList.size());
			}
			
			/*************************************
			 * Calculate average feature value by classes: SUMij(Xij)/Ni, SUMij(Xij) and Ni have been calculated above for every class i
			 *************************************/				
			for (String featureClass : featureClassAvgValueMap.keySet()) {
				String[] split = featureClass.split(":"); // split[0] is the feature, split[1] is the class label
				featureClassAvgValueMap.put(featureClass, 1.0*featureClassAvgValueMap.get(featureClass)/classInstanceCountMap.get(split[1]));
			}			
			
			/*************************************
			 * Calculate std deviation by classes in two steps.
			 * First, calculate SUMj((Xij - Ui)^2), Ui is the avg feature value by class i as calculated above
			 * Second, calculate SUMj((Xij - Ui)^2)/Ni, Ni is the number of instances in class i, as calculated above
			 * Note: here the square root is not taken because it is not used in the Fisher score formula
			 * So this is to calculate the power 2 of std dev instead of std dev
			 *************************************/				
			for (InstanceVector instance : instanceVectorList) { //for every instance
				instanceFeatureCountMap = instance.getSequenceFeatureCountMap();
				for (String feature : filteredActualFeatureSequenceFeatureSetMap.get(featureType)) {
					featureClassKey = feature + ":" + instance.getLabel();
					if (instanceFeatureCountMap.containsKey(feature)) {
						if (!featureClassStdDevValueMap.containsKey(featureClassKey)) {
							doubleValue = Math.pow(instanceFeatureCountMap.get(feature) - featureClassAvgValueMap.get(featureClassKey),2);
						} 
						else {
							doubleValue = featureClassStdDevValueMap.get(featureClassKey) + 1.0*Math.pow(instanceFeatureCountMap.get(feature) - featureClassAvgValueMap.get(featureClassKey),2);
						}	
					}
					else {
						if (!featureClassStdDevValueMap.containsKey(featureClassKey)) {
							doubleValue = Math.pow(featureClassAvgValueMap.get(featureClassKey),2);
						} 
						else {
							doubleValue = featureClassStdDevValueMap.get(featureClassKey) + 1.0*Math.pow(featureClassAvgValueMap.get(featureClassKey),2);
						}
					}
					featureClassStdDevValueMap.put(featureClassKey, doubleValue);
				}
			}
			
			// Then, finalize the std deviation: SUMj((Xij - Ui)^2)/Ni
			for (String featureClassKey2 : featureClassStdDevValueMap.keySet()) {
				String[] split = featureClassKey2.split(":"); // split[0] is the feature, split[1] is the class label
				featureClassStdDevValueMap.put(featureClassKey2, featureClassStdDevValueMap.get(featureClassKey2)/classInstanceCountMap.get(split[1]));
			}
			
			/*************************************
			 * Now, calculate the Fisher score: SUMi(Ni*(Ui - U)^2) / SUMi(Ni*STDDEVi), for all class i
			 *************************************/				
			for (String feature : featureOverallAvgValueMap.keySet()) {
				Double nominator = 0.0;
				Double denominator = 0.0;	
				featureClassKey = "";
				
				//For every class label
				for (String classLabel : classInstanceCountMap.keySet()) { //for every feature-class pair
					featureClassKey = feature + ":" + classLabel;
					if (featureClassAvgValueMap.containsKey(featureClassKey)) {
						nominator += 1.0*classInstanceCountMap.get(classLabel)*Math.pow(featureClassAvgValueMap.get(featureClassKey) - featureOverallAvgValueMap.get(feature),2);
						denominator += 1.0*classInstanceCountMap.get(classLabel)*featureClassStdDevValueMap.get(featureClassKey);
					}
					else {
						//nominator += 1.0*classInstanceCountMap.get(classLabel)*Math.pow(featureOverallAvgValueMap.get(feature),2);
						throw new Exception("Error in Fisher score calculation: featureClassAvgValueMap does not contain key: " + featureClassKey);
					}
				}
				
				if (denominator != 0.0) {
					featureFisherScoreMap.put(feature, 1.0*nominator/denominator);
				}
				else if (nominator == 0.0) {
					featureFisherScoreMap.put(feature, 0.0);
				}
				else {
					featureFisherScoreMap.put(feature, Double.MAX_VALUE);
				}
			}
		}
		
		return featureFisherScoreMap;
		
	}
	
	/**
	 * In case of alphabet feature, the attribute name is converted 
	 * from set of string, which would have this format if use toString(), e.g.: "[ab0],[ab1],[ab2]"
	 * Need to remove special characters and return "ab0ab1ab2"
	 */	
	private String extractStringFromAlphabetFeature(Set<String> feature) {
		ArrayList<String> featureList = new ArrayList<String>(feature);
		Collections.sort(featureList);
		
		String featureString = featureList.toString(); //"[ab0],[ab1],[ab2]"
		featureString = featureString.replace("[", "");
		featureString = featureString.replace("]", "");
		featureString = featureString.replace(" ", "");  
		featureString = featureString.replace(",", ""); // now "ab0ab1ab2"
    	return featureString;		
	}
	
	/**
	 * Scan the instance vector list and compute fisher score for every alphabet feature
	 * This is different from the same task for sequence feature because the 
	 * alphabet feature has a form of a set of features, rather than a string
	 * Input: 
	 * 		- featureInstanceVectorListMap: instance vector in the feature space
	 * 		- filteredActualFeatureAlphabetFeatureSetMap: filtered alphabet features by min support
	 * @return Map from Feature value to Fisher score, in descreasing order of Fisher score
	 * @throws Exception 
	 */
	private Map<String, Double> computeFisherScoreForAlphabetFeatures() throws Exception {
		
		Map<String, Double> featureFisherScoreMap = new HashMap<String, Double>(); //key is feature, value is fisher score
		Map<String, Double> featureClassAvgValueMap = new HashMap<String, Double>(); //key is feature + ":" + class, value is the feature avg value in the class
		Map<String, Double> featureClassStdDevValueMap = new HashMap<String, Double>(); //key is feature + ":" + class, value is the feature std value in the class
		Map<String, Double> featureOverallAvgValueMap = new HashMap<String, Double>(); //key is feature, value is the feature avg value in the whole instance vector list
		Map<String, Integer> classInstanceCountMap = new HashMap<String, Integer>(); //key is class label, value is the number of instances in the class
		
		List<InstanceVector> instanceVectorList;
		Map<String,Integer> instanceFeatureCountMap = new HashMap<String, Integer>();
		String featureClassKey = "";
		Double doubleValue;
		String feature = "";
		
		for (Feature featureType : actualFeatureAlphabetFeatureSetMap.keySet()) {
			instanceVectorList = featureInstanceVectorListMap.get(featureType);
			
			for (InstanceVector instance : instanceVectorList) {
				
				//Convert the set-based key map to string-based key map, to make it similar to the processing for sequence features
				instanceFeatureCountMap.clear();
				for (Set<String> setFeature : instance.getAlphabetFeatureCountMap().keySet()) {
					instanceFeatureCountMap.put(this.extractStringFromAlphabetFeature(setFeature), instance.getAlphabetFeatureCountMap().get(setFeature));
				}
				
				/*************************************
				 * Calculate class instance count for every class i: Ni
				 *************************************/
				if (!classInstanceCountMap.containsKey(instance.getLabel())) {
					classInstanceCountMap.put(instance.getLabel(), 1);
				} 
				else {
					classInstanceCountMap.put(instance.getLabel(), classInstanceCountMap.get(instance.getLabel()) + 1);
				}
				
				// For every feature
				for (Set<String> setFeature : filteredActualFeatureAlphabetFeatureSetMap.get(featureType).keySet()) {
					
					feature = this.extractStringFromAlphabetFeature(setFeature);
					
					double frequency = 0.0;
					if (instanceFeatureCountMap.containsKey(feature)) {
						frequency = instanceFeatureCountMap.get(feature);
					}
					else {
						frequency = 0;
					}					
					
					/*************************************
					 * Calculate overall avg feature value: SUMiSUMj(Xij)/Total No of traces, for all feature j and class i
					 *************************************/
					if (!featureOverallAvgValueMap.containsKey(feature)) {
						doubleValue = 1.0*frequency;
					} 
					else {
						doubleValue = featureOverallAvgValueMap.get(feature) + 1.0*frequency;
					}
					featureOverallAvgValueMap.put(feature, doubleValue);
					
					/*************************************
					 * Calculate average feature value by class with the count only: SUMij(Xij), all feature value of feature j for class i
					 * The average value would be update later by dividing the total number of instances in each class
					 *************************************/					
					featureClassKey = feature + ":" + instance.getLabel();
					if (!featureClassAvgValueMap.containsKey(featureClassKey)) {
						doubleValue = 1.0*frequency;
					} 
					else {
						doubleValue = featureClassAvgValueMap.get(featureClassKey) + 1.0*frequency;
					}
					featureClassAvgValueMap.put(featureClassKey, doubleValue);	//count only	
				}				
			}
			
			/*************************************
			 * Update the total average feature value in the whole dataset
			 * by dividing by the total number of instances 
			 *************************************/
			for (String feature2 : featureOverallAvgValueMap.keySet()) {
				featureOverallAvgValueMap.put(feature2, 1.0*featureOverallAvgValueMap.get(feature2)/instanceVectorList.size());
			}
			
			/*************************************
			 * Calculate average feature value by classes: SUMij(Xij)/Ni, SUMij(Xij) and Ni have been calculated above for every class i
			 *************************************/				
			for (String featureClass : featureClassAvgValueMap.keySet()) {
				String[] split = featureClass.split(":"); // split[0] is the feature, split[1] is the class label
				featureClassAvgValueMap.put(featureClass, 1.0*featureClassAvgValueMap.get(featureClass)/classInstanceCountMap.get(split[1]));
			}			
			
			/*************************************
			 * Calculate std deviation by classes: 
			 * Update in two stages: 
			 * First, calculate SUMj((Xij - Ui)^2), Ui is the avg feature value by class i as calculated above
			 * Second, calculate SUMj((Xij - Ui)^2)/Ni, Ni is the number of instances in class i, as calculated above
			 * Note: here the square root is not taken because it is not used in the Fisher score formula
			 * So this is to calculate the power 2 of std dev instead of std dev
			 *************************************/				
			for (InstanceVector instance : instanceVectorList) { //for every instance
				
				//Convert set-based key to string-based key
				instanceFeatureCountMap.clear();
				for (Set<String> setFeature : instance.getAlphabetFeatureCountMap().keySet()) {
					instanceFeatureCountMap.put(this.extractStringFromAlphabetFeature(setFeature), instance.getAlphabetFeatureCountMap().get(setFeature));
				}		
				
				for (Set<String> setFeature : filteredActualFeatureAlphabetFeatureSetMap.get(featureType).keySet()) {
					
					feature = this.extractStringFromAlphabetFeature(setFeature);
					featureClassKey = feature + ":" + instance.getLabel();
					
					if (instanceFeatureCountMap.containsKey(feature)) {
						if (!featureClassStdDevValueMap.containsKey(featureClassKey)) {
							doubleValue = Math.pow(instanceFeatureCountMap.get(feature) - featureClassAvgValueMap.get(featureClassKey),2);
						} 
						else {
							doubleValue = featureClassStdDevValueMap.get(featureClassKey) + 1.0*Math.pow(instanceFeatureCountMap.get(feature) - featureClassAvgValueMap.get(featureClassKey),2);
						}	
					}
					else {
						if (!featureClassStdDevValueMap.containsKey(featureClassKey)) {
							doubleValue = Math.pow(featureClassAvgValueMap.get(featureClassKey),2);
						} 
						else {
							doubleValue = featureClassStdDevValueMap.get(featureClassKey) + 1.0*Math.pow(featureClassAvgValueMap.get(featureClassKey),2);
						}						
					}
					featureClassStdDevValueMap.put(featureClassKey, doubleValue);
				}
			}
			
			// Then, finalize the std deviation: SUMj((Xij - Ui)^2)/Ni
			for (String featureClassKey2 : featureClassStdDevValueMap.keySet()) {
				String[] split = featureClassKey2.split(":"); // split[0] is the feature, split[1] is the class label
				featureClassStdDevValueMap.put(featureClassKey2, featureClassStdDevValueMap.get(featureClassKey2)/classInstanceCountMap.get(split[1]));
			}
			
			/*************************************
			 * Now, calculate the Fisher score: SUMi(Ni*(Ui - U)^2) / SUMi(Ni*STDDEVi), for all class i
			 *************************************/				
			for (String featureKey : featureOverallAvgValueMap.keySet()) {
				Double nominator = 0.0;
				Double denominator = 0.0;
				featureClassKey = "";				
				for (String classLabel : classInstanceCountMap.keySet()) { //for every feature-class pair
					featureClassKey = featureKey + ":" + classLabel;
					if (featureClassAvgValueMap.containsKey(featureClassKey)) {
						nominator += 1.0*classInstanceCountMap.get(classLabel)*Math.pow(featureClassAvgValueMap.get(featureClassKey) - featureOverallAvgValueMap.get(featureKey),2);
						denominator += 1.0*classInstanceCountMap.get(classLabel)*featureClassStdDevValueMap.get(featureClassKey);
				
					}
					else {
						//nominator += 1.0*classInstanceCountMap.get(classLabel)*Math.pow(featureOverallAvgValueMap.get(featureKey),2);
						throw new Exception("Error in Fisher score calculation: featureClassAvgValueMap does not contain key: " + featureClassKey);
					}
				}
				if (denominator != 0.0) {
					featureFisherScoreMap.put(featureKey, 1.0*nominator/denominator);
				}
				else if (nominator == 0.0) {
					featureFisherScoreMap.put(feature, 0.0);
				}
				else {
					featureFisherScoreMap.put(feature, Double.MAX_VALUE);
				}
			}
		}
		
		return featureFisherScoreMap;
		
	}	
	
	/**
	 * Select feature based on MMRS algorithm
	 * Cheng, Hong, et al. "Discriminative frequent pattern analysis for effective classification." Data Engineering, 2007. ICDE 2007. IEEE 23rd International Conference on. IEEE, 2007.
	 * This one is also implemented in Lo et al.
	 * Input: 
	 * 		- featureInstanceVectorListMap: instance vector in the feature space
	 * 		- filteredActualFeatureSequenceFeatureSetMap: filtered sequence features by min support
	 * 		- filteredActualFeatureAlphabetFeatureSetMap: filtered alphabet features by min support
	 * @throws Exception 
	 * @Note: only accept one feature type at a time: MR/TR/MRA/TRA, no group of features allowed
	 */
	private Map<String, Double> featureSelection(int coverageThreshold) throws Exception {
		Map<String, Double> F = null; //input set of patterns with fisher score calculated
		Map<String, Double> FS = new HashMap<String, Double>(); //selected set of patterns based on fisher score
		Map<InstanceVector, Integer> instanceCoverageMap = new HashMap<InstanceVector, Integer>(); //keep track of coverage count for every instance
		List<InstanceVector> TDB;
		Map<String,Integer> instanceFeatureCountMap; 	
		Iterator<Map.Entry<String, Double>> patternIterator = null;
		String feature = "";
		Set<InstanceVector> removedInstances = new HashSet<InstanceVector>();
		
		// Read coverage threshold from the parameter file
		//coverageThreshold = ConnectionManager.getCoverageThreshold();
		
		/*******************************************************
		 * FOR SEQUENCE FEATURES
		 *******************************************************/
		for (Feature featureType : actualFeatureSequenceFeatureSetMap.keySet()) {
			
			// Sort patterns in F in decreasing order of Fisher score
			F = sortByComparator(this.computeFisherScoreForSequenceFeatures());
			
			System.out.println("Features sorted by Fisher score:");
			System.out.println(F.toString());
			patternIterator = F.entrySet().iterator();
			
			while (true) {
				TDB = featureInstanceVectorListMap.get(featureType); //list of instance vector in feature space
				
				//Find the next pattern in F
				feature = patternIterator.next().getKey();
				
				//------------------------------------------------
				//Check if the current pattern covers at least one instance in TDB
				//Note: count all instances covered, not only one
				//------------------------------------------------
				boolean hasCoveredInstance = false;
				for (InstanceVector instance : TDB) {
					if (!removedInstances.contains(instance)) {
						instanceFeatureCountMap = instance.getSequenceFeatureCountMap();
						if (instanceFeatureCountMap.containsKey(feature)) { // increase the coverage count for the covered instance
							hasCoveredInstance = true;
							if (!instanceCoverageMap.containsKey(instance)) {
								instanceCoverageMap.put(instance, 1);
							}
							else {
								instanceCoverageMap.put(instance, instanceCoverageMap.get(instance) + 1);
							}
						}
					}
				}
				if (hasCoveredInstance) FS.put(feature, F.get(feature));
				patternIterator.remove();
				
				//-------------------------------------------------
				//If an instance in TDB is covered coverageThreshold, remove it out of TDB
				//-------------------------------------------------
				for (InstanceVector coveredInstance : instanceCoverageMap.keySet()) {
					if (instanceCoverageMap.get(coveredInstance) >= coverageThreshold) {
						//TDB.remove(coveredInstance);
						removedInstances.add(coveredInstance);
					}
				}
				
				if (F.isEmpty() || (removedInstances.size() == TDB.size())) { 
					break;
				}
			}
			
		}
		
		/*******************************************************
		 * FOR ALPHABET FEATURES
		 *******************************************************/
		instanceFeatureCountMap = new HashMap<String, Integer>();
		instanceCoverageMap.clear();
		removedInstances.clear();
		for (Feature featureType : actualFeatureAlphabetFeatureSetMap.keySet()) {
			
			// Sort patterns in F in decreasing order of Fisher score
			F = sortByComparator(this.computeFisherScoreForAlphabetFeatures());
			
			patternIterator = F.entrySet().iterator();			

			while (true) {
				TDB = featureInstanceVectorListMap.get(featureType); //list of instance vector in feature space
				
				//Find the next pattern in F
				feature = patternIterator.next().getKey();
				
				//------------------------------------------------
				//Check if the current pattern covers at least one instance in TDB
				//Note: count all instances covered, not only one
				//------------------------------------------------
				boolean hasCoveredInstance = false;
				for (InstanceVector instance : TDB) {
					if (!removedInstances.contains(instance)) {
						//Need to convert the set-based key to string-based key, so that
						//the following code would be reused without any changes
						instanceFeatureCountMap.clear();
						for (Set<String> setFeature : instance.getAlphabetFeatureCountMap().keySet()) {
							instanceFeatureCountMap.put(this.extractStringFromAlphabetFeature(setFeature), instance.getAlphabetFeatureCountMap().get(setFeature));
						}
						
						if (instanceFeatureCountMap.containsKey(feature)) { // increase the coverage count for the covered instance
							hasCoveredInstance = true;
							if (!instanceCoverageMap.containsKey(instance)) {
								instanceCoverageMap.put(instance, 1);
							}
							else {
								instanceCoverageMap.put(instance, instanceCoverageMap.get(instance) + 1);
							}
						}
					}
				}
				if (hasCoveredInstance) FS.put(feature, F.get(feature));
				patternIterator.remove();
				
				//-------------------------------------------------
				//If an instance in TDB is covered coverageThreshold, remove it out of TDB
				//-------------------------------------------------
				for (InstanceVector coveredInstance : instanceCoverageMap.keySet()) {
					if (instanceCoverageMap.get(coveredInstance) >= coverageThreshold) {
						//TDB.remove(coveredInstance);
						removedInstances.add(coveredInstance);
					}
				}
				
				if (F.isEmpty() || (removedInstances.size() == TDB.size())) { 
					break;
				}
			}
		}		
		
		return sortByComparator(FS);
	}
	
	/**
	 * Sort map in decreasing order of values
	 * @return sorted Map
	 * @author Mkyong.com
	 */
	private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap) {
		 
		// Convert Map to List
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
 
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return (-1)*(o1.getValue()).compareTo(o2.getValue());
			}
		});
 
		// Convert sorted map back to a Map
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Double> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	/*
	 * Bruce: compute number of traces in the log that a feature occur
	 * Return a map from feature to its count
	 * 26.05.2015: change from counting 1 for multiple occurrences in 1 trace to multiple times for a feature
	 */
	
	private Map<String,Integer> computeTotalSequenceFeatureSupportCount(List<InstanceVector> instanceVectorList) {
		Map<String,Integer> totalFeatureCountMap = new HashMap<String,Integer>();
		Map<String,Integer> instanceFeatureCountMap;
		int count;
		
		for (InstanceVector instanceVector : instanceVectorList) {
			instanceFeatureCountMap = instanceVector.getSequenceFeatureCountMap();
			for (String feature : instanceFeatureCountMap.keySet()) {
				if (!totalFeatureCountMap.containsKey(feature)) {
					count = instanceFeatureCountMap.get(feature); 
//					count = 1; //only count the trace
				} 
				else {
					count = totalFeatureCountMap.get(feature) + instanceFeatureCountMap.get(feature);
//					count = totalFeatureCountMap.get(feature) + 1;// only count the trace
				}
				totalFeatureCountMap.put(feature, count);
			}
		}
		
		return totalFeatureCountMap;
	}
	
	/*
	 * Bruce: compute number of traces in the log that a feature occur
	 * Return a map from feature to its count
	 * 26.05.2015: change from counting 1 for multiple occurrences in 1 trace to multiple times for a feature
	 */
	private Map<Set<String>, Integer> computeTotalAlphabetFeatureSupportCount (List<InstanceVector> instanceVectorList) {
		Map<Set<String>, Integer> totalFeatureCountMap = new HashMap<Set<String>, Integer>();
		Map<Set<String>, Integer> instanceFeatureCountMap;
		int count;
		
		for (InstanceVector instanceVector : instanceVectorList) {
			instanceFeatureCountMap = instanceVector.getAlphabetFeatureCountMap();
			for (Set<String> feature : instanceFeatureCountMap.keySet()) {
				if (!totalFeatureCountMap.containsKey(feature)) {
					count = instanceFeatureCountMap.get(feature);
//					count = 1;
				} 
				else {
					count = totalFeatureCountMap.get(feature) + instanceFeatureCountMap.get(feature);
//					count = totalFeatureCountMap.get(feature) + 1;
				}
				totalFeatureCountMap.put(feature, count);
			}
		}
		
		return totalFeatureCountMap;		
	}	
	
	/*
	 * Precondition: actual feature sets have been set
	 */
	private void filterFeatureSet(){
		Logger.printCall("Calling Discover Signatures -> filterFeatureSet()");
		filteredActualFeatureSequenceFeatureSetMap = new HashMap<Feature, Set<String>>(actualFeatureSequenceFeatureSetMap);
		filteredActualFeatureAlphabetFeatureSetMap = new HashMap<Feature, Map<Set<String>, Set<String>>>(actualFeatureAlphabetFeatureSetMap);
		//Logger.printReturn("Returning Discover Signatures -> filterFeatureSet()");
	}
	
	/*
	 * Create instance vector
	 * Precondition: feature sets have been computed, set actual and filtered
	 * Format: (<instanceId>,<label>,<feature1>,<feature2>,...,<featureN>)
	 * where <feature1,2,...,N> are feature patterns built in earlier steps.
	 * Input 1: filteredActualFeatureSequenceFeatureSetMap: contains all features
	 * Input 2: instanceProfileList contains all instances
	 * Output 1: featureInstanceVectorListMap contains all instance vectors 
	 * Output 2: featureAttributeNameListMap contains list of all features (attributes)
	 * In case of alphabet features, attribute name in featureAttributeNameListMap is concatenation of 
	 * alphabet components, delimited by comma character, e.g. ("ab0","ab1","ab2") -> "ab0,ab1,ab2" 
	 * For IE feature, it is considered as Alphabet feature set
	 */
	private void createInstanceVector(){
		if(featureInstanceVectorListMap == null){
			featureInstanceVectorListMap = new HashMap<Feature, List<InstanceVector>>();
		}else{
			featureInstanceVectorListMap.clear();
		}
		
		if(featureAttributeNameListMap == null){
			featureAttributeNameListMap = new HashMap<Feature, List<String>>();
		}else{
			featureAttributeNameListMap.clear();
		}
		
		/*
		 * FOR SEQUENCE FEATURES
		 * For IE feature, it also run this code below, however it is overridden by the alphabet code following
		 */
		List<InstanceVector> instanceVectorList;
		InstanceVector instanceVector;
		List<String> attributeNameList;

		Set<String> sequenceFeatureSet;
		List<String> sequenceFeatureList = new ArrayList<String>();
		
		for (Feature feature : filteredActualFeatureSequenceFeatureSetMap.keySet()) {
			sequenceFeatureSet = filteredActualFeatureSequenceFeatureSetMap.get(feature);
			
			sequenceFeatureList.clear();
			sequenceFeatureList.addAll(sequenceFeatureSet);
			
			instanceVectorList = new ArrayList<InstanceVector>();
			
			for (InstanceProfile instanceProfile : instanceProfileList) {
				instanceVector = new InstanceVector();
				instanceVector.setLabel(instanceProfile.getLabel());
				instanceVector.setName(instanceProfile.getName()); //Bruce 27.05.2014
				instanceVector.setEncodedTrace(instanceProfile.getEncodedTrace()); //Bruce 27.05.2014
							
				/*
				 * Create instance vector based on encoded trace and the feature set
				 */
				instanceVector.setSequenceFeatureCountMap(featureExtraction
						.computeNonOverlapSequenceFeatureCountMap(encodingLength,
								instanceProfile.getEncodedTrace(), sequenceFeatureSet));
				
				instanceVector.standarizeNumericVector(sequenceFeatureList);
				instanceVector.standarizeNominalVector(sequenceFeatureList);
				instanceVectorList.add(instanceVector);
			}
			featureInstanceVectorListMap.put(feature, instanceVectorList);
			attributeNameList = new ArrayList<String>();
			attributeNameList.addAll(sequenceFeatureList);
			featureAttributeNameListMap.put(feature, attributeNameList);
		}
		
		/*
		 * FOR ALPHABET FEATURES
		 * For IE feature, this code below is run, thus overrides the above code for sequence feature.
		 */		
		Map<Set<String>, Set<String>> alphabetFeatureSetMap;
		List<Set<String>> alphabetFeatureList = new ArrayList<Set<String>>();
		
		for(Feature feature : filteredActualFeatureAlphabetFeatureSetMap.keySet()){
			alphabetFeatureSetMap = filteredActualFeatureAlphabetFeatureSetMap.get(feature);
			alphabetFeatureList.clear();
			alphabetFeatureList.addAll(alphabetFeatureSetMap.keySet());
			
			instanceVectorList = new ArrayList<InstanceVector>();
			for(InstanceProfile instanceProfile : instanceProfileList){
				instanceVector = new InstanceVector();
				instanceVector.setLabel(instanceProfile.getLabel());
				instanceVector.setName(instanceProfile.getName()); //Bruce 27.05.2014
				instanceVector.setEncodedTrace(instanceProfile.getEncodedTrace()); //Bruce 27.05.2014

				instanceVector.setAlphabetFeatureCountMap(featureExtraction.computeNonOverlapAlphabetFeatureCountMap(encodingLength, instanceProfile.getEncodedTrace(), alphabetFeatureSetMap));
				instanceVector.standarizeNumericVector(alphabetFeatureList);
				instanceVector.standarizeNominalVector(alphabetFeatureList);
				instanceVectorList.add(instanceVector);
				
			}
			featureInstanceVectorListMap.put(feature, instanceVectorList);
			
			attributeNameList = new ArrayList<String>();
			for(Set<String> alphabet : alphabetFeatureList)
				attributeNameList.add(alphabet.toString());

			/*
			 * Add the attribute names
			 */
			featureAttributeNameListMap.put(feature, attributeNameList);
		}
	}
	
	/**
	 * Create InstanceVector from the input instance profile list
	 * Used for create the instance vector for test data
	 * Note that the features have been mined based on training data
	 * Reuse the code from createInstanceVector, but remove all assignments relating to class variables
	 * Ensure that there are no reassignment to class variables by executing this method
	 * Otherwise, it will cause inconsistent class variable values  
	 * Particularly in these two  methods: computeNonOverlapSequenceFeatureCountMap and computeNonOverlapAlphabetFeatureCountMap
	 * @param instanceProfileList
	 * @return
	 * @author Bruce Nguyen
	 */
	private List<InstanceVector> createInstanceVector(List<InstanceProfile> instanceProfileList){
		
		/*
		 * FOR SEQUENCE FEATURES
		 * For IE feature, it also run this code below, however it is overridden by the alphabet code following
		 */
		List<InstanceVector> instanceVectorList = new ArrayList<InstanceVector>();
		InstanceVector instanceVector;

		Set<String> sequenceFeatureSet;
		List<String> sequenceFeatureList = new ArrayList<String>();
		
		for (Feature feature : filteredActualFeatureSequenceFeatureSetMap.keySet()) {
			sequenceFeatureSet = filteredActualFeatureSequenceFeatureSetMap.get(feature);
			
			sequenceFeatureList.clear();
			sequenceFeatureList.addAll(sequenceFeatureSet);
			
			for (InstanceProfile instanceProfile : instanceProfileList) {
				instanceVector = new InstanceVector();
				instanceVector.setLabel(instanceProfile.getLabel());
				instanceVector.setName(instanceProfile.getName()); //Bruce 27.05.2014
				instanceVector.setEncodedTrace(instanceProfile.getEncodedTrace()); //Bruce 27.05.2014
							
				/*
				 * Create instance vector based on encoded trace and the feature set
				 */
				instanceVector.setSequenceFeatureCountMap(featureExtraction
						.computeNonOverlapSequenceFeatureCountMap(encodingLength,
								instanceProfile.getEncodedTrace(), sequenceFeatureSet));
				
				instanceVector.standarizeNumericVector(sequenceFeatureList);
				instanceVector.standarizeNominalVector(sequenceFeatureList);
				instanceVectorList.add(instanceVector);
			}
//			featureInstanceVectorListMap.put(feature, instanceVectorList);
//			attributeNameList = new ArrayList<String>();
//			attributeNameList.addAll(sequenceFeatureList);
//			featureAttributeNameListMap.put(feature, attributeNameList);
		}
		
		/*
		 * FOR ALPHABET FEATURES
		 * For IE feature, this code below is run, thus overrides the above code for sequence feature.
		 */		
		Map<Set<String>, Set<String>> alphabetFeatureSetMap;
		List<Set<String>> alphabetFeatureList = new ArrayList<Set<String>>();
		
		for(Feature feature : filteredActualFeatureAlphabetFeatureSetMap.keySet()){
			alphabetFeatureSetMap = filteredActualFeatureAlphabetFeatureSetMap.get(feature);
			alphabetFeatureList.clear();
			alphabetFeatureList.addAll(alphabetFeatureSetMap.keySet());
			
			for(InstanceProfile instanceProfile : instanceProfileList){
				instanceVector = new InstanceVector();
				instanceVector.setLabel(instanceProfile.getLabel());
				instanceVector.setName(instanceProfile.getName()); //Bruce 27.05.2014
				instanceVector.setEncodedTrace(instanceProfile.getEncodedTrace()); //Bruce 27.05.2014

				instanceVector.setAlphabetFeatureCountMap(featureExtraction.computeNonOverlapAlphabetFeatureCountMap(encodingLength, instanceProfile.getEncodedTrace(), alphabetFeatureSetMap));
				instanceVector.standarizeNumericVector(alphabetFeatureList);
				instanceVector.standarizeNominalVector(alphabetFeatureList);
				instanceVectorList.add(instanceVector);
				
			}
			
//			featureInstanceVectorListMap.put(feature, instanceVectorList);
//			
//			attributeNameList = new ArrayList<String>();
//			for(Set<String> alphabet : alphabetFeatureList)
//				attributeNameList.add(alphabet.toString());
//
//			/*
//			 * Add the attribute names
//			 */
//			featureAttributeNameListMap.put(feature, attributeNameList);
		}
		
		return instanceVectorList;
	}	
	
	/*
	 * Generate Weka .arff file
	 * Read arff file into a Weka DataSource and DataInstance object
	 * Return: featureNominalWekaInstancesMap, featureNumericWekaInstancesMap
	 */
	private void prepareWekaData(){
		// This is the property name for accessing OS temporary directory or
		String tempDirProperty = "java.io.tmpdir"; //On Windows: %USERPROFILE%\AppData\Local\Temp
		String fileSeparator = System.getProperty("file.separator");
		// Set the outputDir within the tempDir
		String outputDir = System.getProperty(tempDirProperty)+fileSeparator+"ProM"+fileSeparator+"SignatureDiscovery";
//		System.out.println("Temp Dir: "+outputDir);
		FileIO io = new FileIO();
		io.writeToFile(outputDir, "charActivityMap.txt", charActivityMap, "\\^");	
		io.writeToFile(outputDir, "instanceProfileList.txt", instanceProfileList);
		List<InstanceVector> instanceVectorList;
		List<String> attributeNameList;
		
		Set<String> classLabelSet = new HashSet<String>();
		FileOutputStream fos;
		PrintStream ps;
		Iterator<String> it;
		try{
			for(Feature feature : featureInstanceVectorListMap.keySet()){
				instanceVectorList = featureInstanceVectorListMap.get(feature);
				attributeNameList = featureAttributeNameListMap.get(feature);
				classLabelSet.clear();
				
				for(InstanceVector instanceVector : instanceVectorList){
					classLabelSet.add(instanceVector.getLabel());
				}
				
				/*
				 * Generate Weka data file
				 */
				
				if(!new File(outputDir+"\\Weka").exists()){
					new File(outputDir+"\\Weka").mkdirs();
				}
				
				
				fos = new FileOutputStream(outputDir+"\\Weka\\"+feature+"_Nominal.arff");
				ps = new PrintStream(fos);
				ps.println("@RELATION SignatureDiscovery");
				for(String attributeName : attributeNameList){
					ps.println("@ATTRIBUTE "+attributeName.replaceAll(", ", "_").trim()+" {0,1}");
				}
				ps.print("@ATTRIBUTE class {");
				it = classLabelSet.iterator();
				while(it.hasNext()){
					ps.print(it.next());
					if(it.hasNext())
						ps.print(",");
				}
				ps.println("}");
				ps.println();
				ps.println("@DATA");
				for(InstanceVector instanceVector : instanceVectorList){
					ps.println(instanceVector.toStringStandarizedNominalVector());
				}
				ps.close();
				fos.close();
				
				fos = new FileOutputStream(outputDir+"\\Weka\\"+feature+"_Numeric.arff");
				ps = new PrintStream(fos);
				ps.println("@RELATION SignatureDiscovery");
				for(String attributeName : attributeNameList){
					ps.println("@ATTRIBUTE "+attributeName.replaceAll(", ", "_").trim()+" REAL");
				}
				ps.print("@ATTRIBUTE class {");
				it = classLabelSet.iterator();
				while(it.hasNext()){
					ps.print(it.next());
					if(it.hasNext())
						ps.print(",");
				}
				ps.println("}");
				ps.println();
				ps.println("@DATA");
				for(InstanceVector instanceVector : instanceVectorList){
					ps.println(instanceVector.toStringStandarizedNumericVector());
				}
				ps.close();
				fos.close();
				
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		
		//	Now read the data from the generated .arff files
		
		featureNominalWekaInstancesMap = new HashMap<Feature, Instances>();
		featureNumericWekaInstancesMap = new HashMap<Feature, Instances>();
		
		DataSource dataSource;
		Instances data;
		try {
			for(Feature feature : featureInstanceVectorListMap.keySet()){
				if(input.isNominalCount || input.featureType == FeatureType.Best){
					dataSource = new DataSource(outputDir+"\\Weka\\"+feature+"_Nominal.arff");
				
					data = dataSource.getDataSet();
					if(data.classIndex() == -1)
						data.setClassIndex(data.numAttributes()-1);
					featureNominalWekaInstancesMap.put(feature, data);
				}
				if(!input.isNominalCount || input.featureType == FeatureType.Best){
					dataSource = new DataSource(outputDir+"\\Weka\\"+feature+"_Numeric.arff");
					data = dataSource.getDataSet();
					if(data.classIndex() == -1)
						data.setClassIndex(data.numAttributes()-1);
					featureNumericWekaInstancesMap.put(feature, data);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void findSignatures(){
//		System.out.println(input.learningAlgorithmType);
		attributesInRuleSet = new HashSet<String>();
		String optionsString;
		Map<String, Map<Feature, RuleListMetrics>> optionsStringFeatureRuleListMetricsMap = new HashMap<String, Map<Feature,RuleListMetrics>>();
		Map<Feature, RuleListMetrics> featureRuleListMetricsMap;
		
		try{
			if(input.learningAlgorithmType == LearningAlgorithmType.Best){
				//Try Decision Tree; ID3 doesn't depend on any parameters 
				featureRuleListMetricsMap = findID3Signatures();
				optionsStringFeatureRuleListMetricsMap.put("Nominal", featureRuleListMetricsMap);
			
				//Parameter tuning is required only for J48 and not for ID3;			
				for(double c = 0.2; c < 0.5 ; c += 0.1){
					optionsString ="-C "+c+" -M 1";
					if(featureNominalWekaInstancesMap.size() > 0){
						featureRuleListMetricsMap = findJ48Signatures(optionsString, featureNominalWekaInstancesMap);
						optionsStringFeatureRuleListMetricsMap.put(optionsString+" Nominal", featureRuleListMetricsMap);
					}
					if(featureNumericWekaInstancesMap.size() > 0){
						featureRuleListMetricsMap = findJ48Signatures(optionsString, featureNumericWekaInstancesMap);
						optionsStringFeatureRuleListMetricsMap.put(optionsString+" Numeric", featureRuleListMetricsMap);
					}
					
				}
				
				//Parameter tuning is required for association rules; we restrict it to minSupport >= 0.2 and minConfidence >= 0.9 
				for(float minSupport = 0.2f; minSupport < 1.0; minSupport += 0.1){
					optionsString = "AssociationRules -minSupport "+minSupport+" -minConfidence "+0.9;
					featureRuleListMetricsMap = findAssociationRules(minSupport, 0.9f);
					optionsStringFeatureRuleListMetricsMap.put(optionsString+" Nominal", featureRuleListMetricsMap);
				}
			}else{
				//One of decision tree or association rule learning algorithms would have been selected
				optionsString = getParameterOptions();
				if(input.learningAlgorithmType == LearningAlgorithmType.AssociationRules){
					featureRuleListMetricsMap = findAssociationRules();
					optionsStringFeatureRuleListMetricsMap.put(optionsString+" Nominal", featureRuleListMetricsMap);
					Logger.println("Options String: "+optionsString);
				}else if(input.learningAlgorithmType == LearningAlgorithmType.J48){
						if(featureNominalWekaInstancesMap.size() > 0){
							featureRuleListMetricsMap = findJ48Signatures(optionsString, featureNominalWekaInstancesMap);
							optionsStringFeatureRuleListMetricsMap.put(optionsString+" Nominal", featureRuleListMetricsMap);
						}
						if(featureNumericWekaInstancesMap.size() > 0){
							featureRuleListMetricsMap = findJ48Signatures(optionsString, featureNumericWekaInstancesMap);
							optionsStringFeatureRuleListMetricsMap.put(optionsString+" Numeric", featureRuleListMetricsMap);
						}
				}else if(input.learningAlgorithmType == LearningAlgorithmType.Id3){
					featureRuleListMetricsMap = findID3Signatures();
					optionsStringFeatureRuleListMetricsMap.put("Nominal", featureRuleListMetricsMap);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		chooseBestRuleList(optionsStringFeatureRuleListMetricsMap);
	}
	
	@SuppressWarnings("unchecked")
	private Map<Feature, RuleListMetrics> findID3Signatures(){
		Instances filteredData, trainData, testData = null, data;
		List<String> currentIterationRuleList = new ArrayList<String>();
		List<String> ruleList = new ArrayList<String>();
		
		Metrics metrics;
		boolean isCurrentIterationRulesOk;
		
		float[] weightedAvgMetrics;
		
		Map<Feature, RuleListMetrics> featureRuleListMetricsMap = new HashMap<Feature, RuleListMetrics>();
		Map<String, Metrics> classMetricsMap;

		Enumeration<String> classValueEnumeration;
		
		Attribute classAttribute;
		int classIndex;
		int tp, fp, tn, fn;
		
		int noCVFolds = 10;
		try{
			for(Feature feature : featureNominalWekaInstancesMap.keySet()){
				Classifier id3 = new J48();
				
				data = featureNominalWekaInstancesMap.get(feature);
				
				classMetricsMap = new HashMap<String, Metrics>();
				classAttribute = data.classAttribute();
				classValueEnumeration = classAttribute.enumerateValues();
				while(classValueEnumeration.hasMoreElements()){
					classMetricsMap.put(classValueEnumeration.nextElement().toString(), new Metrics(0,0,0,0,0));
				}
				
				Logger.println("Class Values: "+classMetricsMap.keySet());
				
				trainData = data;
				filteredData = trainData;
				
				ruleList.clear();
				
				while(ruleList.size() < input.noRulesToGenerate && filteredData.numAttributes() > 0){
					isCurrentIterationRulesOk = false;
					attributesInRuleSet.clear();

					Logger
              .println("No. Instances Before Zero Row: "+filteredData.numInstances());
					Logger.println("No Attributes: "+filteredData.numAttributes());

					/*
					 * Check if there are instances with all attribute values
					 * as 0; If so, remove those instances
					 */
					
					filteredData = getNonZeroInstances(filteredData);
					
					Logger
              .println("No. Instances After Zero Row: "+filteredData.numInstances());
					
					id3.buildClassifier(filteredData);
				
					currentIterationRuleList.clear();
					
					currentIterationRuleList.addAll(convertDecisionTreeToRules(id3.toString()));
					
					/*
					 * There are no rules generated in this current iteration; So, exit out of the loop
					 */
					if(attributesInRuleSet.size() == 0)
						break;
					
					/*
					 * Perform Evaluation based on the chosen settings; If
					 * choose automatically, then evaluation would be based on
					 * Cross Validation (default settings)
					 */
					
					weka.classifiers.Evaluation eval = new weka.classifiers.Evaluation(filteredData);
					if(input.evaluationOptions.evaluationOptionType == EvaluationOptionType.TrainingSet){
						eval.evaluateModel(id3, filteredData);

						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;
							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger
                    .println("EM: "+tp+","+fp+","+tn+","+fn+","+filteredData.numInstances());
								
								metrics.add(tp, fp, tn, fn, filteredData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}else if(input.evaluationOptions.evaluationOptionType == EvaluationOptionType.PercentageSplit){
						data.randomize(new Random(1));
						double percent = new Double(input.evaluationOptions.noFoldsPercentageSplitValueStr.trim());
						int trainSize = (int) Math.round(data.numInstances() * percent/100);
						int testSize = data.numInstances()-trainSize;
						trainData = new Instances(data, 0, trainSize);
						testData = new Instances(data, trainSize, testSize);
						
						Classifier id3Temp = new J48();
						
						id3Temp.buildClassifier(trainData);
						//use test set
						eval.evaluateModel(id3Temp, testData);
						
						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;

							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger
                    .println("EM: "+tp+","+fp+","+tn+","+fn+","+testData.numInstances());
								
								metrics.add(tp, fp, tn, fn, testData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}else{
						// by default do cross validation
						
						noCVFolds = new Integer(input.evaluationOptions.noFoldsPercentageSplitValueStr.trim()).intValue();
						if(noCVFolds > filteredData.numInstances())
							noCVFolds = filteredData.numInstances()-1;
						eval.crossValidateModel(id3, filteredData, noCVFolds, new Random(1));
						
						Logger.println("% Correct: "+eval.pctCorrect());
						
						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;

							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger
                    .println("EM: "+tp+","+fp+","+tn+","+fn+","+filteredData.numInstances());
								
								metrics.add(tp, fp, tn, fn, filteredData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}
					
					if(isCurrentIterationRulesOk)
						ruleList.addAll(currentIterationRuleList);
					
					Logger
              .println("Attributes in Rule Set: "+attributesInRuleSet);
					
					filteredData = filterInstancesOnAttributes(attributesInRuleSet, filteredData);
					
					Logger
              .println("No. Attributes After Filter: "+filteredData.numAttributes());
				}
				featureRuleListMetricsMap.put(feature, new RuleListMetrics("", ruleList, classMetricsMap));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return featureRuleListMetricsMap;
	}
	
	/*
	 * Metrics are computed using Weka
	 */
	@SuppressWarnings("unchecked")
	private Map<Feature, RuleListMetrics> findJ48Signatures(String optionsString, Map<Feature, Instances> featureWekaInstancesMap){
		Logger.printCall("Calling findJ48Signatures");
		Attribute classAttribute;
		Instances filteredData, trainData, testData = null, data;
		List<String> ruleList = new ArrayList<String>();
		List<String> currentIterationRuleList = new ArrayList<String>();
		int noCVFolds = 10;
		int tp, tn, fp, fn;
		Enumeration<String> classValueEnumeration;
		int classIndex;
		Metrics metrics;
		boolean isCurrentIterationRulesOk;
		float[] weightedAvgMetrics;
		
		Map<Feature, RuleListMetrics> featureRuleListMetricsMap = new HashMap<Feature, RuleListMetrics>();
		Map<String, Metrics> classMetricsMap;
		
		try{
			for(Feature feature : featureWekaInstancesMap.keySet()){
				Classifier j48 = new J48();
				((J48)j48).setOptions(weka.core.Utils.splitOptions(optionsString));
				
				data = featureWekaInstancesMap.get(feature);
				
				classMetricsMap = new HashMap<String, Metrics>();
				classAttribute = data.classAttribute();
				classValueEnumeration = classAttribute.enumerateValues();
				while(classValueEnumeration.hasMoreElements()){
					classMetricsMap.put(classValueEnumeration.nextElement().toString(), new Metrics(0,0,0,0,0));
				}
				
				Logger.println("Class Values: "+classMetricsMap.keySet());
				
				trainData = data;
				filteredData = trainData;

				ruleList.clear();
				while(ruleList.size() < input.noRulesToGenerate && filteredData.numAttributes() > 0){
					isCurrentIterationRulesOk = false;
					attributesInRuleSet.clear();

					Logger
              .println("No. Instances (before zero row removal): "+filteredData.numInstances());
					Logger.println("No Attributes: "+filteredData.numAttributes());

					/*
					 * Check if there is an instance with all attribute values as 0;
					 * If so, remove that instance
					 */
					
					filteredData = getNonZeroInstances(filteredData);
					
					Logger
              .println("No. Instances (after zero row removal): "+filteredData.numInstances());
					
					currentIterationRuleList.clear();
					
					j48.buildClassifier(filteredData);

					currentIterationRuleList.addAll(convertDecisionTreeToRules(j48.toString()));
					
					/*
					 * There are no rules generated in this current iteration; So, exit out of the loop
					 */
					if(attributesInRuleSet.size() == 0)
						break;
					
					/*
					 * Perform Evaluation based on the chosen settings; If
					 * choose automatically, then evaluation would be based on
					 * Cross Validation (default settings)
					 */
					
					weka.classifiers.Evaluation eval = new weka.classifiers.Evaluation(filteredData);
					if(input.evaluationOptions.evaluationOptionType == EvaluationOptionType.TrainingSet){
						eval.evaluateModel(j48, trainData);

						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;
							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger
                    .println("EM: "+tp+","+fp+","+tn+","+fn+","+filteredData.numInstances());
								
								metrics.add(tp, fp, tn, fn, filteredData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}else if(input.evaluationOptions.evaluationOptionType == EvaluationOptionType.PercentageSplit){
						data.randomize(new Random(1));
						double percent = new Double(input.evaluationOptions.noFoldsPercentageSplitValueStr.trim());
						int trainSize = (int) Math.round(data.numInstances() * percent/100);
						int testSize = data.numInstances()-trainSize;
						trainData = new Instances(data, 0, trainSize);
						testData = new Instances(data, trainSize, testSize);
						
						Classifier j48Temp = new J48();
						((J48)j48Temp).setOptions(weka.core.Utils.splitOptions(optionsString));
						j48Temp.buildClassifier(trainData);
						//use test set
						eval.evaluateModel(j48Temp, testData);
						
						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;

							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger
                    .println("EM: "+tp+","+fp+","+tn+","+fn+","+testData.numInstances());
								
								metrics.add(tp, fp, tn, fn, testData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}else{
						// by default do cross validation
						noCVFolds = new Integer(input.evaluationOptions.noFoldsPercentageSplitValueStr).intValue();
						if(noCVFolds > filteredData.numInstances())
							noCVFolds = filteredData.numInstances()-1;
						eval.crossValidateModel(j48, filteredData, noCVFolds, new Random(1));
						Logger.println("% Correct: "+eval.pctCorrect());
						
						weightedAvgMetrics = getWeightedAverageMetrics(eval.toClassDetailsString());
						
						if(weightedAvgMetrics[4] > threshold){
							isCurrentIterationRulesOk = true;

							for(String classValue : classMetricsMap.keySet()){
								metrics = classMetricsMap.get(classValue);
								classIndex = classAttribute.indexOfValue(classValue);
								tp = (int)eval.numTruePositives(classIndex);
								tn = (int)eval.numTrueNegatives(classIndex);
								fp = (int)eval.numFalsePositives(classIndex);
								fn = (int)eval.numFalseNegatives(classIndex);
								
								Logger
                    .println("EM: "+tp+","+fp+","+tn+","+fn+","+filteredData.numInstances());
								
								metrics.add(tp, fp, tn, fn, filteredData.numInstances());
								classMetricsMap.put(classValue, metrics);
							}
						}
					}
					
					/*
					 * Add the rules generated in this iteration only if its
					 * performance is above the threshold; We consider the
					 * F-measure as the basis metric
					 */
					if(isCurrentIterationRulesOk)
						ruleList.addAll(currentIterationRuleList);
					
					filteredData = filterInstancesOnAttributes(attributesInRuleSet, filteredData);
					
					Logger
              .println("No. Attributes (after filtering attributes involved in rules): "+filteredData.numAttributes());
				}
				
				for(String classValue : classMetricsMap.keySet()){
					metrics = classMetricsMap.get(classValue);
					Logger
              .println(classValue+" @ "+metrics.getTP()+","+metrics.getFP()+","+metrics.getTN()+","+metrics.getFN()+","+metrics.getNoInstances());
				}
				featureRuleListMetricsMap.put(feature, new RuleListMetrics(optionsString, ruleList, classMetricsMap));
			}
//			buildResultsPanel(ruleList);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		Logger.printReturn("Calling findJ48Signatures");
		return featureRuleListMetricsMap;
	}
	
	/**
	 * This method removes the set of attributes (and the corresponding values)
	 * passed in attributeNameSet from the data instances. If the original
	 * number of attributes are N then the filtered data will be N-M where M is
	 * the number of attributes in attributeNameSet
	 * 
	 * @param attributeNameSet
	 * @param data
	 * @return
	 */
	private Instances filterInstancesOnAttributes(Set<String> attributeNameSet, Instances data){
		Logger.printCall("Calling filterInstancesOnAttributes()");
		Instances filteredData = data;
		try {
			Set<Integer> attributeIndicesSet = new HashSet<Integer>();

			for (int i = 0; i < data.numAttributes(); i++) {
				if (attributeNameSet.contains(data.attribute(i).name())) {
					attributeIndicesSet.add(i);
				}
			}

			int[] attributeIndicesArray = new int[attributeIndicesSet.size()];
			int index = 0;
			for (Integer attributeIndex : attributeIndicesSet)
				attributeIndicesArray[index++] = attributeIndex;

			Remove remove = new Remove();
			remove.setAttributeIndicesArray(attributeIndicesArray);
			remove.setInputFormat(data);
			filteredData = Filter.useFilter(data, remove);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//Logger.printReturn("Returning filterInstancesOnAttributes()");
		return filteredData;
	}
	
	/**
	 * This method filters any instance whose attribute values are all zeros. 
	 * @param data
	 * @return
	 */
	private Instances getNonZeroInstances(Instances data){
		Logger.printCall("Calling getNonZeroInstances()");
		Instance instance;
		boolean zeroRowExists = false;
		boolean isZeroRow;
		do{
			zeroRowExists = false;
			for(int i = 0; i < data.numInstances(); i++){
				instance = data.instance(i);
				isZeroRow = true;
				for(int j = 0; j < instance.numAttributes(); j++){
					if(instance.value(j) != 0){
						isZeroRow = false;
						break;
					}
				}
				if(isZeroRow){
					data.delete(i);
					zeroRowExists = true;
					break;
				}
			}
		}while(zeroRowExists);
		
		//Logger.printReturn("Returning getNonZeroInstances()");
		return data;
	}
	
	private List<String> convertDecisionTreeToRules(String tree){
		Logger.printCall("Calling convertDecisionTreeToRules()");
		Logger.println(tree);

		String[] treeSplit = tree.replaceAll(" ", "").split("[\r\n\b]+");
		
		Map<Integer, String> levelConstriantMap = new HashMap<Integer, String>();
		int depth;
		String[] ruleSplit;
		List<String> ruleList = new ArrayList<String>();
		String constraint, rule;
		int startIndex, endIndex;
		String classLabel;
		if(tree.contains("J48")){
			//The last two rows in treeSplit will give details about number of leaves and size of tree the first two lines will contain J48 (un)pruned and ------
			//
			startIndex = 2;
			endIndex = treeSplit.length-1;
		}else if(tree.contains("Id3")){
			startIndex = 1;
			endIndex = treeSplit.length;
		}else{
			startIndex = 0;
			endIndex = treeSplit.length;
		}
		
		for(int i = startIndex; i < endIndex; i++){
//			System.out.println(treeSplit[i]);
			if(treeSplit[i].contains("Number"))
				break;
			if(treeSplit[i].contains("|")){
				depth = treeSplit[i].split("\\|").length;
//				System.out.println("Depth: "+depth);
				if(treeSplit[i].contains(":")){
					ruleSplit = treeSplit[i].split("\\|")[depth-1].split(":");
					classLabel = ruleSplit[1].split("\\(")[0].trim();
					if(input.generateRulesForClassLabelSet.contains(classLabel)){
						rule = "IF ";
						for(int j = 0; j < depth-1; j++){
							rule += levelConstriantMap.get(j)+" AND ";
						}
						if(ruleSplit[0].contains("="))
							attributesInRuleSet.add(ruleSplit[0].split("=")[0]);
						else if(ruleSplit[0].contains("<"))
							attributesInRuleSet.add(ruleSplit[0].split("<")[0]);
						else if(ruleSplit[0].contains(">"))
							attributesInRuleSet.add(ruleSplit[0].split(">")[0]);
						
						rule += ruleSplit[0]+" THEN "+classLabel;
	//						System.out.println("RULE: "+rule);
						ruleList.add(rule);
					}
				}else{
					constraint = treeSplit[i].split("\\|")[depth-1];
					levelConstriantMap.put(depth-1, constraint);
					if(constraint.contains("="))
						attributesInRuleSet.add(constraint.split("=")[0]);
					else if(constraint.contains("<"))
						attributesInRuleSet.add(constraint.split("<")[0]);
					else if(constraint.contains(">"))
						attributesInRuleSet.add(constraint.split(">")[0]);
//					System.out.println("Constraint: "+constraint);
				}
			}else{
				if(treeSplit[i].contains(":")){
					//A rule with just one constraint (no depth of tree involved with |)
					ruleSplit = treeSplit[i].split(":");
					classLabel = ruleSplit[1].split("\\(")[0].trim();
					if(input.generateRulesForClassLabelSet.contains(classLabel)){
						ruleList.add("IF "+ruleSplit[0]+" THEN "+classLabel);
						if(ruleSplit[0].contains("="))
							attributesInRuleSet.add(ruleSplit[0].split("=")[0]);
						else if(ruleSplit[0].contains("<"))
							attributesInRuleSet.add(ruleSplit[0].split("<")[0]);
						else if(ruleSplit[0].contains(">"))
							attributesInRuleSet.add(ruleSplit[0].split(">")[0]);
					}
				}else{
					constraint = treeSplit[i];
					levelConstriantMap.put(0, treeSplit[i]);
					if(constraint.contains("="))
						attributesInRuleSet.add(constraint.split("=")[0]);
					else if(constraint.contains("<"))
						attributesInRuleSet.add(constraint.split("<")[0]);
					else if(constraint.contains(">"))
						attributesInRuleSet.add(constraint.split(">")[0]);
				}
			}
		}

		//Logger.printReturn("Returning convertDecisionTreeToRules()");
		return ruleList;
	}
	
	private float[] getWeightedAverageMetrics(String classDetailEvaluationString){
		Logger.printCall("Calling getWeightedAverageMetrics()");
		float[] weightedAverageMetrics = new float[6];
		
		classDetailEvaluationString = classDetailEvaluationString.split("Weighted Avg.")[1].trim();
		String[] classDetailEvaluationStringSplit = classDetailEvaluationString.split(" ");
		int noSplits = classDetailEvaluationStringSplit.length;
		int currentIndex = 0;
		for(int i = 0; i < noSplits; i++){
			if(classDetailEvaluationStringSplit[i].trim().equalsIgnoreCase(""))
				continue;
			else{
				weightedAverageMetrics[currentIndex] = new Float(classDetailEvaluationStringSplit[i].trim()).floatValue();
				currentIndex++;
			}
		}
		
		//Logger.printReturn("Returning getWeightedAverageMetrics()");
		return weightedAverageMetrics;
	}
	
	private Map<Feature, RuleListMetrics> findAssociationRules(){
//		System.out.println("In findAssociation Rules");
		float minSupport = new Float(input.getAssociationRuleSettings().getMinSupportValueStr()).floatValue();
		float minConfidence = new Float(input.getAssociationRuleSettings().getSortRulesMetricValueStr()).floatValue();
		ClassSpecificAssociationRuleMiner c = new ClassSpecificAssociationRuleMiner(featureNominalWekaInstancesMap, input.getGenerateSignaturesForClassLabelSet(), minSupport, minConfidence);
		return c.getFeatureRuleListMetrics();
	}
	
	private Map<Feature, RuleListMetrics> findAssociationRules(float minSupport, float minConfidence){
//		System.out.println("In findAssociation Rules");
		ClassSpecificAssociationRuleMiner c = new ClassSpecificAssociationRuleMiner(featureNominalWekaInstancesMap, input.getGenerateSignaturesForClassLabelSet(), minSupport, minConfidence);
		return c.getFeatureRuleListMetrics();
	}
	
	private String getParameterOptions(){
		String optionsString = "";
		if(input.learningAlgorithmType == LearningAlgorithmType.J48){
			if(input.j48Settings.isPruneTrees){
				if(input.j48Settings.isPessimisticErrorPruning){
					optionsString = optionsString.concat("-C ").concat(input.j48Settings.confidenceFactorFoldsStr).concat(" -M 1");
				}else{
					optionsString = optionsString.concat("-R ").concat("-N ").concat(input.j48Settings.confidenceFactorFoldsStr).concat(" -Q 1 -M 1");
				}
			}else{
				optionsString = optionsString.concat("-U -M 1");
			}
		}else if(input.learningAlgorithmType == LearningAlgorithmType.WekaAssociationRules){
			optionsString = optionsString.concat("-N ").concat(input.noRulesToGenerate+" ");
			optionsString = optionsString.concat("-T ").concat(input.associationRuleSettings.sortRulesMetricStr);
			optionsString = optionsString.concat("-C ").concat(input.associationRuleSettings.sortRulesMetricValueStr);
			optionsString = optionsString.concat("-D 0.05 ");
			optionsString = optionsString.concat("-U ").concat(input.associationRuleSettings.maxSupportValueStr);
			optionsString = optionsString.concat("-M ").concat(input.associationRuleSettings.minSupportValueStr);
			if(input.associationRuleSettings.isClassAssociationRules){
				optionsString = optionsString.concat("-A ");
			}
			//Indicate that the last attribute is the one that holds the class label
			optionsString = optionsString.concat("-c -1");
		}else if(input.learningAlgorithmType == LearningAlgorithmType.AssociationRules){
			optionsString = optionsString.concat("-minSupport ").concat(input.associationRuleSettings.getMinSupportValueStr());
			optionsString = optionsString.concat("-minConfidence ").concat(input.associationRuleSettings.getSortRulesMetricValueStr());
		}
		//Logger.printReturn("Returning getParameterOptions()");
		return optionsString;
	}

	/*
	 * Bruce 29.05.2014
	 * Add precision, recall and accuracy
	 */
	private void chooseBestRuleList(Map<String, Map<Feature, RuleListMetrics>> optionsStringFeatureRuleListMetricsMap){
		Map<Feature, RuleListMetrics> featureRuleListMetricsMap;
		RuleListMetrics ruleListMetrics;
		Metrics metrics;
		double f1Score;
		Map<String, Metrics> classMetricsMap;
		
		maxOptionsString="null";
		maxClass = "";
		maxFeature = Feature.None;
		maxF1Score = 0;
		
		for(String optionsString : optionsStringFeatureRuleListMetricsMap.keySet()){
			featureRuleListMetricsMap = optionsStringFeatureRuleListMetricsMap.get(optionsString);
			for(Feature feature : featureRuleListMetricsMap.keySet()){
				
				ruleListMetrics = featureRuleListMetricsMap.get(feature);
				classMetricsMap = ruleListMetrics.getClassMetricsMap();
				for(String classValue : classMetricsMap.keySet()){
					metrics = classMetricsMap.get(classValue);
					f1Score = metrics.getF1Score();
					if(f1Score > maxF1Score){
						maxOptionsString = optionsString;
						maxFeature = feature;
						maxF1Score = f1Score;
						maxClass = classValue;
						
						precisionPerF1Score = metrics.precision();
						accuracyPerF1Score = metrics.accuracy();
						recallPerF1Score = metrics.truePositiveRate();
					}
				}
			}
		}
		
//		System.out.println("Max F1 Score: "+maxF1Score);
//		System.out.println("Max Options String: "+maxOptionsString);
//		System.out.println("Max FeatureType: "+maxFeature);
		
		if(maxOptionsString.equals("null") || maxFeature.equals(
        Feature.None)){
			hasSignatures = false;
			return;
		}
		
		featureRuleListMetricsMap = optionsStringFeatureRuleListMetricsMap.get(maxOptionsString);
		ruleListMetrics = featureRuleListMetricsMap.get(maxFeature);
		finalRuleList = ruleListMetrics.getRuleList();
		prepareEncodedDecodedRuleListMap();
		
		
		if(maxOptionsString.contains("Nominal"))
			evaluateRuleList(finalRuleList, featureNominalWekaInstancesMap.get(maxFeature));
		else
			evaluateRuleList(finalRuleList, featureNumericWekaInstancesMap.get(maxFeature));
		classMetricsMap = ruleListMetrics.getClassMetricsMap();
		metrics = classMetricsMap.get(maxClass);
	}
	
	private void prepareEncodedDecodedRuleListMap(){
		if(encodedDecodedRuleMap == null){
			encodedDecodedRuleMap = new HashMap<String, String>();
		}else{
			encodedDecodedRuleMap.clear();
		}
		
		String[] ruleSplit; 
		String[] antecedantSplit;

		StringBuilder decodedRuleStringBuilder = new StringBuilder();
		for(String encodedRule : finalRuleList){
			
			ruleSplit = encodedRule.replaceAll("IF ", "").split(" THEN ");
			antecedantSplit = ruleSplit[0].split(" AND ");
//			System.out.println(encodedRule+"@"+antecedantSplit.length);
			decodedRuleStringBuilder.setLength(0);
			decodedRuleStringBuilder.append("IF ");
			for(int i = 0; i < antecedantSplit.length; i++){
				if(antecedantSplit[i].contains(">=")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],">="));
				}else if(antecedantSplit[i].contains(">")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],">"));
				}else if(antecedantSplit[i].contains("<=")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],"<="));
				}else if(antecedantSplit[i].contains("<")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],"<"));
				}else if(antecedantSplit[i].contains("=")){
					decodedRuleStringBuilder.append(getDecodedAntecedant(antecedantSplit[i],"="));
				}
				
				if(i < antecedantSplit.length-1){
					decodedRuleStringBuilder.append(" AND ");
				}
			}
			
			decodedRuleStringBuilder.append(" THEN ").append(ruleSplit[1]);
			
			encodedDecodedRuleMap.put(encodedRule, decodedRuleStringBuilder.toString());
		}
	}
	
	private String getDecodedAntecedant(String antecedant, String constraint){
		StringBuilder decodedAntecedant = new StringBuilder();
		String[] featureSplit = antecedant.split(constraint);
		String[] activitySplit = featureSplit[0].replaceAll("\\[", "").replaceAll("\\]", "").split("_");
		
		if(antecedant.contains("["))
			decodedAntecedant.append("[");
		int noActivites = activitySplit.length;
		int index = 0;
		int featureLength;
		for(String encodedActivity : activitySplit){
			if(encodedActivity.length() == encodingLength){
				decodedAntecedant.append(charActivityMap.get(encodedActivity.trim()));
				index++;
				if(index < noActivites)
					decodedAntecedant.append(", ");
			}else{
				//Sequence Feature
				featureLength = encodedActivity.length()/encodingLength;
				for(int i = 0; i < featureLength; i++){
					decodedAntecedant.append(charActivityMap.get(encodedActivity.substring(i*encodingLength, (i+1)*encodingLength)));
				}
			}
		}	
		
		if(antecedant.contains("]"))
			decodedAntecedant.append("]");
		decodedAntecedant.append(constraint).append(featureSplit[1]);
		
		return decodedAntecedant.toString();
	}
	
	/* 
	 * Compute metrics for all rules
	 * Input:
	 * ruleList: contains all rules in form of string with rule format: IF xx THEN label
	 * data: contains all instances in Weka format for the best feature type (TR,MR..)
	 * Output:
	 * finalRuleListMetricsMap: map from rule to Metrics object for each rule
	 * Metrics is computed for each rule as follows:
	 * Scan all instances of the log
	 * At each instance, compare instance attribute value with the attribute value extracted from
	 * the rule attributes.
	 * Positive: means the instance satisfies the rule
	 * True positive: means the instance satisfies the rule and has the same label
	 * False positive: means the instance satisfies the rule and has different label
	 * Negative: means the instance does not satisfy the rule
	 * True negative: means the instance does not satisfy the rule and has different label
	 * False negative: means the instance does not satisfy the rule and has the same label
	 */
	private void evaluateRuleList(List<String> ruleList, Instances data){
		Logger.printCall("Calling evaluateRuleList()");
		
		if(finalRuleListMetricsMap == null){
			finalRuleListMetricsMap = new HashMap<String, Metrics>();
		}else{
			finalRuleListMetricsMap.clear();
		}
		
		int noInstances = data.numInstances();
		Instance instance;
		
		String[] ruleSplit;
		String[] antecedantSplit, antecedantContraintSplit;

		int noAntecedants;
		List<String> antecedantAttributeList = new ArrayList<String>();
		List<String> antecedantConstraintList = new ArrayList<String>();
		double numericValue;
		String nominalValue;
		boolean isAntecedantConstraintsSatisfied;
		List<Integer> ruleSatisifyingInstanceList = new ArrayList<Integer>();
		List<Integer> constraintSatisifyingInstanceList = new ArrayList<Integer>();
		
		Logger
        .println("No. Instances: "+data.numInstances()+" @ No.Rules: "+ruleList.size());
		
		int noInstancesWithRuleClassValue;
		int tp, fp, tn, fn;
		
		/*
		 * Compute metrics for every rule
		 */
		for(String rule : ruleList){
			
			/*
			 * Build antecedents of the rule, i.e.
			 * Example of rule: 
			 * IF antecedentAttribute1 >= antecedentConstraint1 AND antecedentAttribute2 < antecedentConstraint2 
			 * 		THEN <Label>
			 * antecedantSplit: all pairs [antecedentAttribute,antecedentConstraint] 
			 * antecedantAttributeList: all antecedent attributes [antecedentAttribute,....]
			 * antecedantConstraintList: all antecedent constraint [antecedentConstraint,...]
			 */
			ruleSplit = rule.replaceAll("IF ", "").split(" THEN ");
			antecedantSplit = ruleSplit[0].split(" AND ");
			noAntecedants = antecedantSplit.length;
			
			Logger.println("Rule: "+rule);
			Logger.println("No. Antecedants: "+noAntecedants);
			
			antecedantAttributeList.clear();
			antecedantConstraintList.clear();
			
			for(String antecedant : antecedantSplit){
				if(antecedant.contains(">=")){
					antecedantContraintSplit = antecedant.split(">=");
				}else if(antecedant.contains("<=")){
					antecedantContraintSplit = antecedant.split("<=");
				}else if(antecedant.contains("=")){
					antecedantContraintSplit = antecedant.split("=");
				}else if(antecedant.contains(">")){
					antecedantContraintSplit = antecedant.split(">");
				}else{
					antecedantContraintSplit = antecedant.split("<");
				}
				
				Logger
            .println(antecedant+"@("+antecedantContraintSplit[0].trim()+","+data.attribute(antecedantContraintSplit[0].trim()).toString()+")@"+antecedantContraintSplit[1]);
				
				antecedantAttributeList.add(antecedantContraintSplit[0].trim());
				antecedantConstraintList.add(antecedantContraintSplit[1].trim());
			}
			
			Logger.println("Consequent: "+ruleSplit[1]);
			noInstancesWithRuleClassValue = 0;
			
			constraintSatisifyingInstanceList.clear();
			ruleSatisifyingInstanceList.clear();
			fn = tn = tp = fp = 0;
			
			/*
			 * Scan the whole log to compute metrics of one rule
			 * Scan every instance, at each instance, get the attribute value based on the attribute name,
			 * compare that value with the antecedent constraint. If satisfied, check the next attribute.
			 * If one of the attribute is not satisfied: the whole antecedent is not satisfied. 
			 */
			for(int i = 0; i < noInstances; i++){
				instance = data.instance(i);
				
				if(instance.stringValue(instance.classAttribute()).equals(ruleSplit[1].trim()))
					noInstancesWithRuleClassValue++;
				
				isAntecedantConstraintsSatisfied = true;
				for(int j = 0; j < noAntecedants; j++){
					if(antecedantSplit[j].contains(">=")){
						numericValue = instance.value(data.attribute(antecedantAttributeList.get(j)));
						if(!(numericValue >= new Double(antecedantConstraintList.get(j)).doubleValue())){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}else if(antecedantSplit[j].contains("<=")){
						numericValue = instance.value(data.attribute(antecedantAttributeList.get(j)));
						if(!(numericValue <= new Double(antecedantConstraintList.get(j)).doubleValue())){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}else if(antecedantSplit[j].contains(">")){
						numericValue = instance.value(data.attribute(antecedantAttributeList.get(j)));
						if(!(numericValue > new Double(antecedantConstraintList.get(j)).doubleValue())){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}else if(antecedantSplit[j].contains("<")){
						numericValue = instance.value(data.attribute(antecedantAttributeList.get(j)));
						if(!(numericValue < new Double(antecedantConstraintList.get(j)).doubleValue())){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}else{
						//Nominal attributes have only constraints as =
						nominalValue = instance.stringValue(data.attribute(antecedantAttributeList.get(j)));
//						System.out.println(antecedantAttributeList.get(j)+"@"+data.attribute(antecedantAttributeList.get(j))+"@ Nominal value: "+nominalValue+"@ Cons: "+antecedantConstraintList.get(j));
						if(!nominalValue.equals(antecedantConstraintList.get(j))){
							isAntecedantConstraintsSatisfied = false;
							break;
						}
					}
				}
				
				/*
				 * Check label to determine TP/FP/TN/FN
				 * Positive: means the instance satisfies the rule
				 * True positive: means the instance satisfies the rule and has the same label
				 * False positive: means the instance satisfies the rule and has different label
				 * 
				 * Negative: means the instance does not satisfy the rule
				 * True negative: means the instance does not satisfy the rule and has different label
				 * False negative: means the instance does not satisfy the rule and has the same label
				 */
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
			
			Logger
          .println("No. Constraints Satisfying Instances: "+constraintSatisifyingInstanceList.size());
			Logger
          .println("Constraint Satisfying Instance List: "+constraintSatisifyingInstanceList);
			Logger
          .println("No. Rule Satisfying Instances: "+ruleSatisifyingInstanceList.size());
			Logger
          .println("Rule Satisfying Instance List: "+ruleSatisifyingInstanceList);
			finalRuleListMetricsMap.put(rule, new Metrics(tp,fp,tn,fn,noInstances));
		}
		//Logger.printReturn("Returning evaluateRuleList()");
	}
	

	public List<String> getFinalRuleList() {
		return finalRuleList;
	}

	public Map<String, Metrics> getFinalRuleListMetricsMap() {
		return finalRuleListMetricsMap;
	}

	public Map<String, String> getEncodedDecodedRuleMap() {
		return encodedDecodedRuleMap;
	}

	public String getMaxOptionsString() {
		return maxOptionsString;
	}

	public Feature getMaxFeature() {
		return maxFeature;
	}

	public double getMaxF1Score() {
		return maxF1Score;
	}
	
	public double getPrecisionPerMaxF1Score() {
		return precisionPerF1Score;
	}	
	
	public double getAccuracyPerMaxF1Score() {
		return accuracyPerF1Score;
	}	
	
	public double getRecallPerMaxF1Score() {
		return recallPerF1Score;
	}	
	
	public boolean hasSignatures(){
		return hasSignatures;
	}
}
