package cmabreu.sagitarii.executors;

import java.util.ArrayList;
import java.util.List;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;

public class RExecutor implements IExecutor {
	private List<String> console;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 

	@Override
	public List<String> getConsole() {
		return console;
	}
	
	@Override
	public int execute(String rScript, String workFolder) {
		console = new ArrayList<String>();
        console.add("running R Engine for script " + rScript);
        logger.debug("running R Engine for script " + rScript );
        
		try {
			Rengine rengine = new Rengine(new String [] {"--vanilla"}, false, new TextConsole( console ) );
	        if ( !rengine.waitForR() ) {
	            console.add("Cannot load R");
	            logger.error("Cannot load R");
	            return 1;
	        }
	        
	        rengine.eval("sagitariiWorkFolder <- \""+ workFolder +"\"");
	        rengine.eval( "source( '" + rScript + "') " );
	        REXP message = rengine.eval("messageToSagitarii");
	        
	        if ( message != null ) {
	        	console.add( message.toString() );
	        }

	        rengine.end();

		} catch ( Exception e ) {
	        console.add("finished with error " + e.getMessage() );
            return 1;
		}
        console.add("done.");
        
        return 0;
	}

}
