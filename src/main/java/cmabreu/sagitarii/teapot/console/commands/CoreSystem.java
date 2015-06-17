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

import cmabreu.sagitarii.teapot.Main;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class CoreSystem implements CommandLine.ICommand {

	@Override
	public boolean doIt( List<String> v ) {

		if ( v.size() == 1 ) {
			System.out.println("usage: system <pause | resume | whoami | loadlevel>");
			return true;
		}

		
		if ( v.get(1).equals("pause") ) {
			Main.pause();
			System.out.println("System paused");
		}
		if ( v.get(1).equals("resume") ) {
			Main.resume();
			System.out.println("System resumed");
		}
		if ( v.get(1).equals("whoami") ) {
			System.out.println( Main.getConfigurator().getSystemProperties().getMachineName() + " " + 
					Main.getConfigurator().getSystemProperties().getMacAddress() + " " + 
					Main.getConfigurator().getSystemProperties().getLocalIpAddress() );
		}
		
		if ( v.get(1).equals("loadlevel") ) {
			System.out.println( "Tasks: " + Main.getRunners().size() + " CPU at " + Main.getConfigurator().getSystemProperties().getCpuLoad() + "%" ); 
		}

		return true;
	}
	
}