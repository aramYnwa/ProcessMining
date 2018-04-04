package com.company;

import com.company.serializers.AssosiationBasedSerializer;
import com.company.serializers.FrequencySerializer;
import com.company.serializers.IndividualActivtyBasedSerializer;
import com.company.weka.api.AprioriMethod;
import com.company.weka.api.ClassifyLog;
import com.company.xlog.XLogHandler;
import com.company.xlog.XLogReader;
import org.deckfour.xes.model.*;


public class Main {

	public static void main(String args[]) {

		try {

			ClassifyLog classifier =  new ClassifyLog();
			XLog log = XLogReader.openLog("hospital_log.xes");

			XLogHandler handler = new XLogHandler(log, "Hospital");


			/*IndividualActivtyBasedSerializer binS = new BinarySerializer(handler);
			IndividualActivtyBasedSerializer freqS = new FrequencySerializer(handler);
			freqS.serialize();

			classifier.classify(freqS);*/

			AssosiationBasedSerializer assS = new AssosiationBasedSerializer(handler);
			assS.serialize();

			AprioriMethod aprioriMethod = new AprioriMethod(0.5, assS.getArffPath());
			aprioriMethod.findFrequentItemsets();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
