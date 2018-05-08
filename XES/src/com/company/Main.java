package com.company;

import com.company.ML.DecisionTreeJ48;
import com.company.feature_extraction.encoding.SequenceBasedEncoder;
import com.company.feature_extraction.encoding.SetBasedEncoder;
import com.company.feature_extraction.encoding.SetBasedBinaryEncoder;
import com.company.feature_extraction.encoding.XLogManager;
import com.company.xlog.XLogReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import org.deckfour.xes.model.*;
import weka.associations.Apriori;
import weka.core.Instances;
import weka.core.converters.ArffSaver;


public class Main {

	public static void main(String args[]) {

		try {

			XLog log = XLogReader.openLog("logs/hospital_log_cut.xes");

			//FrequencyBasedEncoder frequencyBasedEncoder = new FrequencyBasedEncoder(log);
			SetBasedEncoder frequencyBasedEncoder = new SetBasedEncoder(log);
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

			//SetBasedBinaryEncoder encoder = new SetBasedBinaryEncoder(manager, frequentItemsets);
			SequenceBasedEncoder encoder = new SequenceBasedEncoder(manager, frequentItemsets);
			encoder.encodeTraces();

			instances = encoder.getEncodedTraces();
			ArffSaver saver = new ArffSaver();
			saver.setInstances(instances);
			saver.setFile(new File("arffFiles/a1.arff"));
			saver.writeBatch();

			DecisionTreeJ48 j48 = new DecisionTreeJ48(instances);
			j48.classify();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
