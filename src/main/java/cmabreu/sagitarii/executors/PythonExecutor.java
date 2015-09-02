package cmabreu.sagitarii.executors;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.python.util.PythonInterpreter;

import cmabreu.sagitarii.teapot.Activation;

// http://www.jython.org/jythonbook/en/1.0/JythonAndJavaIntegration.html

public class PythonExecutor implements IExecutor {
	private List<String> console;
	
	@Override
	public int execute( Activation activation ) {
		String command = activation.getCommand();
		String workFolder = activation.getNamespace();
		String libraryFolder = activation.getWrappersFolder();
		
		console = new ArrayList<String>();
		console.add("will start python script " + command);
		int result = 0;
		
		PythonInterpreter interpreter = new PythonInterpreter();
		interpreter.setOut( System.out );
		interpreter.set("sagitariiWorkFolder", workFolder);
		interpreter.set("libraryFolder", libraryFolder);
		
		try {
			InputStream is = new FileInputStream( command ); 
			interpreter.execfile( is );
		} catch ( Exception e ) {
			for ( StackTraceElement ste : e.getStackTrace() ) {
				console.add( ste.toString() );
			}
			result = 1;
		}
		
		interpreter.close();
		console.add("done");
		return result;
	}

	@Override
	public List<String> getConsole() {
		return console;
	}

}
