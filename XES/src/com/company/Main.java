package com.company;

import com.company.feature_extraction.encoding.AssociationBasedEncoder;
import com.company.feature_extraction.encoding.AssociationBasedFrequencyEncoder;
import com.company.feature_extraction.encoding.XLogManager;
import com.company.weka.api.ClassifyLog;
import com.company.xlog.XLogReader;
import java.io.File;
import java.util.ArrayList;
import org.deckfour.xes.model.*;
import weka.associations.Apriori;
import weka.core.Instances;
import weka.core.converters.ArffSaver;


public class Main {

	public static void main(String args[]) {

		try {

			XLog log = XLogReader.openLog("sepsis_cases.xes");

			//FrequencyBasedEncoder frequencyBasedEncoder = new FrequencyBasedEncoder(log);
			AssociationBasedEncoder frequencyBasedEncoder = new AssociationBasedEncoder(log);
			frequencyBasedEncoder.encodeTraces(log);

			Instances instances = frequencyBasedEncoder.getEncodedTraces();

			Apriori apriori = new Apriori();

			String[] options = new String[2];
			options[0] = "-S";
			options[1] = "0.8";
			apriori.setOptions(options);
			apriori.buildAssociations(instances);

			ArrayList<ArrayList<String>> frequentItemsets = apriori.getM_LsToString();
			XLogManager manager = new XLogManager(log);
			AssociationBasedFrequencyEncoder encoder = new AssociationBasedFrequencyEncoder(manager,
					frequentItemsets);
			encoder.encodeTraces();

			instances = encoder.getEncodedTraces();
			ArffSaver saver = new ArffSaver();
			saver.setInstances(instances);
			saver.setFile(new File("aa.arff"));
			saver.writeBatch();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
