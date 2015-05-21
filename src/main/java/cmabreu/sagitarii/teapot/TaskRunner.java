package cmabreu.sagitarii.teapot;

import java.util.List;
import java.util.UUID;

import cmabreu.sagitarii.teapot.comm.Communicator;
import cmabreu.taskmanager.core.DateLibrary;
import cmabreu.taskmanager.core.SystemProperties;
import cmabreu.taskmanager.core.Task;

public class TaskRunner extends Thread {
	private Teapot teapot;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private String serial;
	private String response;
	private boolean active = true;
	private String startTime;  
	private long startTimeNano;    

	public String getStartTime() {
		return startTime;
	}
	
	public boolean isActive() {
		return active;
	}

	public String getSerial() {
		return serial;
	}
	
	public List<Activation> getExecutionQueue() {
		return teapot.getExecutionQueue();
	}
	
	public Task getCurrentTask() {
		return teapot.getCurrentTask();
	}
	
	public TaskRunner( String response, Communicator communicator, SystemProperties systemPropeprties, Configurator configurator ) {
		this.teapot = new Teapot(systemPropeprties, communicator, configurator);
		this.serial = UUID.randomUUID().toString().substring(0, 5);
		this.response = response;
		setName("Teapot Task Runner");
	}
	
	public double getTime() { 
		double estimatedTime = ( System.nanoTime() - startTimeNano ) / 1000000000.0;
		return estimatedTime;
	}
	
	@Override
	public void run() {
		startTime = DateLibrary.getInstance().getHourTextHuman();
		startTimeNano = System.nanoTime();
		try {
			logger.debug("[" + serial + "] runner thread start");
			
			// Blocking call 
			teapot.process( response );
			
			logger.debug("[" + serial + "] runner thread end");
		} catch ( Exception e ) {
			logger.error("[" + serial + "] " + e.getMessage() );
		}
		active = false;
	}
	
}
