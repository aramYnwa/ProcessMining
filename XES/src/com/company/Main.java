package com.company;

import com.company.weka.api.ClassifyLog;
import org.deckfour.xes.model.*;

public class Main {

	public static void main(String args[]) {

		try {

			ClassifyLog classifier =  new ClassifyLog();
			XLog log = XLogReader.openLog("hospital_log.xes");

			AttributeDictionary dict = new AttributeDictionary("Hospital", log);

			XesSerializeToArff serialize = new XesSerializeToArff(dict, log);

			serialize.serialize(SerializationType.BINARY);
			classifier.run();

			serialize.serialize(SerializationType.FREQUENCY);
			classifier.run();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
