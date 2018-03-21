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
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MachineCpuAndMemChecker
{
	/**
	 * Set the value for log4j LOG_INFO
	 */
	public static int LOG_INFO = CpuAndMemCheckerCjp.LOG_INFO;
	
	/**
	 * Command to run on Windows Powershell command to get CPU utilization or Processor Time
	 * 
	 * Should return 1 line that look like this:
	 * 
	 * Processor_Time=31.5902376654125
	 * 
	 */
	public static final String CPU_UTILIZATION_COMMAND_WINDOWS = "powershell.exe -file .\\KPImetricsCpuUtilization.ps1";

	/**
	 * Command to run on Windows Powershell command to get Memory utilization
	 * 
	 * Should return 1 line that look like this:
	 * 
	 * Memory_Total=5999.48828125
	 * Memory_In_Use=4190.64453125
	 * Memory_Available=1808.84375
	 * 
	 */
	public static final String MEM_UTILIZATION_COMMAND_WINDOWS = "powershell.exe -file .\\KPImetricsMemUtilization.ps1";

	/**
	 * Pattern to look for in the OS name to be returned by the Windows OS.
	 * Pattern is all lower case since we'll convert the OS name to lower case before the search.  
	 */
	public static final String OS_WINDOWS_LOOKUP_STR = "windows";
	
	/**
	 * Script containing command to run on Linux to get CPU utilization.
	 * This command is wrapped in a shell script to simplify passing the results into java, since
	 * java's Process.exec() is not a shell interpreter and can handle only one command (with arguments),
	 * not output stream redirection or pipe from that command.  
	 * 
	 * The Linux 6 script [KPImetricsTopCommandGrepCpu_linux6.sh] is:
	 * 		#!/bin/sh
	 *		# Linux 6 command and format
	 *		top -b -n 4 | grep Cpu
	 *		exit 0
	 * 
	 * top -b -n 4 | grep Cpu    - command for Linux; 4 measurements (samples). We'll ignore the 1st of them,
	 * since its average from the machine start; all remaining measurements are for current
	 * CPU utilization, so we'll average those ourselves. 
	 * 
	 * top -n 0 -l 1 | grep idle - command for Mac OS X, 1 measurement
	 * 
	 * iostat                    - alternative to top command, but may need to be installed separately, so we are not using it.
	 * 
	 * Here is an example of output of the above command for one measurement (or one sample in the terms of top command, it returns one line):
	 *  
	 * Mac OS X:   CPU usage: 4.54% user, 15.90% sys, 79.54% idle 
	 * Linux:      Cpu(s):  3.7%us,  0.4%sy,  0.0%ni, 95.6%id,  0.1%wa,  0.0%hi,  0.1%si,  0.0%st
	 *
	 * The Linux 7 script [KPImetricsTopCommandGrepCpu_linux7.sh] is:
	 *		#!/bin/sh
	 *		# Linux 7 command and format
	 *		#    Instructions: Modify CIS_HOME variable
	 *		if [ "$CIS_HOME" == "" ];
	 *		then
	 *		   CIS_HOME=/comp/composite/CIS_7.0
	 *		   export CIS_HOME
	 *		fi
	 *		top -b -n 4 | grep Cpu | sed -f $CIS_HOME/bin/KPImetricsCpuFormat_linux7
	 *		exit 0 
	 *
	 * The Linux 7 sed script [KPImetricsCpuFormat_linux7] is:
	 * 		1,$s/ us/\%us/
	 * 		1,$s/ sy/\%sy/
	 * 		1,$s/ ni/\%ni/
	 * 		1,$s/ id/\%id/
	 * 		1,$s/ wa/\%wa/
	 * 		1,$s/ hi/\%hi/
	 * 		1,$s/ si/\%si/
	 * 		1,$s/ st/\%st/
	 * 		1,$s/^\%//
	 *
	 */
	public static final String CPU_UTILIZATION_SCRIPT_LINUX =  "./KPImetricsTopCommandGrepCpu_linux7.sh"; // Linux 7 shell script
	// 														   "./KPImetricsTopCommandGrepCpu_linux6.sh"; // Linux 6 shell script
	
	/**
	 * Script containing command to run on Linux to get physical Memory utilization.
	 * 
	 * The Linux 6 script [KPImetricsFreeMemCommand_linux6.sh] is:
	 *		#!/bin/sh
	 *		# Linux 6 command and format
	 *		free -m | grep '/cache'  
	 *		exit 0
	 * 
	 * The Linux 7 script [KPImetricsFreeMemCommand_linux7.sh] is:
	 *		#!/bin/sh 
	 *		# Linux 7 command and format
	 *		export CACHE_INFO1=`free -m | grep Mem | awk '{print   $6}'`
	 *		export CACHE_INFO2=`free -m | grep Mem | awk '{print   $7}'`
	 *		echo "-/+ buffers/cache:     $CACHE_INFO1    $CACHE_INFO2"
	 *		exit 0
	 *
	 * With that command we get one line that shows OS cache and buffers as free memory. 
	 * 
	 */
	public static final String MEM_UTILIZATION_SCRIPT_LINUX = "./KPImetricsFreeMemCommand_linux7.sh"; // Linux 7 shell script
	// 														  "./KPImetricsFreeMemCommand_linux6.sh"; // Linux 6 shell script
	
	/**
	 * Pattern to look for in the OS name to be returned by the Linux OS.
	 * Pattern is all lower case since we'll convert the OS name to lower case before the search.  
	 */
	public static final String OS_LINUX_LOOKUP_STR = "linux";	
	
	public static final int OS_IS_UNDEFINED = 0;
	public static final int OS_IS_WINDOWS = 1;
	public static final int OS_IS_LINUX = 2;
	
	/**
	 * Obtains the OS name and parses it to understand the OS type: Windows or Linux.
	 * At this point we don't take into account which version of Windows or Linux we have.
	 * 
	 * @return	OS type: Windows or Linux.
	 */
	static int getOSname()
	{
		String osNameProperty = System.getProperty("os.name").toLowerCase();
		
		//System.out.println("osNameProperty: " + osNameProperty);
		int osName = OS_IS_UNDEFINED;
		
		if	(osNameProperty.contains(OS_WINDOWS_LOOKUP_STR) )
		{	
			osName = OS_IS_WINDOWS;
			//System.out.println("OS is WINDOWS");
		}
		
		else if (osNameProperty.contains(OS_LINUX_LOOKUP_STR) )
		{
			osName = OS_IS_LINUX;
			//System.out.println("OS is LINUX");
		}
		
		return osName;
	}
	
	/**
	 * OS is defined as Windows when this method is called
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	static Float getCurrentCPUUsagePercentWindows(String debug, String commandOrScript) throws InterruptedException, IOException
	{
		String moduleName = "getCurrentCPUUsagePercentWindows";
		String name = null;
		float output = 0F;
		Runtime runtimeEnv = Runtime.getRuntime(); 	
		Process osProcess = null;
		String currentLine = null;
		String[] currentLineTokens = null;
        DecimalFormat df = new DecimalFormat("#.00");

		if(commandOrScript == null) {
			commandOrScript = CPU_UTILIZATION_COMMAND_WINDOWS;
		}

		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : Windows command or script is: " + commandOrScript);

		osProcess = runtimeEnv.exec(commandOrScript);	

// Waiting for the process to finish.
//			p.waitFor();
//	        System.out.println("Process exited with code = " + rt.exitValue());

// Get process' output: its InputStream
	    java.io.InputStream is = osProcess.getInputStream();
	    java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
	        // And print each line
	    /*
	     * powershell -file .\KPImetricsCpuUtilization.ps1
		 * Processor_Time=36.1352003502669
	     */
        while ( (currentLine = reader.readLine() ) != null)
        {
    		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : line="+currentLine);

    		// Split on "=" where line=Processor_Time=36.13
        	currentLineTokens = currentLine.split("=");

        	for (int i = 0; i < currentLineTokens.length; i++)
        	{
        		if (i == 0)
        			name = currentLineTokens[i].trim();

        		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : <" + currentLineTokens[i] + ">");
        		
        		if (i > 0 && name.toLowerCase().contentEquals("processor_time")) {
        			output = Float.parseFloat(currentLineTokens[i].trim());
        		}
        	}
        }
        is.close();		
        int exitVal = osProcess.waitFor();
        
        String outputStr = df.format(output);
        if (outputStr.isEmpty())
        	outputStr = "-1.0";

        //System.out.println("Exit value for the OS process: " + exitVal);
		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : output=" + outputStr);
        
        return new Float(outputStr);
	}

	
	/**
	 * OS is defined as Windows when this method is called
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	static ArrayList<Float> getCurrentFreeMemMbWindows(String debug, String commandOrScript) throws InterruptedException, IOException
	{
		String moduleName = "getCurrentFreeMemMbWindows";
		String name = null;
		float output = 0F;
		Runtime runtimeEnv = Runtime.getRuntime(); 	
		Process osProcess = null;
		String currentLine = null;
		String[] currentLineTokens = null;
		ArrayList<Float> result = new ArrayList<Float>();
		
		if(commandOrScript == null) {
			commandOrScript = MEM_UTILIZATION_COMMAND_WINDOWS;
		}

		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : Windows command or script is: " + commandOrScript);
		
		osProcess = runtimeEnv.exec(commandOrScript);	

// Waiting for the process to finish.
//			p.waitFor();
//	        System.out.println("Process exited with code = " + rt.exitValue());

// Get process' output: its InputStream
	    java.io.InputStream is = osProcess.getInputStream();
	    java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
	        // And print each line
	    /*  
	     * powershell -file .\KPImetricsMemUtilization.ps1
	     * Memory_Total=5999.48828125
		 * Memory_In_Use=4622.953125		<-- return the used memory
		 * Memory_Available=1376.53515625	<-- return the available memory
	     */
        while ( (currentLine = reader.readLine() ) != null)
        {
        	// Split on "=" where line=Process_Time=36.13
        	currentLineTokens = currentLine.split("=");
    		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : line="+currentLine);

    		for (int i = 0; i < currentLineTokens.length; i++)
        	{
        		if (i == 0)
        			name = currentLineTokens[i].trim();

        		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : <" + currentLineTokens[i] + ">");

        		// Only add the value and not the name to the array list
        		if (i > 0 && name.toLowerCase().contentEquals("memory_in_use")) {
        			output = Float.parseFloat(currentLineTokens[i].trim());
        			result.add(output);
            		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : memory_in_use output=" + output);
        		}
        		if (i > 0 && name.toLowerCase().contentEquals("memory_available")) {
        			output = Float.parseFloat(currentLineTokens[i].trim());
        			result.add(output);
            		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : memory_available output=" + output);
        		}
        	}
        }
        is.close();		
        int exitVal = osProcess.waitFor();
        //System.out.println("Exit value for the OS process: " + exitVal);
        return result;
	}

	
	/**
	 * OS is defined as Linux when this method is called
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	static Float getCurrentCPUUsagePercentLinux(String debug, String commandOrScript) throws InterruptedException, IOException
	{
		String moduleName = "getCurrentCPUUsagePercentLinux";
		Runtime runtimeEnv = Runtime.getRuntime(); 	
		Process osProcess = null;
		String currentLine = null;
		String[] currentLineTokens = null;
		String idleCpuPercentStr = null;     						   // idle CPU as String to be used in a loop. 
		ArrayList<Float> idleCpuPercentage = new ArrayList<Float>();   // array of idle CPU measurements.
		float averageCPU_Utiliz = 0F;
        DecimalFormat df = new DecimalFormat("#.00");
		
		if(commandOrScript == null) {
			commandOrScript = CPU_UTILIZATION_SCRIPT_LINUX;
		}

		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : Linux command or script is: " + commandOrScript);
		
		osProcess = runtimeEnv.exec(commandOrScript);	
		
	    java.io.InputStream is = osProcess.getInputStream();
	    java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
 
	    if ( (currentLine = reader.readLine() ) !=  null)   // read just one line in this case.
        {
	    	//System.out.println("Ignoring the first line since it gives the average CPU idle percentage from the machine start...");
	    	//System.out.println("First line was: " + currentLine);
        }
	    else 
	    {
	    	//System.out.println("No output line from Linux top command is found to read.");
	    	System.exit(1);
	    }

		int startPos, endPos = 0;
	    
	    while ( (currentLine = reader.readLine() ) !=  null) // starting from the second line.
	    {
 			CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : currentLine=" + currentLine);

			startPos = currentLine.indexOf("ni,") + 3;
			endPos = currentLine.indexOf("id,");
	
			CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : startPos:" + startPos + ";endPos:" + endPos + ";");
	
			idleCpuPercentStr = currentLine.substring(startPos, endPos).trim();		  
 			CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : idleCpuPercentStr:" + idleCpuPercentStr);
	
 			idleCpuPercentStr = idleCpuPercentStr.replaceAll("%", "");
 			CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : idleCpuPercentStr:" + idleCpuPercentStr);

        	idleCpuPercentage.add(Float.parseFloat(idleCpuPercentStr)) ;	
        }
	    if (idleCpuPercentage.size() == 0)
	    {
 			CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : No output is found for CPU utilization, exiting");
	    	System.exit(1);
	    }
	    
        is.close();
        int exitVal = osProcess.waitFor();
        //System.out.println("Exit value for the OS process: " + exitVal);
        
        // Get the average value of the used CPU by subtracting the idle values from 100% and taking average:
        for (int i = 0; i < idleCpuPercentage.size(); i++)
        {
        	 averageCPU_Utiliz += (100.0 - idleCpuPercentage.get(i) );
        }

        averageCPU_Utiliz = averageCPU_Utiliz / idleCpuPercentage.size() ;
        String averageCPU_UtilizStr = df.format(averageCPU_Utiliz);
        if (averageCPU_UtilizStr.isEmpty())
        	averageCPU_UtilizStr = "-1.0";

		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : averageCPU_UtilizStr=" + averageCPU_UtilizStr);
        
        return new Float(averageCPU_UtilizStr); 
     }

	
	/**
	 * OS is defined as Linux when this method is called
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 * 
	 * @return an array of two elements: used memory and available memory.
	 * 			Linux cache and buffers are considered part of available, not used memory.
	 */
	static ArrayList<Float> getCurrentFreeMemMbLinux(String debug, String commandOrScript) throws InterruptedException, IOException
	{
		String moduleName = "getCurrentFreeMemMbLinux";
		Runtime runtimeEnv = Runtime.getRuntime(); 	
		Process osProcess = null;
		String currentLine = null;
		String[] currentLineTokens = null;
		float freeMemMb = 0F;
		float usedMemMb = 0F;
		ArrayList<String> nonEmptyTokens = new ArrayList<String>();
		ArrayList<Float> result = new ArrayList<Float>();
		

		if(commandOrScript == null) {
			commandOrScript = MEM_UTILIZATION_SCRIPT_LINUX;
		}

		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : Linux command or script is: " + commandOrScript);
		
		osProcess = runtimeEnv.exec(commandOrScript);	

	    java.io.InputStream is = osProcess.getInputStream();
	    java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
	 
	    if ( (currentLine = reader.readLine() ) !=  null)   // read just one line in this case.
        {
			CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : currentLine=" + currentLine);

			currentLineTokens = currentLine.split("\\s");
	    	
	    	for (int i = 0; i < currentLineTokens.length; i++)
        	{
        		if(!currentLineTokens[i].isEmpty() )
        		{
    				CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : <" + currentLineTokens[i] + ">");
        			nonEmptyTokens.add(currentLineTokens[i]);
        		}
        	}
	    	
	    	usedMemMb = Float.parseFloat(nonEmptyTokens.get(nonEmptyTokens.size() - 2) );   // we need 2d token from the end.
	    	freeMemMb = Float.parseFloat(nonEmptyTokens.get(nonEmptyTokens.size() - 1) );   // we need the last token.
        }
	    else 
	    {
			CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : No output line from Linux top command is found to read.");
	    	System.exit(1);
	    }
	    
        is.close();
        int exitVal = osProcess.waitFor();
        //System.out.println("Exit value for the OS process: " + exitVal);
        
 		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : usedMemMb=" + usedMemMb);
		CpuAndMemCheckerCjp.log(debug, LOG_INFO, moduleName+" : freeMemMb=" + freeMemMb);

        result.add(new Float(usedMemMb));
        result.add(new Float(freeMemMb));
        
        return result;
     }		
	
	
	/**
	 * For local testing
	 * 
	 * @param args
	 */
	public static void main(String args[])
	{
		int osName = getOSname();
		String debug = "Y";
		String cpuScriptName = null;
		String memScriptName = null;
		
		if (osName == OS_IS_WINDOWS)
		{
			if(args.length == 2 )
			{
				cpuScriptName = new String (args[0]);
				memScriptName = new String (args[1]);
			}
			else
			{
				cpuScriptName = CPU_UTILIZATION_COMMAND_WINDOWS;
				memScriptName = MEM_UTILIZATION_COMMAND_WINDOWS;
			}

			try
			{
				getCurrentCPUUsagePercentWindows(debug, cpuScriptName);
				getCurrentFreeMemMbWindows(debug, memScriptName);
			}
			catch (IOException e)
			{
				//System.out.println(e.getMessage());
			}
			catch (InterruptedException e)
			{
				//System.out.println(e.getMessage());
			}
		}
		else if (osName == OS_IS_LINUX)
		{
			if(args.length == 2 )
			{
				cpuScriptName = new String (args[0]);
				memScriptName = new String (args[1]);
			}
			else
			{
				cpuScriptName = CPU_UTILIZATION_SCRIPT_LINUX;
				memScriptName = MEM_UTILIZATION_SCRIPT_LINUX;
			}
			
			try
			{
				getCurrentCPUUsagePercentLinux(debug, cpuScriptName);
				getCurrentFreeMemMbLinux(debug, memScriptName);
			}
			catch (IOException e)
			{
				//System.out.println(e.getMessage());
			}
			catch (InterruptedException e)
			{
				//System.out.println(e.getMessage());
			}
		}
		

	}
}
