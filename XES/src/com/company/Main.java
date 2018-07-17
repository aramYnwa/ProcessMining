package com.company;

import com.company.ML.DecisionTreeJ48;
import com.company.feature_extraction.encoding.*;
import com.company.xlog.XLogHandler;
import com.company.xlog.XLogReader;
import java.io.File;
import java.util.List;
import org.deckfour.xes.model.*;

import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.xstream.XLogConverter;
import org.processmining.plugins.signaturediscovery.DiscoverSignatures;
import org.processmining.plugins.signaturediscovery.SignatureDiscoveryInput;
import weka.core.Instances;
import weka.core.converters.ArffSaver;


public class Main {

	public static void main(String args[]) {

		try {

			XLog log = XLogReader.openLog("logs/go10000.xes");
			//XLog log = XLogReader.openLog("logs/hospital_log.xes");
			XLogManager logManager = new XLogManager(log);

/*
			for (XTrace trace : log) {

				trace.getAttributes().put("Label", new XAttributeLiteralImpl("Label", "value"));
				System.out.println(trace.getAttributes());
			}

			SignatureDiscoveryInput input = new SignatureDiscoveryInput();
			input.removeAllFeatures();
			input.addFeature("Tandem Repeat");
			input.addFeature("Maximal Repeat");
			input.addFeature("Tandem Repeat Alphabet");
			input.addFeature("Maximal Repeat Alphabet");
			DiscoverSignatures discoverSignatures = new DiscoverSignatures(log,log, 0.5, 3, input, true);
*/

			SequentalPattern sequentalPattern = new SequentalPattern(log);
			IndividualActivityEncoder encoder = new IndividualActivityEncoder(log, EncodingType.FREQUENCY);
			//SetBasedEncoder encoder = new SetBasedEncoder(log, EncodingType.FREQUENCY);
			encoder.encodeTraces(log);
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
