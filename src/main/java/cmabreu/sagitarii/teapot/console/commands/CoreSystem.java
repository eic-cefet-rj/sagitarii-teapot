package cmabreu.sagitarii.teapot.console.commands;

import java.util.List;

import cmabreu.sagitarii.teapot.Main;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class CoreSystem implements CommandLine.ICommand {

	@Override
	public boolean doIt( List<String> v ) {

		if ( v.size() == 1 ) {
			System.out.println("usage: system <pause | resume | whoami | occupation>");
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
		
		if ( v.get(1).equals("occupation") ) {
			System.out.println( "Tasks: " + Main.getRunners().size() + " CPU at " + Main.getConfigurator().getSystemProperties().getCpuLoad() + "%" ); 
		}

		return true;
	}
	
}