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

import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cmabreu.sagitarii.teapot.comm.Communicator;

public class TaskRunner extends Thread {
	private Teapot teapot;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private String serial;
	private String response;
	private boolean active = true;
	private String startTime;  
	private long startTimeMillis;    

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
	
	public Activation getCurrentActivation() {
		return teapot.getCurrentActivation();
	}
	
	public TaskRunner( String response, Communicator communicator, Configurator configurator ) {
		this.teapot = new Teapot( communicator, configurator);
		this.serial = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
		this.response = response;
		setName("Teapot Task Runner " + this.serial );
	}
	
	public String getTime() {
		long millis = getTimeMillis(); 

		String time = String.format("%03d %02d:%02d:%02d", 
				TimeUnit.MILLISECONDS.toDays( millis ),
				TimeUnit.MILLISECONDS.toHours( millis ),
				TimeUnit.MILLISECONDS.toMinutes( millis ) -  
				TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours( millis ) ), 
				TimeUnit.MILLISECONDS.toSeconds( millis ) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes( millis ) ) );

		return time;
	}
	
	public long getTimeMillis() { 
		long estimatedTime = ( Calendar.getInstance().getTimeInMillis() - startTimeMillis );
		return estimatedTime;
	}
	
	@Override
	public void run() {
		startTime = DateLibrary.getInstance().getHourTextHuman();
		startTimeMillis = Calendar.getInstance().getTimeInMillis();
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
