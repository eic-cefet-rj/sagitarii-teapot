package cmabreu.taskmanager.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import cmabreu.sagitarii.teapot.Activation;
import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;

public class ExternalTask implements ITask {
	private ITaskManager monitor;
	private List<String> sourceData;
	private String applicationName;
	private TaskStatus status;
	private int exitCode;
	private Activation activation;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	
	
	@Override
	public List<String> getSourceData() {
		return sourceData;
	}

	@Override
	public void setSourceData(List<String> sourceData) {
		this.sourceData = sourceData;
	}

	@Override
	public TaskStatus getTaskStatus() {
		return this.status;
	}
	
	@Override
	public String getApplicationName() {
		return this.applicationName;
	}
	
	@Override
	public String getTaskId() {
		return this.activation.getTaskId();
	}	
	
	
	public ExternalTask( ITaskManager monitor, Activation activation, String applicationName ) {
		this.applicationName = applicationName;
		this.monitor = monitor;
        this.activation = activation;
        status = TaskStatus.STOPPED;
        this.activation = activation;
	}
	
	@Override
	public void run() {
		Process process = null;
        status = TaskStatus.RUNNING;
        try {
        	process = Runtime.getRuntime().exec(applicationName);
        	
        	InputStream in = process.getInputStream(); 
        	BufferedReader br = new BufferedReader( new InputStreamReader(in) );
        	String line = null;
        	while( ( line=br.readLine() )!=null ) {
        		logger.debug( "[" + activation.getActivitySerial() + "] " + activation.getExecutor() + " > " + line );
        	}        	
        	
            process.waitFor();
            exitCode = process.exitValue();
        } catch (IOException e) {
            status = TaskStatus.ERROR;
        } catch (InterruptedException e) {
            status = TaskStatus.ERROR;
            return;
        }
        status = TaskStatus.FINISHED;
        monitor.notify( this );
    }

	@Override
	public int getExitCode() {
		return this.exitCode;
	}

	@Override
	public Activation getActivation() {
		return this.activation;
	}


}