package cmabreu.sagitarii.teapot.console.commands;

import java.util.List;

import cmabreu.sagitarii.teapot.console.CommandLine;

public class Show implements CommandLine.ICommand {

	@Override
	public boolean doIt( List<String> v ) {

		/*
		if ( v.get(1).equals("tasks") ) {
			System.out.println("Running tasks:");
			for ( TaskRequester taskRequester : Main.getRequesters() ) {
				Activation act = taskRequester.getActivation();
				System.out.println(" > " + act.getActivitySerial() + " " + act.getExecutorType() + " " + act.getExecutor() );
			}
		}
		*/

		return true;
	}
	
}