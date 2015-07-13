package cmabreu.sagitarii.teapot;

/**
 * Copyright 2015 Carlos Magno Abreu
 * magno.mabreu@gmail.com 
 *
 * Licensed under the Apache  License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required  by  applicable law or agreed to in  writing,  software
 * distributed   under the  License is  distributed  on  an  "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the  specific language  governing  permissions  and
 * limitations under the License.
 * 
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cmabreu.sagitarii.executors.BashExecutor;

public class Task {
	private List<String> sourceData;
	private List<String> console;
	private List<String> execLog;
	private TaskStatus status;
	private int exitCode;
	private Activation activation;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 

	
	private void error( String s ) {
		execLog.add( s );
		logger.error( s );
	}
	
	private void debug( String s ) {
		execLog.add( s );
		logger.debug( s );
	}
	
	public Activation getActivation() {
		return activation;
	}
	
	public List<String> getSourceData() {
		return sourceData;
	}

	public List<String> getConsole() {
		return console;
	}
	
	public List<String> getExecLog() {
		return execLog;
	}

	public void setSourceData(List<String> sourceData) {
		this.sourceData = sourceData;
	}

	public TaskStatus getTaskStatus() {
		return this.status;
	}

	public String getApplicationName() {
		return activation.getCommand();
	}

	public String getTaskId() {
		return this.activation.getTaskId();
	}	

	public Task( Activation activation, List<String> execLog ) {
		this.activation = activation;
		status = TaskStatus.STOPPED;
		this.activation = activation;
		this.console = new ArrayList<String>();
		this.execLog = execLog;
	}
	
	/**
	 * BLOCKING
	 * Will execute a external program (wrapper)
	 * WIll block until task is finished
	 * 
	 */
	public void run( Configurator conf ) {
		Process process = null;
		status = TaskStatus.RUNNING;
		try {

			if ( activation.getExecutorType().equals("BASH") ) {
				debug("running Bash Script " + activation.getCommand() );
				BashExecutor ex = new BashExecutor();
				exitCode = ex.execute( activation.getCommand(), activation.getNamespace() );
				console = ex.getConsole();
			} else {
				debug("running external wrapper " + activation.getCommand() );
				process = Runtime.getRuntime().exec( activation.getCommand() );
				InputStream in = process.getInputStream(); 
				BufferedReader br = new BufferedReader( new InputStreamReader(in) );
				String line = null;

				InputStream es = process.getErrorStream();
				BufferedReader errorReader = new BufferedReader(  new InputStreamReader(es) );
				while ( (line = errorReader.readLine() ) != null) {
					console.add( line );
					logger.error( line );
				}	
				errorReader.close();
				
				while( ( line=br.readLine() )!=null ) {
					console.add( line );
					logger.debug( "[" + activation.getActivitySerial() + "] " + activation.getExecutor() + " > " + line );
				}  
				br.close();
				
				exitCode = process.waitFor();
			}     
		} catch ( Exception ex ){
			error( ex.getMessage() );
			for ( StackTraceElement ste : ex.getStackTrace() ) {
				error( ste.toString() );
			}
		}
		status = TaskStatus.FINISHED;
		debug("external wrapper finished.");
	}

	public int getExitCode() {
		return this.exitCode;
	}


}