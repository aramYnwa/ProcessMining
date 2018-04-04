package com.company;

import com.company.classifiers.ArffLogClassifier;
import com.company.classifiers.AssosiationBasedClassifier;
import com.company.classifiers.BinaryClassifier;
import com.company.classifiers.FrequencyClassifier;
import com.company.classifiers.IndividualActivtyBasedClassifier;
import com.company.weka.api.ClassifyLog;
import com.company.xlog.XLogHandler;
import javax.naming.InitialContext;
import org.deckfour.xes.model.*;
import sun.awt.X11.XLayerProtocol;

public class Main {

	public static void main(String args[]) {

		try {
			ClassifyLog classifier =  new ClassifyLog();

			XLog log = XLogReader.openLog("hospital_log.xes");

			System.out.println(log.size());
			//AttributeDictionary dict = new AttributeDictionary("Hospital", log);

			//XesSerializeToArff serialize = new XesSerializeToArff(dict, log);

			/*serialize.serialize(SerializationType.BINARY);
			classifier.run();*/

			//serialize.serialize(SerializationType.FREQUENCY);
			//classifier.run();



//			XLog log = XLogReader.openLog("hospital_log.xes");


			XLogHandler handler = new XLogHandler(log, "Hospital");


			ArffLogClassifier classA = new AssosiationBasedClassifier(handler);
			classA.serialize();

			classifier.experement();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
