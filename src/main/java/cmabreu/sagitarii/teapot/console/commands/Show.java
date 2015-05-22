package cmabreu.sagitarii.teapot.console.commands;

import java.util.List;

import cmabreu.sagitarii.teapot.Main;
import cmabreu.sagitarii.teapot.TaskRunner;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class Show implements CommandLine.ICommand {

	@Override
	public boolean doIt( List<String> v ) {

		if ( v.size() == 1 ) {
			System.out.println("usage: show tasks");
			return true;
		}
		
		if ( v.get(1).equals("tasks") ) {
			System.out.println("Found " + Main.getRunners().size() + " running tasks:");
			for ( TaskRunner taskRequester : Main.getRunners() ) {
				if ( taskRequester.getCurrentTask() != null ) {
					String time = taskRequester.getStartTime() + " (" + (long)taskRequester.getTime() + "s)";
					System.out.println( " > " +  
							"/" + taskRequester.getCurrentTask().getActivation().getExperiment() + 
							"/" + taskRequester.getCurrentTask().getActivation().getActivitySerial() + 
							" " + taskRequester.getCurrentTask().getTaskId() + " (" + 
							taskRequester.getCurrentTask().getActivation().getExecutor() + ") : " + time);
				}
			}
		}

		return true;
	}
	
}