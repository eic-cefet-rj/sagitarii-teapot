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

import java.util.List;
import java.util.UUID;

import cmabreu.sagitarii.teapot.comm.Communicator;

public class TaskRunner extends Thread {
	private Teapot teapot;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private String serial;
	private String response;
	private boolean active = true;
	private String startTime;  
	private long startTimeNano;    

	public String getStartTime() {
		return startTime;
	}
	
	public boolean isActive() {
		return active;
	}

	public String getSerial() {
		return serial;
	}
	
	public List<Activation> getJobPool() {
		return teapot.getJobPool();
	}
	
	public Task getCurrentTask() {
		return teapot.getCurrentTask();
	}
	
	public TaskRunner( String response, Communicator communicator, SystemProperties systemPropeprties, Configurator configurator ) {
		this.teapot = new Teapot(systemPropeprties, communicator, configurator);
		this.serial = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
		this.response = response;
		setName("Teapot Task Runner");
	}
	
	public double getTime() { 
		double estimatedTime = ( System.nanoTime() - startTimeNano ) / 1000000000.0;
		return estimatedTime;
	}
	
	@Override
	public void run() {
		startTime = DateLibrary.getInstance().getHourTextHuman();
		startTimeNano = System.nanoTime();
		try {
			logger.debug("[" + serial + "] runner thread start");
			
			// Blocking call 
			teapot.process( response );
			
			logger.debug("[" + serial + "] runner thread end");
		} catch ( Exception e ) {
			logger.error("[" + serial + "] " + e.getMessage() );
		}
		active = false;
	}
	
}
