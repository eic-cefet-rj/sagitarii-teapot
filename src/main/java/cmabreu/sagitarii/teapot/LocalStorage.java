package cmabreu.sagitarii.teapot;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cmabreu.sagitarii.teapot.comm.Downloader;
import cmabreu.sagitarii.teapot.comm.FileUnity;

public class LocalStorage {
	private Configurator configurator;
	private static LocalStorage instance;
	private Map<String, Integer> lockers;

	public String getLocation() {
		return configurator.getSystemProperties().getLocalStorage();
	}
	
	public static LocalStorage getInstance( Configurator configurator ) {
		if ( instance == null ) {
			instance = new LocalStorage( configurator );
		} 
		return instance;
	}
	
	private LocalStorage( Configurator configurator ) {
		this.configurator = configurator;
		this.lockers = new HashMap<String,Integer>();
	}
	
	private synchronized void requestFileLock( FileUnity file ) {
		System.out.println("request file lock: " + file.getName() );

		String uuid = UUID.fromString( file.getName() + String.valueOf( file.getId() ) + file.getSourceTable() ).toString();
		lockers.put( uuid, file.getId() );
	}

	private synchronized boolean isFileLocked( FileUnity file ) {
		System.out.println("check file lock: " + file.getName() + ": " + lockers.containsValue( file.getId() ) );
		
		return lockers.containsValue( file.getId() );
	}
	
	private synchronized void releaseFileLock( FileUnity file ) {
		System.out.println("release file lock: " + file.getName() );

		String uuid = UUID.fromString( file.getName() + String.valueOf( file.getId() ) + file.getSourceTable() ).toString();
		lockers.remove(uuid);
	}
	
	public synchronized void downloadAndCopy( FileUnity file, String dest, Downloader dl ) throws Exception {
		String url = configurator.getHostURL() + "/getFile?idFile="+ file.getId()+"&macAddress=" + configurator.getSystemProperties().getMacAddress();
		String targetPath = getLocation() + "/" + file.getId() + "/";
		String targetFile = targetPath + file.getName();
		
		// is other activation already downloading this file? 
		while ( isFileLocked(file) ) {
			System.out.println("waiting file lock to release");
		}
		
		// try to copy from local repo ( someone finished download )
		if ( !copy( file, dest) ) {
			// if can't, try to download from sagitarii...
			requestFileLock( file );
			File trgt = new File( targetPath );
			trgt.mkdir();
			dl.download(url, targetFile, true);
			releaseFileLock(file);
			// ... and copy from local repo
			copy( file, dest);
		}
	}
	
	public boolean copy( FileUnity file, String dest ) {
		String source = getLocation() + "/" + file.getId() + "/" + file.getName();
		try {
			File src = new File(source);
			File trgt = new File(dest);
			if ( src.exists() ) {
			    Files.copy( src.toPath(), trgt.toPath() );
			    return true;
			} else {
				return false;
			}
		} catch ( Exception e ) {
			return false;
		}
	}

	
}
