package cmabreu.sagitarii.teapot;

import java.net.ConnectException;

import cmabreu.sagitarii.teapot.comm.Communicator;
import cmabreu.sagitarii.teapot.comm.Downloader;
import cmabreu.sagitarii.teapot.comm.Uploader;
import cmabreu.sagitarii.teapot.console.CommandLoader;
import cmabreu.taskmanager.core.TaskManager;


public class Main {
	private static Logger logger = LogManager.getLogger( "cmabreu.sagitarii.teapot.Main" ); 
	private static CommandLoader cm;
	private static TaskManager tm;
	private static Teapot teapot;
	private static Configurator gf;
	private static boolean paused = false;
	
	public static void pause() {
		paused = true;
	}
	
	public static Configurator getConfigurator() {
		return gf;
	}

	public static void resume() {
		paused = false;
	}

	public static Teapot getTeapot() {
		return teapot;
	}
	
	public static TaskManager getTaskManager() {
		return tm;
	}

	public static void main( String[] args ) {
		boolean wrappersDownloaded = false;
		try {
			System.out.println("");
	    	System.out.println("Sagitarii Teapot Node v1.0.125        23/04/2015");
	    	System.out.println("Carlos Magno Abreu        magno.mabreu@gmail.com");
			System.out.println("------------------------------------------------");
			System.out.println("");

			logger.debug("Loading Repository Manager ...");
			
			gf = new Configurator("config.xml");
			gf.loadMainConfig();
			RepositoryManager rm = new RepositoryManager();

			logger.debug("Loading Task Manager ...");
			tm = new TaskManager();

			logger.debug("Available processors: " + tm.getAvailableProcessors() + " cores." );
			logger.debug("SO name / machine: " + tm.getSoName() + " / " + tm.getMachineName() );
			logger.debug("SO family: " + tm.getOsType() );
			logger.debug("IP/MAC: " +  tm.getLocalIpAddress() + " / " + tm.getMacAddress() );
			logger.debug("Java version " + tm.getJavaVersion() );
			
			logger.debug("Announce interval: " + gf.getPoolIntervalMilliSeconds() +"ms." );
			logger.debug("Sagitarii URL: " + gf.getHostURL() );

			logger.debug("R Processor location: " + gf.getrPath());

			
			if ( gf.useProxy() ) {
				logger.debug("Proxy: " + gf.getProxyInfo().getHost() );
			}
			if ( !gf.getShowConsole() ) {
				logger.debug("No activations console.");
			}
			
			logger.debug("Searching for wrappers...");
			try {
				rm.downloadWrappers( gf.getHostURL(), tm.getOsType() );
				wrappersDownloaded = true;
			} catch ( ConnectException e ) {
				logger.error("Cannot download wrappers. Will interrupt startup until Sagitarii is up.");
			}

			logger.debug("Staring communicator...");
			Communicator comm = new Communicator( gf, tm );
			teapot = new Teapot(tm, comm, gf);
			
			if ( wrappersDownloaded ) {
				logger.debug("Teapot started.");
			}
			
			// =============================================================
			// =============================================================
			if ( args.length > 0) {
				
				if( args[0].equalsIgnoreCase("interactive") ) {
					logger.debug("Stating interactive mode...");
					cm = new CommandLoader();
					cm.start();
					LogManager.disableLoggers();
				}
				
				if( args[0].equalsIgnoreCase("upload") )   {
					if ( args.length != 5 ) {
						System.out.println("Use teapot upload <arquivo.csv> <nome da relacao> <tag do experimento> <pasta de arquivos>");
						System.exit(0);
					}
					String fileName = args[1];
					String relationName = args[2];
					String experimentSerial = args[3];
					String folderName = args[4];

					new Uploader(gf).uploadCSV(fileName, relationName, experimentSerial, folderName, null, tm.getMacAddress() );
					
					System.exit(0);
				}
				
				if ( args[0].equalsIgnoreCase("pwd") ) {
					System.out.println( new Activation().getNamespace() );
					System.exit(0);
				}
				
				if ( args[0].equalsIgnoreCase("download") ) {
					String fileId = args[1];
					String saveTo = args[2];
					String url = gf.getHostURL() + "/getFile?idFile=" + fileId;
					new Downloader().download(url, saveTo, true);
					System.exit(0);
				}

			}
			// =============================================================
			// =============================================================
			
			while (true) {
				
				System.out.println("MAIN: process");
				
				if ( !wrappersDownloaded ) {
					try {
						logger.debug("Searching for wrappers...");
						rm.downloadWrappers( gf.getHostURL(), tm.getOsType() );
						wrappersDownloaded = true;
						logger.debug("Done. Teapot Started.");
					} catch ( ConnectException e ) {
						logger.error("Cannot download wrappers. Skipping.");
					}
				} else {
					if ( !paused ) {
						teapot.process();
					}
				}
				/*
				try {
				    Thread.sleep( 300 );
				    System.out.println("MAIN: will sleep " + gf.getPoolIntervalMilliSeconds() );
				} catch( InterruptedException ex ) {
				
				}
				*/			
			}
			
			
		} catch (Exception e) {
			logger.debug("Critical error. Cannot start Teapot Node.");
			logger.debug("Error details:");
			e.printStackTrace();
		}

	}

}
