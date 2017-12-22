package com.tibco.ps.kpimetrics;

/**
 * (c) 2017 TIBCO Software Inc. All rights reserved.
 * 
 * Except as specified below, this software is licensed pursuant to the Eclipse Public License v. 1.0.
 * The details can be found in the file LICENSE.
 * 
 * The following proprietary files are included as a convenience, and may not be used except pursuant
 * to valid license to Composite Information Server or TIBCO(R) Data Virtualization Server:
 * csadmin-XXXX.jar, csarchive-XXXX.jar, csbase-XXXX.jar, csclient-XXXX.jar, cscommon-XXXX.jar,
 * csext-XXXX.jar, csjdbc-XXXX.jar, csserverutil-XXXX.jar, csserver-XXXX.jar, cswebapi-XXXX.jar,
 * and customproc-XXXX.jar (where -XXXX is an optional version number).  Any included third party files
 * are licensed under the terms contained in their own accompanying LICENSE files, generally named .LICENSE.txt.
 * 
 * This software is licensed AS-IS. Support for this software is not covered by standard maintenance agreements with TIBCO.
 * If you would like to obtain assistance with this software, such assistance may be obtained through a separate paid consulting
 * agreement with TIBCO.
 * 
 */

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

import com.compositesw.extension.CustomProcedure;
import com.compositesw.extension.CustomProcedureException;
import com.compositesw.extension.ExecutionEnvironment;
import com.compositesw.extension.ParameterInfo;

public class CpuAndMemCheckerCjp implements CustomProcedure
{
    /**
     * 
     */
	private static ExecutionEnvironment qenv;  //V3
	
	private String debug;
	private String cpuScriptName;
	private String memScriptName;
	
    /**
     * 
     */
    public void initialize(ExecutionEnvironment qenv)
    {
    	this.qenv = qenv;
    }
    
    /**
     * 
     */
	public ParameterInfo[] getParameterInfo()
    {
    	ParameterInfo[] paramsArray = 
		        new ParameterInfo[]
		        {
			    	new ParameterInfo("debug", Types.VARCHAR, DIRECTION_IN),
    			    new ParameterInfo("cpuScriptNameOrCommand", Types.VARCHAR, DIRECTION_IN),
    			    new ParameterInfo("memScriptNameOrCommand", Types.VARCHAR, DIRECTION_IN),

    			    new ParameterInfo("cpuUsedPercent", Types.VARCHAR, DIRECTION_OUT),
    			    new ParameterInfo("memoryUsedMb",   Types.VARCHAR, DIRECTION_OUT),
    			    new ParameterInfo("memoryAvailMb",   Types.VARCHAR, DIRECTION_OUT),
		        };
    	return paramsArray;
    }
	
	/**
	 * Only processes input parameters at this point.
	 */
    public void invoke(Object inputValues[])
            throws CustomProcedureException, SQLException
    {    	
    	// Get the input params:
    	this.debug = (String)(inputValues[0]);
    	if (this.debug == null)
    		this.debug = "N";
       	this.cpuScriptName = (String)(inputValues[1]);
    	this.memScriptName = (String)(inputValues[2]);

    	log(this.debug, LOG_INFO, "LOG_INFO: invoke() populated the following input params:");
    	log(this.debug, LOG_INFO, "LOG_INFO: debug = " + this.debug);
    	log(this.debug, LOG_INFO, "LOG_INFO: cpuScriptName = " + this.cpuScriptName);
    	log(this.debug, LOG_INFO, "LOG_INFO: memScriptName = " + this.memScriptName);
    	log(this.debug, LOG_DEBUG, "LOG_DEBUG: invoke() populated the following input params:");
    	log(this.debug, LOG_DEBUG, "LOG_DEBUG: cpuScriptName = " + this.cpuScriptName);
    	log(this.debug, LOG_DEBUG, "LOG_DEBUG: memScriptName = " + this.memScriptName);
   }
    
    /**
     *  Mandatory method. No logic here for now.
     */
    public int getNumAffectedRows()
    {
   		log(this.debug, LOG_INFO, "getNumAffectedRows() called");
        return 0;
    }
    
    /**
     * All logic is currently here.
     * 
     */
    public Object[] getOutputValues() throws CustomProcedureException
    {
   		log(this.debug, LOG_INFO, "getOutputValues() is called");
    	int osName = MachineCpuAndMemChecker.getOSname();
    	Float cpuUtilizValue = null;
    	Float memoryUsed = null;
    	Float memoryAvailable = null;
    	ArrayList<Float> memoryUtilizValues = null;
		
    	try
    	{
    		if (osName == MachineCpuAndMemChecker.OS_IS_LINUX)
    		{
   	    		log(this.debug, LOG_INFO, "OS is Linux");    		
    	
   	    		cpuUtilizValue = MachineCpuAndMemChecker.getCurrentCPUUsagePercentLinux(this.debug, this.cpuScriptName);
   	    		log(this.debug, LOG_INFO, "cpuUtilizValue="+cpuUtilizValue); 
    			if (cpuUtilizValue == null) {
    				throw new CustomProcedureException("The cpu utilization script returned null.  script command="+this.cpuScriptName);
    			}
    			
    			memoryUtilizValues = MachineCpuAndMemChecker.getCurrentFreeMemMbLinux(this.debug, this.memScriptName);
    			
    			if (memoryUtilizValues != null && memoryUtilizValues.size() == 2) {
    				memoryUsed = memoryUtilizValues.get(0);
    				memoryAvailable = memoryUtilizValues.get(1);
       	    		log(this.debug, LOG_INFO, "memoryUsed="+memoryUsed); 
       	    		log(this.debug, LOG_INFO, "memoryAvailable="+memoryAvailable); 
    			}
    			else {
       	    		log(this.debug, LOG_INFO, "Throwing an exception:  The memory utilization script did not return the proper number of array values.  script command="+this.memScriptName); 
        	    	throw new CustomProcedureException("The memory utilization script did not return the proper number of array values.  script command="+this.memScriptName);
    			}
    		}
		
	    	else if(osName == MachineCpuAndMemChecker.OS_IS_WINDOWS)
	    	{
   	    		log(this.debug, LOG_INFO, "OS is Windows");  
	    		 
	    		cpuUtilizValue = MachineCpuAndMemChecker.getCurrentCPUUsagePercentWindows(this.debug, this.cpuScriptName);
   	    		log(this.debug, LOG_INFO, "cpuUtilizValue="+cpuUtilizValue);  
    			if (cpuUtilizValue == null) {
    				throw new CustomProcedureException("The cpu utilization script returned null.  script command="+this.cpuScriptName);
    			}
	    		
	   			memoryUtilizValues = MachineCpuAndMemChecker.getCurrentFreeMemMbWindows(this.debug, this.memScriptName);

    			if (memoryUtilizValues != null && memoryUtilizValues.size() == 2) {
    				memoryUsed = memoryUtilizValues.get(0);
    				memoryAvailable = memoryUtilizValues.get(1);
       	    		log(this.debug, LOG_INFO, "memoryUsed="+memoryUsed); 
       	    		log(this.debug, LOG_INFO, "memoryAvailable="+memoryAvailable); 
    			}
    			else {
       	    		log(this.debug, LOG_INFO, "Throwing an exception:  The memory utilization script did not return the proper number of array values.  script command="+this.memScriptName); 
    				throw new CustomProcedureException("The memory utilization script did not return the proper number of array values.  script command="+this.memScriptName);
    			}
	    	}
    	
	    	else
	    	{
	    		String errorMsg = "OS is " + osName + ". It is currently not supported.";
   	    		log(this.debug, LOG_ERROR, errorMsg);
	    		throw new CustomProcedureException(errorMsg);
	    	}
    	}
  
  		catch (IOException e)
		{
			throw new CustomProcedureException(
					"IOException is caught in getOutputValues(). " +
					"Check file paths. Detailed exception msg is: " +
					e.getMessage());
		}
		catch (InterruptedException e)
		{
			throw new CustomProcedureException(
					"InterruptedException is caught in getOutputValues(). " +
					"Check the scope and lifetime of the child process that is created by Process.exec() " +
					"Detailed exception msg is: " +
					e.getMessage());
		}
			
    	Object[] returnObj = new Object[] 
    						{ 
    							cpuUtilizValue.toString(), 	// CPU percent in use
    							memoryUsed.toString(), 		// Used memory
    							memoryAvailable.toString()  // Available memory
    						}; 
    	return returnObj;
    }
	  
    /**
     *  Clean-up
     */
    public void close()
        throws SQLException
    {
   		log(this.debug, LOG_INFO, "CpuAndMemCheckerCjp.close() is called.");
    }
    
    /**
     * 
     */
    public String getName()
    {
   		log(this.debug, LOG_INFO, "CpuAndMemCheckerCjp.getName() called");
        return "CpuAndMemCheckerCjp";
    }

    public String getDescription()
    {
   		log(this.debug, LOG_INFO, "CpuAndMemCheckerCjp.getDescription() is called");
        return "CJP to get current machine CPU and Memory utilization.";
    }

    public boolean canCommit()
    {
   		log(this.debug, LOG_INFO, "CpuAndMemCheckerCjp.canCommit() is called");
        return true; // false;
    }

    public void commit()
        throws CustomProcedureException
    {
   		log(this.debug, LOG_INFO, "CpuAndMemCheckerCjp.commit() is called");
    }

    public void rollback()
        throws CustomProcedureException
    {
   		log(this.debug, LOG_INFO, "CpuAndMemCheckerCjp.rollback() is called");
    }

    public boolean canCompensate()
    {
   		log(this.debug, LOG_INFO, "CpuAndMemCheckerCjp.canCompensate() is called");
   		return false;
    }

    public void compensate(ExecutionEnvironment env)   throws CustomProcedureException
    {
   		log(this.debug, LOG_INFO, "CpuAndMemCheckerCjp.compensate() is called");
    }
    
	/**
	 *   
	 * @param level
	 * @param msg
	 */
    public static void log(String debug, int level, String msg)  //V3
    {
        if(qenv == null)
            System.out.println(msg);
        else
        	if (level == LOG_ERROR || level == LOG_DEBUG || (debug != null && debug.equalsIgnoreCase("Y"))) {
        		qenv.log(level, msg);
        	}
    }	

}
