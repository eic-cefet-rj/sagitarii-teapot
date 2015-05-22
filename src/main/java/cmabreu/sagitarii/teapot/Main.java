package cmabreu.sagitarii.teapot;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;

import cmabreu.sagitarii.teapot.comm.Communicator;
import cmabreu.sagitarii.teapot.comm.Downloader;
import cmabreu.sagitarii.teapot.comm.Uploader;
import cmabreu.sagitarii.teapot.console.CommandLoader;


public class Main {
	private static Logger logger = LogManager.getLogger( "cmabreu.sagitarii.teapot.Main" ); 
	private static CommandLoader cm;
	private static SystemProperties systemProperties;
	private static Configurator configurator;
	private static boolean paused = false;
	private static List<TaskRunner> runners = new ArrayList<TaskRunner>();
	private static boolean restarting = false;
	private static boolean reloading = false;
	private static boolean quiting = false;
	
	public static SystemProperties getSystemProperties() {
		return systemProperties;
	}
	
	public static void pause() {
		paused = true;
	}
	
	public static Configurator getConfigurator() {
		return configurator;
	}

	public static void resume() {
		paused = false;
	}

	public static SystemProperties getTaskManager() {
		return systemProperties;
	}

	/**
	 * Remover o diretório raiz do namespace
	 * Chamado antes de iniciar os trabalhos para sempre ter um namespace limpo.
	 */
	private static void cleanUp() {
		try {
			FileUtils.deleteDirectory( new File( "namespaces" ) ); 
		} catch ( IOException e ) {
			logger.error( e.getMessage() ); 
		}
	}

	/**
	 * Teapot entry point
	 * 
	 * EX UNITATE VIRES !
	 * 
	 * @param args parameters
	 */
	public static void main( String[] args ) {
		boolean wrappersDownloaded = false;
		try {
			System.out.println("");
	    	System.out.println("Sagitarii Teapot Node v1.0.125        23/04/2015");
	    	System.out.println("Carlos Magno Abreu        magno.mabreu@gmail.com");
			System.out.println("------------------------------------------------");
			System.out.println("");

			logger.debug("Loading Repository Manager ...");
			
			configurator = new Configurator("config.xml");
			configurator.loadMainConfig();
			RepositoryManager rm = new RepositoryManager();

			logger.debug("Loading Task Manager ...");
			systemProperties = new SystemProperties();

			logger.debug("Available processors: " + systemProperties.getAvailableProcessors() + " cores." );
			logger.debug("SO name / machine: " + systemProperties.getSoName() + " / " + systemProperties.getMachineName() );
			logger.debug("SO family: " + systemProperties.getOsType() );
			logger.debug("IP/MAC: " +  systemProperties.getLocalIpAddress() + " / " + systemProperties.getMacAddress() );
			logger.debug("Java version " + systemProperties.getJavaVersion() );
			
			logger.debug("Announce interval: " + configurator.getPoolIntervalMilliSeconds() +"ms." );
			logger.debug("Sagitarii URL: " + configurator.getHostURL() );

			logger.debug("R Processor location: " + configurator.getrPath());

			logger.debug("cleaning workspace...");
			cleanUp();
			
			if ( configurator.useProxy() ) {
				logger.debug("Proxy: " + configurator.getProxyInfo().getHost() );
			}
			if ( !configurator.getShowConsole() ) {
				logger.debug("No activations console.");
			}
			
			logger.debug("Searching for wrappers...");
			try {
				rm.downloadWrappers( configurator.getHostURL(), systemProperties.getOsType() );
				wrappersDownloaded = true;
			} catch ( ConnectException e ) {
				logger.error("Cannot download wrappers. Will interrupt startup until Sagitarii is up.");
			}

			logger.debug("Staring communicator...");
			Communicator communicator = new Communicator( configurator, systemProperties );
			
			
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
						System.out.println("Use teapot upload <file.csv> <target_table> <experiment_tag> <work_folder>");
						System.exit(0);
					}
					String fileName = args[1];
					String relationName = args[2];
					String experimentSerial = args[3];
					String folderName = args[4];

					new Uploader(configurator).uploadCSV(fileName, relationName, experimentSerial, folderName, null, systemProperties );
					
					System.exit(0);
				}
				
				if ( args[0].equalsIgnoreCase("pwd") ) {
					System.out.println( new Activation().getNamespace() );
					System.exit(0);
				}
				
				if ( args[0].equalsIgnoreCase("download") ) {
					String fileId = args[1];
					String saveTo = args[2];
					String url = configurator.getHostURL() + "/getFile?idFile=" + fileId;
					new Downloader().download(url, saveTo, true);
					System.exit(0);
				}

			}
			// =============================================================
			// =============================================================
			
			while (true) {
				clearRunners();
				logger.debug( "init new cycle: " + runners.size() + " of " + configurator.getActivationsMaxLimit() + " tasks running:" );
				
				for ( TaskRunner tr : runners ) {
					if ( tr.getCurrentTask() != null ) {
						String time = tr.getStartTime() + " (" + tr.getTime() + "s)";
						logger.debug( " > " + tr.getCurrentTask().getTaskId() + " (" + tr.getCurrentTask().getActivation().getExecutor() + ") : " + time);
					}
				}
				
				SpeedEqualizer.equalize( configurator, runners.size() );
				
				if ( !wrappersDownloaded ) {
					try {
						logger.debug("Searching for wrappers...");
						rm.downloadWrappers( configurator.getHostURL(), systemProperties.getOsType() );
						wrappersDownloaded = true;
						logger.debug("Done. Teapot Started.");
					} catch ( ConnectException e ) {
						logger.error("Cannot download wrappers. Skipping.");
					}
				} else {
					if ( !paused ) {
						
						if ( runners.size() < configurator.getActivationsMaxLimit() ) {
							logger.debug( "asking Sagitarii for tasks to process...");
							String response = communicator.announceAndRequestTask( systemProperties.getCpuLoad() );
							if ( response.length() > 0 ) {
								logger.debug("Sagitarii answered " + response.length() + " bytes");
								if ( preProcess( response ) ) {
									TaskRunner tr = new TaskRunner(response, communicator, systemProperties, configurator);
									runners.add(tr);
									tr.start();
								}
							} else {
								logger.debug("nothing to do for now");
							}
						}
					}
				}
				
				try {
				    Thread.sleep( configurator.getPoolIntervalMilliSeconds() );
				} catch( InterruptedException ex ) {
				
				}
						
			}
			
			
		} catch (Exception e) {
			logger.debug("Critical error. Cannot start Teapot Node.");
			logger.debug("Error details:");
			e.printStackTrace();
		}

	}
	
	/**
	 * Will check if Sagitarii sent a special command to this node
	 * 
	 * @param response Sagitarii response
	 * @return if can process XML file or not
	 */
	private static boolean preProcess( String response ) {
		logger.debug("checking preprocess");
		if ( quiting ) {
			// If we're here, is becaus the first call not finished (have tasks still running),
			// so we'll try again
			quit();
			return false;
		}
		if ( restarting ) {
			// If we're here, is becaus the first call not finished (have tasks still running), 
			// so we'll try again
			restart();
			return false;
		}
		if ( reloading ) {
			// If we're here, is becaus the first call not finished (have tasks still running), 
			// so we'll try again
			reloadWrappers();
			return false;
		}
		
		if ( ( !response.equals( "NO_ANSWER" ) ) && ( !response.equals( "COMM_ERROR" ) ) && ( !response.equals( "" ) ) ) {
			if ( response.equals( "COMM_RESTART" ) ) {
				logger.debug("get restart command from Sagitarii");
				restart();
			} else
			if ( response.equals( "RELOAD_WRAPPERS" ) ) {
				logger.debug("get reload wrappers command from Sagitarii");
				reloadWrappers();
			} else
			if ( response.equals( "COMM_QUIT" ) ) {
				logger.debug("get quit command from Sagitarii");
				quit();
			} else
			if ( response.equals( "COMM_CLEAN_WORKSPACE" ) ) {
				logger.debug("get clean workspace command from Sagitarii");
				if (  getRunners().size() > 0 ) {
					logger.debug("will not clean workspace. " + getRunners().size() + " tasks still runnig");
				} else {
					cleanUp();
					logger.debug("workspace cleaned");
				}
			} 
		}
		
		return true;
	}
	
	/**
	 * Will restart Teapot
	 * It is a Sagitarii command
	 */
	public static void restartApplication() {
		try {
		  final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		  final File currentJar = new File ( Teapot.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	
		  /* is it a jar file? */
		  if( !currentJar.getName().endsWith(".jar") ) {
		    return;
		  }
	
		  /* Build command: java -jar application.jar */
		  final ArrayList<String> command = new ArrayList<String>();
		  command.add( javaBin );
		  command.add( "-jar" );
		  command.add( currentJar.getPath() );
	
		  final ProcessBuilder builder = new ProcessBuilder(command);
		  builder.start();
		  System.exit(0);
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Restart Teapot
	 */
	private static void restart() {
		restarting = true;
		if ( getRunners().size() > 0 ) {
			logger.debug("cannot restart now. " + getRunners().size() + " tasks still runnig");
		} else {
			logger.debug("restart now.");
			restartApplication();
		}
	}
	
	/**
	 * Close Teapot
	 */
	private static void quit() {
		quiting = true;
		if ( getRunners().size() > 0 ) {
			logger.debug("cannot quit now. " + getRunners().size() + " tasks still runnig");
		} else {
			logger.debug("quit now.");
			 System.exit(0);
		}
	}
	
	/**
	 * Download all wrappers from Sagitarii again
	 */
	private static void reloadWrappers() {
		reloading = true;
		if ( getRunners().size() > 0 ) {
			logger.debug("cannot reload wrappers now. " + getRunners().size() + " tasks still runnig");
		} else {
			logger.debug("reload all wrappers now.");
			try {
				RepositoryManager rm = new RepositoryManager();
				rm.downloadWrappers( configurator.getHostURL(), systemProperties.getOsType() );
				logger.debug("all wrappers reloaded.");
			} catch ( Exception e ) {
				logger.error("cannot reload wrappers: " + e.getMessage() );
			}
			reloading = false;
		}
	}

	
	public static List<TaskRunner> getRunners() {
		return new ArrayList<TaskRunner>( runners );
	}
	
	/**
	 * Remove all finished task threads from buffer
	 *  
	 */
	private static void clearRunners() {
		logger.debug("cleaning task runners...");
		int total = 0;
		Iterator<TaskRunner> i = runners.iterator();
		while ( i.hasNext() ) {
			TaskRunner req = i.next(); 
			if ( !req.isActive() ) {
				logger.debug(" > killing task runner " + req.getSerial() + " (" + req.getCurrentTask().getTaskId() + ")" );
				i.remove();
				total++;
			}
		}		
		logger.debug( total + " task runners deleted" );
	}

}
