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

public class Logger {
	private org.apache.logging.log4j.Logger internalLogger;
	private boolean enabled = true;
	private String name;
	
	public void disable() {
		enabled = false;
	}

	public void enable() {
		enabled = true;
	}
	
	public String getName() {
		return name;
	}
	
	public Logger( String logger, boolean enabled ) {
		internalLogger = org.apache.logging.log4j.LogManager.getLogger( logger );
		this.enabled = enabled;
		this.name = logger;
	}

	public void debug( String what ) {
		if ( enabled ) internalLogger.debug( what );
	}

	public void error( String what ) {
		if ( enabled ) internalLogger.error( what );
	}

	public void warn( String what ) {
		if ( enabled ) internalLogger.warn( what );
	}

	
}
