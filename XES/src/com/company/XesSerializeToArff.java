package com.company;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class XesSerializeToArff {

    private AttributeDictionary dict;
    private String fileName;
    private XLog logFile;
    private Path arffPath;
    private long averageTime;
    private ArrayList<String> attributes;

    public XesSerializeToArff(AttributeDictionary attributeDictionary, XLog xlog) {
        dict = attributeDictionary;
        attributes = new ArrayList<>(dict.getAttributeDictionary().keySet());
        logFile = xlog;
        fileName = attributeDictionary.getDbname();
        arffPath = Paths.get(fileName + ".arff");
        averageTime = calculateAverageTime(xlog);
    }

    private Long calculateAverageTime(XLog xlog) {
        long accumulator = 0;
        long temp = 0;
        for (XTrace trace : xlog) {
            long seconds = getTraceDuration(trace);
            accumulator += seconds;
            temp++;

        }
        return accumulator / temp;
    }

    private long getTraceDuration(XTrace trace) {
        int length = trace.size();
        XEvent firstEvent = trace.get(0);
        Date firstEventTS = XTimeExtension.instance().extractTimestamp(firstEvent);

        XEvent lastEvent = trace.get(length - 1);
        Date lastEventTS = XTimeExtension.instance().extractTimestamp(lastEvent);

        //Getting difference by days.
        //FIXME: Make sure that measurement with days is a right approach.
        long days = (lastEventTS.getTime() - firstEventTS.getTime()) / (1000 * 60 * 60 * 24);

        return days;
    }

    /**
     * Our goal is examine slower traces. We classify by "1" traces which is slow (lasts longer then
     * average) We classify by "0" traces which is fast.
     */
    private String classifyTrace(XTrace trace) {
        long traceExecutionTime = getTraceDuration(trace);
        if (traceExecutionTime > averageTime)
            return "1";
        else
            return "0";
    }

    //FIXME: Add binary and frequency constants to use in parameters.
    public void binarySerialize() {
        List<String> arrfFile = createAttributes();
        createData(arrfFile);
        writeToArff(arrfFile);
    }

    private void createData(List<String> arffFile) {
        arffFile.add("@DATA \n");
        for (XTrace trace : logFile) {
            String label = ", " + classifyTrace(trace);
            String instanceString = traceToString(trace);
            String arffInstance = instanceString.substring(1, instanceString.length() - 1);
            arffInstance += label;
            arffFile.add(arffInstance);
        }
    }

    private String traceToString(XTrace trace) {
        Integer length = attributes.size();
        List<Integer> instance = new ArrayList<>(Collections.nCopies(length, 0));
        for (XEvent event : trace) {
            String eventName = XConceptExtension.instance().extractName(event);
            eventName = eventName.replaceAll("[^A-Za-z0-9 ]", "");
            eventName = eventName.replaceAll(" ", "_").toLowerCase();
            Integer index = attributes.indexOf(eventName);
            instance.set(index, 1);
        }
        return instance.toString();
    }

    private List<String> createAttributes() {
        List<String> file = new ArrayList<>();

        String relationName = "@RELATION " + fileName;
        file.add(relationName);
        file.add("\n");

        for (String attribute : dict.getAttributeDictionary().keySet()) {
            String arrfAttr = "@ATTRIBUTE " + attribute + " NUMERIC";
            file.add(arrfAttr);
        }

        // Add the label column which is binary
        // but in ARFF format there is no BINARY type.
        String labelAttribute = "@ATTRIBUTE label NUMERIC";
        file.add(labelAttribute);
        file.add("\n");
        return file;
    }

    private void writeToArff(List<String> file) {
        try {
            Files.write(arffPath, file);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}

