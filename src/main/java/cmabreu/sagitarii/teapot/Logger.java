package cmabreu.sagitarii.teapot;

public class Logger {
	private org.apache.logging.log4j.Logger internalLogger;
	private boolean enabled = true;
	private String name;
	
	public void disable() {
		enabled = false;
	}

	public void enable() {
		enabled = true;
	}
	
	public String getName() {
		return name;
	}
	
	public Logger( String logger, boolean enabled ) {
		internalLogger = org.apache.logging.log4j.LogManager.getLogger( logger );
		this.enabled = enabled;
		this.name = logger;
	}

	public void debug( String what ) {
		if ( enabled ) internalLogger.debug( what );
	}

	public void error( String what ) {
		if ( enabled ) internalLogger.error( what );
	}

	public void warn( String what ) {
		if ( enabled ) internalLogger.warn( what );
	}

	
}
