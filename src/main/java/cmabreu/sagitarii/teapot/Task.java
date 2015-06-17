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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Task {
	private List<String> sourceData;
	private List<String> console;
	private String applicationName;
	private TaskStatus status;
	private int exitCode;
	private Activation activation;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	
	public List<String> getSourceData() {
		return sourceData;
	}

	public List<String> getConsole() {
		return console;
	}
	
	public void setSourceData(List<String> sourceData) {
		this.sourceData = sourceData;
	}

	public TaskStatus getTaskStatus() {
		return this.status;
	}
	
	public String getApplicationName() {
		return this.applicationName;
	}
	
	public String getTaskId() {
		return this.activation.getTaskId();
	}	
	
	public Task( Activation activation, String applicationName ) {
		this.applicationName = applicationName;
        this.activation = activation;
        status = TaskStatus.STOPPED;
        this.activation = activation;
        this.console = new ArrayList<String>();
	}

	/**
	 * BLOCKING
	 * Will execute a external program (wrapper)
	 * WIll block until task is finished
	 * 
	 */
	public void run() {
		Process process = null;
        status = TaskStatus.RUNNING;
        try {
        	process = Runtime.getRuntime().exec(applicationName);
        	
        	InputStream in = process.getInputStream(); 
        	BufferedReader br = new BufferedReader( new InputStreamReader(in) );
        	String line = null;
        	while( ( line=br.readLine() )!=null ) {
        		console.add( line );
        		logger.debug( "[" + activation.getActivitySerial() + "] " + activation.getExecutor() + " > " + line );
        	}        	
        	
            process.waitFor();
            exitCode = process.exitValue();
        } catch (IOException e) {
            status = TaskStatus.ERROR;
        } catch (InterruptedException e) {
            status = TaskStatus.ERROR;
            return;
        }
        status = TaskStatus.FINISHED;
    }

	public int getExitCode() {
		return this.exitCode;
	}

	public Activation getActivation() {
		return this.activation;
	}

}