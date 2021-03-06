package cmabreu.sagitarii.teapot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;

import cmabreu.sagitarii.teapot.comm.Communicator;
import cmabreu.sagitarii.teapot.comm.Downloader;
import cmabreu.sagitarii.teapot.comm.FileUnity;
import cmabreu.sagitarii.teapot.comm.Uploader;

public class TaskManager {
	private SystemProperties tm;
	private Communicator comm;
	private Configurator configurator;
	private XMLParser parser;
	private List<Activation> executionQueue;
	private List<Activation> jobPool;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private List<Task> tasks = new ArrayList<Task>();
	private Task currentTask = null;
	private Activation currentActivation;
	private List<String> execLog = new ArrayList<String>();
	
	private void debug( String s ) {
		if ( !s.equals("")) {
			logger.debug( s );
			execLog.add( s );
			notifySagitarii( s );
		}
	}
	
	private void error( String s ) {
		logger.error( s );
		execLog.add( "[ERROR] " +  s );
		notifySagitarii( s );
	}
	
	public Task getCurrentTask() {
		return currentTask;
	}
	
	public Activation getCurrentActivation() {
		return this.currentActivation;
	}

	public List<Activation> getJobPool() {
		return new ArrayList<Activation>( jobPool );
	}
	
	public List<Task> getTasks() {
		return new ArrayList<Task>( tasks );
	}
	
	public TaskManager(Communicator comm, Configurator gf) {
		this.tm = gf.getSystemProperties();
		this.comm = comm;
		this.configurator = gf;
		this.parser = new XMLParser();
		this.executionQueue = new ArrayList<Activation>();
		this.jobPool = new ArrayList<Activation>();
	}
	

	private void sanitize( Task task ) {
		if ( configurator.getClearDataAfterFinish() ) {
			try {
				FileUtils.deleteDirectory( new File( currentTask.getActivation().getNamespace() ) ); 
			} catch ( IOException e ) {
				debug( "sanitization error: " + e.getMessage() );
			}
		}
	}

	/**
	 * Check if sagi_output have some data
	 * 
	 */
	private boolean validateProduct( Activation act ) {
		String taskFolder = act.getNamespace();
		String sagiOutput = taskFolder + "/" + "sagi_output.txt";
		String outbox = taskFolder + "/" + "outbox";
		String executor = act.getExecutor(); 

		debug("check executor " + executor + " output folder " + outbox );
		
		try {
			File file = new File(sagiOutput);
			if( !file.exists() ) { 
				debug( executor + ": output CSV data file sagi_output.txt not found");
				return false;
			} 
			if ( file.length() == 0 ) { 
				debug( executor + ": output CSV data file sagi_output.txt is empty");
				return false;
			} else {
				debug( executor + ": sagi_output.txt have " + file.length() + " bytes.");
			}
			BufferedReader br = new BufferedReader( new FileReader( file ) );
			String header = br.readLine(); 					
			if ( header == null ) { 
				debug( executor + ": output CSV data file sagi_output.txt have no header line");
				br.close();
				return false;
			} 
			String line = br.readLine(); 					
			if ( line == null ) { 
				debug( executor + ": output CSV data file sagi_output.txt have no data line");
				br.close();
				return false;
			} 
			br.close();
		} catch ( Exception e ) {
			debug( "validation error: " + executor + ": " + e.getMessage() );
			return false;
		}
		
		File outboxDir = new File( outbox );
		if( outboxDir.list().length == 0 ){
			debug("no files found in outbox");
		} else {
			debug( outboxDir.list().length + " files found in outbox (first 10):");
			int limit = 10;
			if ( limit > outboxDir.list().length ) {
				limit = outboxDir.list().length;
			}
			for ( int i = 0; i < limit; i++  ) {
				debug( " > " + outboxDir.list()[i] );
			}
		}
		outboxDir = null;
		return true;
	}
	
	
	public void notifySagitarii( String message ) {
		String executor = "STARTING";
		if ( currentActivation != null ) {
			executor = currentActivation.getExecutor();
		}
		message = "[" + executor + "] " + message;
		try {
			String parameters = "macAddress=" + tm.getMacAddress() + "&errorLog=" + URLEncoder.encode( message, "UTF-8");
			comm.send("receiveNodeLog", parameters);
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Formata o comando da ativacao seguinte usando o resultado CSV da ativacao anterior
	 * 
	 * @return o comando da ativacao apos a substituicao das tags
	 */
	private String generateCommand( Activation activation ) {
		String command = "";
		String classPathParam = "-Djava.library.path=" + configurator.getSystemProperties().getJriPath();
		String workFolder = activation.getNamespace();
		String wrappersFolder = activation.getWrappersFolder();
		
		if ( activation.getExecutorType().equals("RSCRIPT") ) {
			String wrapperCommand = wrappersFolder + "r-wrapper.jar";
			String scriptFile = wrappersFolder + activation.getCommand();
			command = "java "+classPathParam+" -jar "+ wrapperCommand + " " + scriptFile + " " + workFolder + " " + wrappersFolder;
			
		} else if ( activation.getExecutorType().equals("BASH") ) {
			command = wrappersFolder + activation.getCommand() + " " + workFolder + " " + wrappersFolder;
		} else if ( activation.getExecutorType().equals("PYTHON") ) {
			command = wrappersFolder + activation.getCommand();
		} else {
			command = "java "+classPathParam+" -jar " + wrappersFolder + activation.getCommand() + " " + workFolder + " " + wrappersFolder;
		}
		return command;
	}
	
	private void executeNext( Task task ) {
		debug("searching for instance tasks for task " + currentTask.getActivation().getExecutor() + " (index " + currentTask.getActivation().getOrder() + ") fragment " + currentTask.getActivation().getFragment() 
				+ " exit code: " + task.getExitCode() + " buffer size: " + task.getSourceData().size());
		Activation previousActivation = currentTask.getActivation();
		int nextOrder = previousActivation.getOrder() + 1;
		String fragmentId = previousActivation.getFragment();
		if ( (task.getExitCode() == 0) && ( task.getSourceData().size() > 1 ) ) {
			for ( Activation nextAct : executionQueue ) {
				debug(" > checking task " + nextAct.getExecutor() + " order " + nextAct.getOrder() + " fragment " + nextAct.getFragment() );
				if( (nextAct.getOrder() == nextOrder) && ( nextAct.getFragment().equals(fragmentId) ) ) {
					debug( " > accepted." );
					executionQueue.remove( nextAct );

					String wrappersFolder = configurator.getSystemProperties().getTeapotRootFolder() + "wrappers/";
					nextAct.setWrappersFolder(wrappersFolder);
					
					String newCommand = generateCommand( nextAct );
					nextAct.setCommand( newCommand );
					nextAct.setSourceData( task.getSourceData() );
					nextAct.setPreviousActivation( previousActivation );
					currentActivation = nextAct;
					try {
						saveInputData( nextAct );
						runTask( nextAct );
					} catch ( Exception e ) {
						error( e.getMessage() );
						comm.send("activityManagerReceiver", "instanceId=" + nextAct.getInstanceSerial() + "&response=CANNOT_EXEC&node=" + tm.getMacAddress() ); 
						debug( e.getMessage() );
					}
					return;
				} else {
					debug(" > not accepted.");
				}
			}
		} else {
			debug("task " + currentTask.getActivation().getExecutor() + " have empty buffer or error exit code.");
		}
	}
	
	
	/**
	 * Run a wrapper task
	 * BLOCKING
	 */
	private void runTask( Activation activation ) {
		String instanceId = activation.getInstanceSerial();
		int order = activation.getOrder();
		debug("starting executor " + activation.getExecutor() );

		debug("start task " + activation.getTaskId() + "(" + activation.getType() + ") " + activation.getExecutor() + " ("+ instanceId + " :: " + order + ")");
        
		activation.setStatus( TaskStatus.RUNNING );
		
		Task task = new Task( activation, execLog );
		task.setSourceData( activation.getSourceData() );
		task.setRealStartTime( Calendar.getInstance().getTime() );
		
		try {
			comm.send("activityManagerReceiver", "instanceId=" + activation.getInstanceSerial() + "&response=RUNNING&node=" + tm.getMacAddress() + "&executor" + activation.getExecutor() );
	        tasks.add(task);
	        currentTask = task;
	        
	        // Will Block Until Finished ...
	        task.run( configurator );
	        
	        // When finished...
	        finishAndClose( task );
	        
		} catch ( Exception e ) {
			error("Sagitarii not received task RUNNING response. Maybe offline.");
		}
	}
	
	/**
	* Implementation of ITaskObserver.notify()
	*/
	public synchronized void finishAndClose( Task task ) {
		debug("task " + task.getTaskId() + "("+ currentTask.getActivation().getExecutor() + ") finished. (" + task.getExitCode() + ")" );
		try {
			
			Activation act = task.getActivation();
			act.setStatus( TaskStatus.FINISHED );
			task.setRealFinishTime( Calendar.getInstance().getTime() );
			
			
			// Check output file
			if ( !validateProduct( act ) ) {
				error(currentTask.getActivation().getExecutor() + ": NO OUTPUT CSV DATA FOUND");
			} else {
				debug("product is valid.");
			}
				
			// Send data and files // Do not be tempted to simplify the "act" parameter. It must be this way
			// because upload command line passes this parameters too
			debug("uploading results to Sagitarii...");
			new Uploader(configurator).uploadCSV("sagi_output.txt", act.getTargetTable(), act.getExperiment(), 
					act.getNamespace(), task, tm );

			debug("uploading to Sagitarii done. Will try to execute next Activity in this Instance");
			// Run next task in same instance (if exists)
			executeNext( task );

			// Clean up
			debug("cleaning up...");
			sanitize( task );
			
			debug("all done! leaving execution thread.");
		} catch ( Exception e ) {
			error("error finishing task " + task.getApplicationName() + " at " + currentTask.getActivation().getNamespace() + " : " + e.getMessage() );
		}
		
	}
	

	/**
	 * Cria a pasta para os dados de trabalho da tarefa e caixas de
	 * entrada e saida para os arquivos.
	 * 
	 */
	private void createWorkFolder( Activation act ) throws Exception {
		File outputFolder = new File( act.getNamespace() + "/" + "outbox" );
		outputFolder.mkdirs();

		File inputFolder = new File( act.getNamespace() + "/" + "inbox" );
		inputFolder.mkdirs();
	}

	
	private void download ( FileUnity file, Activation act, Downloader dl ) throws Exception {
		String target = act.getNamespace() + "/" + "inbox" + "/" + file.getName();
		
		debug("providing file " + file.getName() + " for " + act.getTaskId() + " (" + act.getExecutor() + ")");
		LocalStorage ls = new LocalStorage( comm, configurator, act );
		boolean result = ls.downloadAndCopy( file, target, dl);
		if ( !result ) {
			error("could not copy the file " + file.getName() );
			throw new Exception( "could not copy the file " + file.getName() );
		}
		
	}
	
	
	private void saveInputData( Activation act ) throws Exception {
		debug("start data preparation for task " + act.getExecutor() + " (Activity: " + act.getActivitySerial() + "/ Task: " + act.getTaskId() + ")" );
		if ( act.getSourceData().size() < 2 ) {
			// We need at least 2 lines ( one line for header and one line of data )
			error( "Not enough input data. Aborting..." );
			throw new Exception ("Not enough data in input CSV for Task " + act.getActivitySerial() );
		}
		createWorkFolder(act);
		
		Activation previous = act.getPreviousActivation(); 
		if ( previous != null ) {
			// So this is not the first task inside instance. 
			String previousOutbox = previous.getNamespace() + "/" + "outbox";
			String destInbox = act.getNamespace() + "/" + "inbox";

			// Copy sagi_output.txt from previous task to this task source data.
			act.setSourceData( readFile( previous.getNamespace() + "/" + "sagi_output.txt" ) );

			// Save previous output as this input
			FileWriter writer = new FileWriter( act.getNamespace() + "/" + "sagi_input.txt"); 
			for(String str: act.getSourceData() ) {
			  writer.write( str + "\n" );
			}
			writer.close();
			debug( "input data file sagi_input.txt saved with " + act.getSourceData().size() + " lines");
			
			// Copy files from previous task's output to this input box.
			File source = new File( previousOutbox );
			File dest = new File( destInbox );
			if ( !isDirEmpty( source.toPath() )  ) {
				debug(" > will copy files from previous task " + previous.getTaskId() + "..." );
				debug("   from > " + previousOutbox );
				debug("   to   > " + destInbox );
				
				FileUtils.copyDirectory( source, dest );
			}
		} else {
			// This is the first task in instance
			FileWriter writer = new FileWriter( act.getNamespace() + "/" + "sagi_input.txt"); 
			for(String str: act.getSourceData() ) {
			  writer.write( str + "\n" );
			}
			writer.close();
			debug( "input data file sagi_input.txt saved with " + act.getSourceData().size() + " lines");
			
			// Check if Sagitarii ask us to download some files...
			if ( act.getFiles().size() > 0 ) {
				debug("this task needs to download " + act.getFiles().size() + " files: ");
				Downloader dl = new Downloader();
				for ( FileUnity file : act.getFiles() ) {
					debug("need file " + file.getName() + " (id " + file.getId() + ") for attribute " + file.getAttribute() +
							" of table " + file.getSourceTable() );
					
					download(file, act, dl);
					
					File fil = new File( act.getNamespace() + "/" + "inbox" + "/" + file.getName() );
					if ( fil.exists() ) {
						debug( "file " + file.getName() + " downloaded.");
					} else {
						error( "cannot find file " + file.getName() + " after download.");
					}
					
				}
			} else {
				debug("no need to download files.");
			}
	
		}
		debug("done preparing task " + act.getExecutor() + " (" + act.getActivitySerial() + "/" + act.getTaskId() + ")" );
	}
	
	// Read a text file and save into a List
	private List<String> readFile( String file ) throws Exception {
		String line = "";
		ArrayList<String> list = new ArrayList<String>();
		BufferedReader br = new BufferedReader( new FileReader( file ) );
		while ( (line = br.readLine() ) != null ) {
		    list.add( line );
		}
		if (br != null) {
			br.close();
		}		
		return list;
	}
	
	/**
	 * Check if a directory is empty
	 */
	private boolean isDirEmpty( Path directory ) throws IOException {
		boolean result = false;
	    try( DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory) ) {
	        result = !dirStream.iterator().hasNext();
	        dirStream.close();
	    }
	    return result;
	}
	
	private void saveXmlData( Activation act ) throws Exception {
		FileWriter writer = new FileWriter( act.getNamespace() + "/" + "sagi_source_data.xml"); 
		String xml = act.getXmlOriginalData();
		xml = xml.replaceAll("><", ">\n<");
		writer.write( xml );
		writer.close();		
		debug("XML source data file saved");
	}
	
	
	public void process( String hexResp ) throws Exception {
		String instanceSerial = "";
		try {
			
			byte[] compressedResp = ZipUtil.toByteArray( hexResp );
			String response = ZipUtil.decompress(compressedResp);

			List<Activation> acts = parser.parseActivations( response );
			executionQueue.addAll( acts );
			jobPool.addAll( acts );
			debug("starting instance with " + acts.size() + " activities.");
			boolean found = false;
			for ( Activation act : acts ) {
				if( act.getOrder() == 0 ) {
					currentActivation = act;
					found = true;
					debug("execute first task in instance " + act.getInstanceSerial() );
					instanceSerial = act.getInstanceSerial();
					executionQueue.remove(act);

					String wrappersFolder = configurator.getSystemProperties().getTeapotRootFolder() + "wrappers/";
					act.setWrappersFolder(wrappersFolder);
					
					String newCommand = generateCommand( act );
					act.setCommand( newCommand );
					saveInputData( act );
					saveXmlData( act );
					runTask( act );
					break;
				}
			}
			if ( !found ) {
				error("no activities found in instance ");
			}
		} catch (Exception e) {
			e.printStackTrace();
			
			error( "error starting process: " + e.getMessage() );
			comm.send("activityManagerReceiver", "instanceId=" + instanceSerial + "&response=CANNOT_EXEC&node=" + tm.getMacAddress() );
		}
	}

	
}
