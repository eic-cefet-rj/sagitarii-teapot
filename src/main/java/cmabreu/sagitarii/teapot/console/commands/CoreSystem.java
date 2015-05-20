package cmabreu.sagitarii.teapot.console.commands;

import java.util.List;

import cmabreu.sagitarii.teapot.Main;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class CoreSystem implements CommandLine.ICommand {

	@Override
	public boolean doIt( List<String> v ) {

		if ( v.get(1).equals("pause") ) {
			Main.pause();
			System.out.println("System paused");
		}
		if ( v.get(1).equals("resume") ) {
			Main.pause();
			System.out.println("System resumed");
		}
		if ( v.get(1).equals("whoami") ) {
			System.out.println( Main.getTaskManager().getMachineName() + " " + 
					Main.getTaskManager().getMacAddress() + " " + 
					Main.getTaskManager().getLocalIpAddress() );
		}
		if ( v.get(1).equals("occupation") ) {
			System.out.println( "Tasks: " + Main.getTaskManager().getRunningTaskCount() + " CPU at " + Main.getTaskManager().getCpuLoad() + "%" ); 
		}

		if ( v.get(1).equals("record") ) {
			if ( v.size() > 2 ) {
				Main.getTaskManager().startRecord( v.get(2) );
				System.out.println("Starting record " + v.get(2) );
			}
		}

		if ( v.get(1).equals("stoprecord") ) {
			Main.getTaskManager().stopRecord();
		}

		if ( v.get(1).equals("setspeed") ) {
			if ( v.size() == 3 ) {
				int old = Main.getConfigurator().getPoolIntervalMilliSeconds(); 
				Main.getConfigurator().setPoolIntervalMilliSeconds( Integer.valueOf( v.get(2) ) );
				System.out.println("Speed set to " + v.get(2) + "ms (was " + old + "ms)");
			}
		}

		
		if ( v.get(1).equals("notify") ) {
			if ( v.size() > 2 ) {
				String toSend = "";
				for ( int x = 2; x < v.size(); x++ ) {
					toSend = toSend + v.get(x) + " ";
				}
				Main.getTeapot().sendErrorLog( v.get(2) );
			}
		}
		
		return true;
	}
	
}