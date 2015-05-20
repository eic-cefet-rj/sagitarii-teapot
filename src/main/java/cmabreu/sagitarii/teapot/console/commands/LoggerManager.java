package cmabreu.sagitarii.teapot.console.commands;

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