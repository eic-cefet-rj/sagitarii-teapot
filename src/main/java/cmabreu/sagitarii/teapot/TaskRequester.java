package cmabreu.sagitarii.teapot;

import java.util.UUID;

import cmabreu.sagitarii.teapot.comm.Communicator;
import cmabreu.taskmanager.core.TaskManager;

public class TaskRequester extends Thread {
	private Communicator comm;
	private TaskManager tm;
	private Teapot teapot;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private String serial;
	private boolean active = true;

	public boolean isActive() {
		return active;
	}

	public TaskRequester( Communicator comm, TaskManager tm, Teapot teapot ) {
		this.comm = comm;
		this.tm = tm;
		this.teapot = teapot;
		this.serial = UUID.randomUUID().toString().substring(0, 5);
		setName("Teapot task requester");
	}
	
	@Override
	public void run() {
		logger.debug( "[" + serial + "] asking Sagitarii for tasks to process...");
		String resposta = comm.anuncia( tm.getCpuLoad() );
		try {
			logger.debug("[" + serial + "] Sagitarii answered " + resposta.length() + " bytes");
			teapot.process( resposta );
			logger.debug("[" + serial + "] requester thread end");
		} catch ( Exception e ) {
			logger.error("[" + serial + "] " + e.getMessage() );
		}
		active = false;
	}
	
}
