package com.company.SignatureDiscovery.tool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

/*
 * Bruce: 02.06.2014
 * Write dataset for Discriminative Pattern Mining
 */

public class DatasetWriter {
	private CsvReader csvReader;
	private String folder;
	private XLog log;
	Map<String, String> caseActivityMap = new Hashtable<String, String>();
	
	public DatasetWriter(String folder, XLog log) throws ClassNotFoundException, SQLException, IOException {
		
		this.folder = folder;
		this.log = log;
		
        /*
         * Build hash table: key is Case_ID, value is string of activity codes, white space separate
         */
        System.out.println("Build case-trace hash table...");
        
        
        XAttributeMap attributeMap;
        String activityString;
        
        for (XTrace trace : log) {
        	activityString = "";
			for (XEvent event : trace) {
				attributeMap = event.getAttributes();
				if (activityString.equals("")) {
					activityString = attributeMap.get("ActivityCode").toString();
				} else {
					activityString += " " + attributeMap.get("ActivityCode").toString();
				}
			}
			caseActivityMap.put(trace.getAttributes().get("concept:name").toString(), activityString);
		} 		
		
        // Training dataset files
        for (int i=1; i<=5; i++) {
            this.WriteDatasetFile(folder + "\\TRAIN" + i + ".csv", folder + "\\f" + i + "\\train-m.txt", 
            						folder + "\\f" + i + "\\train-c.txt");
        }

        // Test dataset files
        for (int i=1; i<=5; i++) {
        	this.WriteDatasetFile(folder + "\\TEST" + i + ".csv", folder + "\\f" + i + "\\test-m.txt",
        							folder + "\\f" + i + "\\test-c.txt");
        }		
	}
	
    /*
     * tableName: DB Table contains the dataset
     * tableName must have these two fields:
     *      Case_ID: Text
     *      Label: Text
     * 
     * caseTable: DB Table contains all case traces
     * caseTable must have these two fields:
     *      Case_ID: Text
     *      ActivityCode: Number 
     * 
     * 
     * This function creates Training and testing dataset
     * There are two types of training/testing dataset: train-m.txt/train-c.txt and test-m.txt/test-c.txt
     * Difference between x-m.txt versus x-c.txt (x can be train or test)
     * x-m.txt: each line end with -1, file end with -2
     * x-c.txt: each line end with the trace label, no end of file marker
     * x-c.txt.caseid.txt: contains caseid corresponding to each trace (line) in x-m.txt and x-c.txt.
     * x-c.txt.caseid.txt is used for tracing needs.
     * 
     * Example x-m.txt format:
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 56 60 68 70 72 75 78 88 147 147 -1
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 147 -1
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 147 147 56 60 68 70 72 75 78 88 -1
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 147 56 60 68 70 72 75 78 88 147 147 -1
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 56 60 68 70 72 75 78 88 147 147 147 147 -1
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 -1 -2
     * 
     * Example x-c.txt format
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 56 60 68 70 72 75 78 88 147 147 0
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 147 0
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 147 147 56 60 68 70 72 75 78 88 0
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 147 56 60 68 70 72 75 78 88 147 147 1
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 56 60 68 70 72 75 78 88 147 147 147 147 1
     * 9 16 17 19 24 26 28 56 60 68 70 72 75 78 88 1
     * 
     * Example x-c.txt.caseid.txt 
     * 29651940
     * 29655551
     * 29658404
     * 29659147
     * 29659702
    */
    public void WriteDatasetFile (String csvFile, String fileNameM, String fileNameC) 
            throws IOException {


        File aFile;
        
        System.out.println("Building dataset: " + csvFile);

        String newline = System.getProperty("line.separator");

        aFile = new File(fileNameM);
        if (aFile == null) {
          throw new IllegalArgumentException("File should not be null.");
        }
        
        /*
         * Create directory if not exists
         */
        String dirPath = aFile.getParent();
        File dir = new File(dirPath);
        dir.mkdirs();        

        FileWriter writerM = new FileWriter(aFile);

        aFile = new File(fileNameC);

        if (aFile == null) {
          throw new IllegalArgumentException("File should not be null.");
        }

        FileWriter writerC = new FileWriter(aFile); 
        
        String filePath = aFile.getAbsolutePath();
        FileWriter writerCaseID = new FileWriter(new File(filePath  + ".caseid.txt"));


        /*
         * Write to files
         */
        System.out.println("Writing traces to file " + fileNameM + "...");
        
        CsvReader csvReader = new CsvReader(csvFile);
        csvReader.readHeaders();

        StringBuilder oneLineCfile = new StringBuilder();
        StringBuilder oneLineMfile =  new StringBuilder();
        StringBuilder fileCaseIDContent = new StringBuilder();
        
        String caseID, label;
        Boolean hasMore = true;
        
        hasMore = csvReader.readRecord();
        while (hasMore) {
        	caseID = csvReader.get("Case_ID");
        	label = csvReader.get("Label");
        	
            // train-m, test-m file
            oneLineMfile.append(caseActivityMap.get(caseID));
            oneLineMfile.append(" -1"); //wait for next read to append newline or not
            
            // train-c, test-c file
            oneLineCfile.append(caseActivityMap.get(caseID));
            oneLineCfile.append(" " + label);  
            oneLineCfile.append(newline);
            
         // tracking file containing case_IDs
            fileCaseIDContent.append(caseID);
            fileCaseIDContent.append(newline);
            
            /*
             * Read second time 
             * to check end of file
             */
            hasMore = csvReader.readRecord();
            
            if (!hasMore) {
                oneLineMfile.append(" -2");
                oneLineMfile.append(newline);
            } else {
            	oneLineMfile.append(newline); //for end of previous line and start new line
            }
            
            
        }
        
        csvReader.close();
        
        writerM.write(oneLineMfile.toString());
        writerC.write(oneLineCfile.toString());    
        writerCaseID.write(fileCaseIDContent.toString());

        writerM.flush();
        writerM.close();

        writerC.flush();
        writerC.close();
        
        writerCaseID.flush();
        writerCaseID.close();

    }	
}
