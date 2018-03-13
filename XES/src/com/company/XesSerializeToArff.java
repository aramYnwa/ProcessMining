package com.company;

import org.deckfour.xes.extension.std.XConceptExtension;
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
    private ArrayList<String> attributes;

    public XesSerializeToArff(AttributeDictionary attributeDictionary, XLog xlog) {
        dict = attributeDictionary;
        attributes = new ArrayList<>(dict.getAttributeDictionary().keySet());
        logFile = xlog;
        fileName = attributeDictionary.getDbname();
        arffPath = Paths.get(fileName + ".arff");
    }

    //FIXME: Add binary and frequency constants to use in parameters.
    public void BinarySerialize () {

        List<String> arrfFile = CreateAttributes();
        CreateData(arrfFile);
        WriteToArff(arrfFile);
    }

    private void CreateData(List<String> arffFile) {
        arffFile.add("@DATA \n");
        for (XTrace trace :logFile) {
            String instanceString = TraceToString(trace);
            String arffInstance = instanceString.substring(1, instanceString.length() - 1);
            arffFile.add(arffInstance);
        }
    }

    private String TraceToString(XTrace trace) {
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

    private List<String> CreateAttributes() {
        List<String> file = new ArrayList<>();

        String relationName = "@RELATION " + fileName;
        file.add(relationName);
        file.add("\n");

        for (String attribute : dict.getAttributeDictionary().keySet()) {
            String arrfAttr = "@ATTRIBUTE " + attribute + " NUMERIC";
            file.add(arrfAttr);
        }
        file.add("\n");
        return file;
    }

    private void WriteToArff (List<String> file) {
        try {
            Files.write(arffPath, file);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

