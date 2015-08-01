package cmabreu.sagitarii.teapot;

import java.util.HashMap;
import java.util.Map;

import cmabreu.sagitarii.teapot.comm.FileUnity;

public class StorageLocker {
	private static StorageLocker instance;
	private Map<String, Integer> lockers;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 


	private StorageLocker() {
		this.lockers = new HashMap<String,Integer>();
	}

	public static StorageLocker getInstance() {
		if ( instance == null ) {
			instance = new StorageLocker();
		}
		return instance;
	}

	
	private String getUuid( FileUnity file ) {
		return file.getSourceTable() + "." + file.getName() + "." + String.valueOf( file.getId() );
	}
	
	public synchronized void requestFileLock( FileUnity file ) {
		logger.debug("request file lock: " + file.getName() );
		if ( isFileLocked( file ) ) {
			logger.debug("already locked: " + file.getName() );
		} else {
			lockers.put( getUuid( file ), file.getId() );
		}
	}
	
	public synchronized boolean isFileLocked( FileUnity file ) {
		return lockers.containsValue( file.getId() );
	}
	
	public synchronized void releaseFileLock( FileUnity file ) {
		logger.debug("release file lock: " + file.getName() );
		lockers.remove( getUuid( file ) );
	}
	
}
