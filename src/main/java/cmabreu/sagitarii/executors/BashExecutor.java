package cmabreu.sagitarii.executors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cmabreu.sagitarii.teapot.Activation;

public class BashExecutor implements IExecutor {
	private List<String> console;

	@Override
	public List<String> getConsole() {
		return console;
	}
	
	@Override
	public int execute( Activation activation ) {
		String command = activation.getCommand();
		String workFolder = activation.getNamespace();
		
		console = new ArrayList<String>();
		console.add("will start " + command);

		List<String> commands = new ArrayList<String>();
		int result = 0;
		File folder = new File( workFolder );
		
	    commands.add("/bin/sh");
	    commands.add("-c");
	    commands.add( command );
		
	    try {
			SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
			result = commandExecutor.executeCommand( folder );
		    console.addAll( commandExecutor.getStandardOutputFromCommand() );
	    } catch ( Exception e ) {
	    	result = 1;
	    }
		
	    console.add("done");
	    return result;
		
	}
	
	
}
