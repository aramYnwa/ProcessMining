package com.company.SignatureDiscovery.tool;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * XML File Example:
   <?xml version="1.0" encoding="UTF-8" standalone="no"?>
   <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
   <properties> 
        <entry key="jdbc.driver">sun.jdbc.odbc.JdbcOdbcDriver</entry>
        <entry key="jdbc.url">jdbc:odbc:SAW</entry>
        <entry key="jdbc.username">root</entry>
        <entry key="jdbc.password">mysql</entry>
        
   </properties>
 
 */
public class DBConnectionParamReader {
    private String driverName;
    private String url;
    private String username;
    private String password;
    private Double coverageThreshold;
    private Double minSup;
    
    public DBConnectionParamReader(String filename) throws FileNotFoundException, IOException, NumberFormatException {

            //Reading properties file in Java example
            Properties props = new Properties();
            FileInputStream fis = new FileInputStream(filename);

            //loading properties from properties file
            props.loadFromXML(fis);

            //reading property
            driverName = props.getProperty("jdbc.driver");
            url = props.getProperty("jdbc.url");
            username = props.getProperty("jdbc.username");
            password = props.getProperty("jdbc.password");
            minSup = Double.valueOf(props.getProperty("min.support"));
            coverageThreshold = Double.valueOf(props.getProperty("coverage.threshold"));
    }
    
    public String getDriverName() {
        return driverName;
    }

    public String getUrl () {
        return url;
    }
    
    public String getUsername () {
        return username;
    }
    
    public String getPassword () {
        return password;
    }
    
    public Double getMinSup() {
    	return minSup;
    }
    
    public Double getCoverageThreshold() {
    	return coverageThreshold;
    }
   
}
