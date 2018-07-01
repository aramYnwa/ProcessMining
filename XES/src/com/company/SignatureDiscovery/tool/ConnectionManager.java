package com.company.SignatureDiscovery.tool;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Map;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Administrator
 */
public class ConnectionManager {
    private static String driverName = "";   
    private static String conURL = "";   
    private static String username = "";   
    private static String password = "";
    private static Connection con;
    private static ConnectionManager me = null;
    
    public static void main(String[] args) {
        String str="";
        try {
//            getConnection();
//            str = ConnectionManager.executeSQL("select * from Employees");
        	ConnectionManager.initParametersFromFile();
            
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
        System.out.println(str);
        
    }
    
    public static ConnectionManager getInstance() {
        if (me == null) {
            me = new ConnectionManager();
        }
        return me; 
    }    
    
    public static void close() {
    	try {
    		if (con != null) {
    			con.close();
    		}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static void initConnectionProperties(Map<String, String> conParams) {
        driverName = conParams.get("DBDriverName");
        conURL = conParams.get("DBConnectionURL");
        username = conParams.get("DBUsername");
        password = conParams.get("DBPassword");
        
        if (conParams.get("DBDriverName") != null && 
                conParams.get("DBConnectionURL") != null &&
                conParams.get("DBUsername") != null &&
                conParams.get("DBPassword") != null) {
            driverName = conParams.get("DBDriverName");
            conURL = conParams.get("DBConnectionURL");
            username = conParams.get("DBUsername");
            password = conParams.get("DBPassword");
        }
        else {
            System.out.println("Initialisation failed for DB Connection Parameters");
        }        
    }     
      
    public static Connection getConnection() throws ClassNotFoundException, SQLException {         
        try {
            Class.forName(driverName);
            try {
                System.out.println("jdbc.driver: " + driverName);
                System.out.println("jdbc.url: " + conURL);
                System.out.println("jdbc.username: " + username);
                System.out.println("jdbc.password: " + password);  
                
                con = DriverManager.getConnection(conURL, username, password);              
            } catch (SQLException ex) {
                // log an exception. fro example:
//                System.out.println("Failed to create the database connection.");
                throw ex;
            }
        } catch (ClassNotFoundException ex) {
            // log an exception. for example:
//            System.out.println("Driver not found."); 
            throw ex;
        }
        return con;
    }
    
    public static String getInstanceLabel(String instanceID, String tableName) throws SQLException, ClassNotFoundException {
        String queryResult = null; 
        String strQuery;
        ResultSet rs;
        
        
        strQuery = "select Label from " + tableName + " where Case_ID = '" + instanceID + "'";
        rs = ConnectionManager.executeSQL(strQuery);
        
        // Get the value from the first result
        while (rs.next()) {
            queryResult = rs.getString("Label");
            return queryResult;
        }
        return queryResult;
    }
    
    public static ResultSet executeSQL(String sql) throws ClassNotFoundException, SQLException {
        ResultSet rs;
        Statement stmt;
        
        if (con == null) {
            getConnection();
        }
        else if (con.isClosed()) {
            getConnection(); 
        }

        stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        rs = stmt.executeQuery(sql); 
 
        return rs;
    }
    
    public static void executeStatement(String sql) throws ClassNotFoundException, SQLException {
        Statement stmt;
        
        if (con == null) {
            getConnection();
        }
        else if (con.isClosed()) {
            getConnection(); 
        }

        stmt = con.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();         
 
    }    
    
    /*
     * Initialize parameters from an XML file
     * This file is located in the same folder as the root folder of this application
     */
    public static void initParametersFromFile() {
        // Read database connection properties
        DBConnectionParamReader paramReader;
        try {
            paramReader = new DBConnectionParamReader(System.getProperty("user.dir") + "\\properties.xml");
        }
        catch (FileNotFoundException ex){
            System.out.println(ex.getMessage());
            return;
        }
        catch (IOException ex) {
            System.out.println(ex.getMessage());
            return;
        }

        Map<String, String> mapDBParams = new Hashtable<String, String>();
        mapDBParams.put("DBDriverName", paramReader.getDriverName());
        mapDBParams.put("DBConnectionURL", paramReader.getUrl());
        mapDBParams.put("DBUsername", paramReader.getUsername());
        mapDBParams.put("DBPassword", paramReader.getPassword());
        ConnectionManager.initConnectionProperties(mapDBParams);        
    }
    
    public static Double getMinSup() throws FileNotFoundException, IOException, NumberFormatException {
        // Read database connection properties
        DBConnectionParamReader paramReader;
        paramReader = new DBConnectionParamReader(".\\properties.xml");
        return paramReader.getMinSup();  	
    }
    
    public static Double getCoverageThreshold() throws FileNotFoundException, IOException, NumberFormatException {
        // Read database connection properties
        DBConnectionParamReader paramReader;
        paramReader = new DBConnectionParamReader(".\\properties.xml");
        return paramReader.getCoverageThreshold();
    }    

}

