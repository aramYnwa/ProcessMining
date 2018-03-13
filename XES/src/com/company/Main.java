package com.company;

import java.util.Date;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.*;

public class Main {
	
	public static void main (String args[])  {

		try {
			XLog log = XLogReader.openLog("hospital_log.xes");

			AttributeDictionary dict = new AttributeDictionary("Hospital", log);

			XesSerializeToArff serialize = new XesSerializeToArff(dict, log);
			serialize.BinarySerialize();

			//ConstraintConditions formula = ConstraintConditions.build("[A.x>0][A.y==T.y][2,6,d]");
			
			//LTLRule rule1 = new LTLRule(DeclareTemplate.Response, formula, "Task1", "Task2");
			
			/*for(XTrace trace:log){
				String traceName = XConceptExtension.instance().extractName(trace);
				System.out.println("TraceName: "+traceName);
				XAttributeMap caseAttributes = trace.getAttributes();
				for(XEvent event : trace){
					String activityName = XConceptExtension.instance().extractName(event);
					System.out.println("ActivityName: "+activityName);
					Date timestamp = XTimeExtension.instance().extractTimestamp(event);
					System.out.println("Timestamp: "+timestamp);
					String eventType = XLifecycleExtension.instance().extractTransition(event);
					XAttributeMap eventAttributes = event.getAttributes();
					for(String key :eventAttributes.keySet()){
						String value = eventAttributes.get(key).toString();
						System.out.println("key: "+key+"  value: "+value);
					}
					for(String key :caseAttributes.keySet()){
						String value = caseAttributes.get(key).toString();
						System.out.println("key: "+key+"  value: "+value);
					}
					
					//Invokation of the Planner
					//Create an arraylist of adds  and  deletes using the same order in which they have to be applied starting by the original trace -- IT IS IMPORTANT TO KEEP THE SAME ORDER
					XFactory factory = XFactoryRegistry.instance().currentDefault();
					XEvent eventToAdd = factory.createEvent();
					
					
					//activity, event type and timestamp mandatory  activity name is a string (name in the example)  and the timestamp must be provided as a long (120000 in the example)  event type is always "complete"
					XConceptExtension.instance().assignName(eventToAdd, "name");
					XTimeExtension.instance().assignTimestamp(eventToAdd, 120000);
					XLifecycleExtension.instance().assignStandardTransition(eventToAdd, StandardModel.COMPLETE);
					
					
					
					// needed if we consider data;
					XAttributeMap  eventAttributesMap = factory.createAttributeMap();
					// specificare attributi all'evento da aggiungere
					
					//Add add = new Add(0,1,eventToAdd);
					//Delete delete = new Delete(0);
					//ArrayList<Action> actions = new ArrayList<Action>();
					//actions.add(add);
					//actions.add(delete);
					// in the right order
					
					//ArrayList<Alignment> output = new ArrayList<Alignment>();
				}
				
			
			}
			
			//System.out.println("Formula: "+formula);
			System.out.println("First Trace in log: "+XConceptExtension.instance().extractName(log.get(0)));
			
			
			*/
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
