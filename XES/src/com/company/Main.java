package com.company;

import com.company.ML.DecisionTreeJ48;
import com.company.feature_extraction.SignatureDiscovery.DiscoverSignatures;
import com.company.feature_extraction.SignatureDiscovery.SignatureDiscoveryInput;
import com.company.feature_extraction.encoding.EncodingType;
import com.company.feature_extraction.encoding.SetBasedEncoder;
import com.company.feature_extraction.encoding.TransactionBasedEncoder;
import com.company.xlog.XLogReader;
import java.io.File;
import org.deckfour.xes.model.*;
import weka.core.Instances;
import weka.core.converters.ArffSaver;


public class Main {

	public static void main(String args[]) {

		try {
			//XLog log = XLogReader.openLog("logs/hospital_log.xes");
			XLog log = XLogReader.openLog("logs/sepsis_cases.xes");

			SignatureDiscoveryInput input = new SignatureDiscoveryInput();
			input.removeAllFeatures();
			input.addFeature("Tandem Repeat");
			input.addFeature("Maximal Repeat");
			input.addFeature("Tandem Repeat Alphabet");
			input.addFeature("Maximal Repeat Alphabet");
			DiscoverSignatures discoverSignatures = new DiscoverSignatures(log, input);

			discoverSignatures.getFinalRuleList();
			//IndividualActivityEncoder encoder = new IndividualActivityEncoder(log, EncodingType.FREQUENCY);
			SetBasedEncoder encoder = new SetBasedEncoder(log, EncodingType.BINARY);
			encoder.encodeTraces();
			Instances instances = encoder.getEncodedTraces();
			writeInstancesToFile(instances);

			DecisionTreeJ48 j48 = new DecisionTreeJ48(instances);
			j48.classify();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void writeInstancesToFile(Instances instances) {
		try {
			ArffSaver saver = new ArffSaver();
			saver.setInstances(instances);
			saver.setFile(new File("arffFiles/a1.arff"));
			saver.writeBatch();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
