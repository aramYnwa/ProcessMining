package com.company;

import org.deckfour.xes.model.*;

public class Main {

	public static void main(String args[]) {

		try {

			XLog log = XLogReader.openLog("hospital_log.xes");

			AttributeDictionary dict = new AttributeDictionary("Hospital", log);

			XesSerializeToArff serialize = new XesSerializeToArff(dict, log);
			serialize.binarySerialize();

			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
