package cmabreu.sagitarii.executors;

import java.util.List;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

public class TextConsole implements RMainLoopCallbacks {
	private List<String> console;
	
	public TextConsole( List<String> console ) {
		this.console = console;
	}
	
    public void rWriteConsole(Rengine re, String text, int oType) {
    	console.add( text );
    }
    
    public void rBusy(Rengine re, int which) {
        console.add( "rBusy("+which+")" );
    }
    
    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        return null;
    }
    
    public void rShowMessage(Rengine re, String message) {
    	console.add( message );
    }
	
    public String rChooseFile(Rengine re, int newFile) {
    	return null;
    }
    
    public void   rFlushConsole (Rengine re) {
    }
	
    public void   rLoadHistory  (Rengine re, String filename) {
    }			
    
    public void   rSaveHistory  (Rengine re, String filename) {
    }
    
}
