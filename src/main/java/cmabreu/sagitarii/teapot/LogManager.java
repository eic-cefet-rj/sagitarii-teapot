package cmabreu.sagitarii.teapot;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LogManager {
	private static Map<String,Logger> allLoggers = new HashMap<String,Logger>();
	private static boolean enabled = true;
	
	public static Logger getLogger( String className ) {
		Logger newLogger = new Logger( className, enabled ); 
		allLoggers.put(className, newLogger);
		return newLogger;
	}
	
	public static void listLoggers() {
		while ( allLoggers.entrySet().iterator().hasNext() ) {
			System.out.println( allLoggers.entrySet().iterator().next() );
		}
	}
	
	public static void disableLoggers() {
		enabled = false;
		for ( Entry entry : allLoggers.entrySet() ) {
			Logger logger  = ( (Entry<String,Logger>)entry ).getValue();
			logger.disable();
		}
	}

	public static void enableLoggers() {
		enabled = true;
		for ( Entry entry : allLoggers.entrySet() ) {
			Logger logger  = ( (Entry<String,Logger>)entry ).getValue();
			logger.enable();
		}
	}

	public static void enableLogger( String className ) {
		enabled = true;
		for ( Entry entry : allLoggers.entrySet() ) {
			Logger logger  = ( (Entry<String,Logger>)entry ).getValue();
			if ( logger.getName().contains( className )  ) {
				logger.enable();
			}
		}
	}

	public static void disableLogger( String className ) {
		enabled = true;
		for ( Entry entry : allLoggers.entrySet() ) {
			Logger logger  = ( (Entry<String,Logger>)entry ).getValue();
			if ( logger.getName().contains( className )  ) {
				logger.disable();
			}
		}
	}

	
}
