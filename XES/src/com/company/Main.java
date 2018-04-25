package com.company;

import com.company.serializers.AssosiationBasedBinarySerializer;
import com.company.serializers.AssosiationBasedSerializer;
import com.company.serializers.BinarySerializer;
import com.company.serializers.FrequencySerializer;
import com.company.serializers.IndividualActivtyBasedSerializer;
import com.company.weka.api.AprioriMethod;
import com.company.weka.api.ClassifyLog;
import com.company.xlog.XLogHandler;
import com.company.xlog.XLogReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.deckfour.xes.model.*;



public class Main {

	public static void main(String args[]) {

		try {

			ClassifyLog classifier =  new ClassifyLog();
			XLog log = XLogReader.openLog("hospital_log.xes");

			XLogHandler handler = new XLogHandler(log, "Hospital");


			AssosiationBasedSerializer assS = new AssosiationBasedSerializer(handler);
			assS.serialize();

			AprioriMethod aprioriMethod = new AprioriMethod(0.5, assS.getArffPath());
			ArrayList<ArrayList<String>> frequentItemsets = aprioriMethod.findFrequentItemsets();

			AssosiationBasedBinarySerializer abbs = new AssosiationBasedBinarySerializer(handler, frequentItemsets);
			abbs.serialize();

			classifier.classify(abbs);

	
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
