package cmabreu.sagitarii.teapot;

import java.io.File;
import java.net.URLEncoder;
import java.nio.file.Files;

import cmabreu.sagitarii.teapot.comm.Communicator;
import cmabreu.sagitarii.teapot.comm.Downloader;
import cmabreu.sagitarii.teapot.comm.FileUnity;

public class LocalStorage {
	private Configurator configurator;
	private StorageLocker locker;
	private Activation act;
	private Communicator comm;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 

	public String getLocation() {
		return configurator.getSystemProperties().getLocalStorage();
	}
	
	public LocalStorage( Communicator comm, Configurator configurator, Activation act ) {
		this.configurator = configurator;
		this.locker = StorageLocker.getInstance();
		this.act = act;
		this.comm = comm;
	}
	
	private void debug( String s ) {
		if ( !s.equals("")) {
			logger.debug( s );
			notifySagitarii( s );
		}
	}
	
	private void error( String s ) {
		logger.error( s );
		notifySagitarii( s );
	}
	
	public void notifySagitarii( String message ) {
		String executor = "STARTING";
		executor = act.getExecutor();
		message = "[" + executor + "] " + message;
		try {
			String parameters = "macAddress=" + configurator.getSystemProperties().getMacAddress() + "&errorLog=" + URLEncoder.encode( message, "UTF-8");
			comm.send("receiveErrorLog", parameters);
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	public synchronized boolean downloadAndCopy( FileUnity file, String dest, Downloader dl )  {
		String url = configurator.getHostURL() + "/getFile?idFile="+ file.getId()+"&macAddress=" + configurator.getSystemProperties().getMacAddress();
		String targetPath = getLocation() + "/" + file.getId() + "/";
		String targetFile = targetPath + file.getName();
		
		debug("will request file lock for " + file.getName() );
		while ( !locker.requestFileLock( file ) ) {
			try {
				Thread.currentThread().wait(500);
			} catch ( Exception ignored ) {
				
			}
		}

		debug("file lock for file "+file.getName()
				+ " given to task "+act.getTaskId() + ". copy or download..." );

		String source = getLocation() + "/" + file.getId() + "/" + file.getName();
		File src = new File(source);
		
		if ( !src.exists() ) {
			debug("will download "+file.getName()+" from Sagitarii");
			File trgt = new File( targetPath );
			trgt.mkdir();
			try {
				
				dl.download(url, targetFile, true);
				debug("download of file "+file.getName()+" done. checking...");
				
				trgt = new File( targetFile ); 
				if ( !trgt.exists() ) {
					error("error downloading file " + file.getName() + " from Sagitarii: FILE NOT FOUND");
				} else {
					debug("file "+file.getName()+" found at local");
				}
				
				
			} catch ( Exception e ) {
				error("error downloading file " + file.getName() + " from Sagitarii: " + e.getMessage() );
			}
			
		} else {
			debug("file "+file.getName()+" already downloaded by other task. using local storage");
		}
		
		boolean result = copy( file, dest);
		debug("will release the file lock for " + file.getName() );
		locker.releaseFileLock(file);
		return result;

	}
	
	private synchronized boolean copy( FileUnity file, String dest ) {
		String source = getLocation() + "/" + file.getId() + "/" + file.getName();
		debug("will copy " + source + " to " + dest);
		try {
			File src = new File(source);
			File trgt = new File(dest);
			if ( src.exists() ) {
			    Files.copy( src.toPath(), trgt.toPath() );
			    if ( !trgt.exists() ) {
					error("file " + file.getName() + " NOT FOUND after copy to " + dest );
					return false;
			    } else {
			    	if( src.length() != trgt.length()  ) {
						error("target file size differ from source file size" );
						return false;
			    	} else {
						debug("copy " + file.getName() + " to " + dest + ": successful");
				    	return true;
			    	}
			    }
			} else {
				error("file " + file.getName() + " NOT FOUND at source folder " + source );
				return false;
			}
		} catch ( Exception e ) {
			error( "critical error when copying file " + file.getName() + ": " + e.getMessage() );
			return false;
		}
	}

	
}
