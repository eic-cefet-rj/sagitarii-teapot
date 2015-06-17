package cmabreu.sagitarii.teapot.console.commands;
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

import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class LoggerManager implements CommandLine.ICommand {

	@Override
	public boolean doIt(List<String> v) {
		if ( v.get(1).equals("disable") ) {
			if ( v.size() == 3 ) {
				LogManager.disableLogger( v.get(2) );
			} else {
				LogManager.disableLoggers();
			}
		}
		
		if ( v.get(1).equals("enable") ) {
			
			if ( v.size() == 3 ) {
				LogManager.enableLogger( v.get(2) );
			} else {
				LogManager.enableLoggers();
			}
			
		}
		return true;
	}
	
}