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

import cmabreu.sagitarii.teapot.Activation;
import cmabreu.sagitarii.teapot.Main;
import cmabreu.sagitarii.teapot.TaskRunner;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class Show implements CommandLine.ICommand {

	@Override
	public boolean doIt( List<String> v ) {

		if ( v.size() == 1 ) {
			System.out.println("usage: show <tasks | total | instances>");
			return true;
		}
		
		if ( v.get(1).equals("total") ) {
			System.out.println("Total instances processed until now: " + Main.getTotalInstancesProcessed() );
		}
		
		
		if ( v.get(1).equals("instances") ) {
			System.out.println("Found " + Main.getRunners().size() + " running tasks:");
			for ( TaskRunner taskRequester : Main.getRunners() ) {
				if ( taskRequester.getCurrentTask() != null ) {
					String time = taskRequester.getStartTime() + " (" + taskRequester.getTime() + ")";
					
					String line = taskRequester.getCurrentActivation().getInstanceSerial() + " ";
					
					for ( Activation activation :  taskRequester.getJobPool() ) {
						line = line + "[" + activation.getExecutor() + " | " + activation.getStatus().toString() + "] ";
					}

					line = line + time;
					System.out.println( line );
					
				}
			}
		}
		
		
		if ( v.get(1).equals("tasks") ) {
			System.out.println("Found " + Main.getRunners().size() + " running tasks:");
			for ( TaskRunner taskRequester : Main.getRunners() ) {
				if ( taskRequester.getCurrentActivation() != null ) {
					String time = taskRequester.getStartTime() + " (" + taskRequester.getTime() + ")";
					System.out.println( " > " +  
							taskRequester.getCurrentActivation().getExperiment() + 
							"/" + taskRequester.getCurrentActivation().getActivitySerial() + 
							" " + taskRequester.getCurrentTask().getTaskId() + " (" + 
							taskRequester.getCurrentActivation().getExecutor() + ") : " + time);
				}
			}
		}

		return true;
	}
	
}