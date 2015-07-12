package cmabreu.sagitarii.teapot;

/**
 * Copyright 2015 Carlos Magno Abreu
 * magno.mabreu@gmail.com 
 *
 * Licensed under the Apache  License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required  by  applicable law or agreed to in  writing,  software
 * distributed   under the  License is  distributed  on  an  "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the  specific language  governing  permissions  and
 * limitations under the License.
 * 
 */

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
import java.util.List;

import org.apache.commons.io.FileUtils;

import cmabreu.sagitarii.teapot.comm.Communicator;
import cmabreu.sagitarii.teapot.comm.Downloader;
import cmabreu.sagitarii.teapot.comm.FileUnity;
import cmabreu.sagitarii.teapot.comm.Uploader;

public class Teapot {
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
		}
	}
	
	private void error( String s ) {
		logger.error( s );
		execLog.add( "[ERROR] " +  s );
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
	
	public Teapot(Communicator comm, Configurator gf) {
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
				FileUtils.deleteDirectory( new File( currentActivation.getNamespace() ) ); 
			} catch ( IOException e ) {
				notifySagitarii( e.getMessage() );
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
				notifySagitarii( executor + ": output CSV data file sagi_output.txt not found");
				return false;
			} 
			if ( file.length() == 0 ) { 
				notifySagitarii( executor + ": output CSV data file sagi_output.txt is empty");
				return false;
			} else {
				debug( executor + ": sagi_output.txt have " + file.length() + " lines.");
			}
			BufferedReader br = new BufferedReader( new FileReader( file ) );
			String header = br.readLine(); 					
			if ( header == null ) { 
				notifySagitarii( executor + ": output CSV data file sagi_output.txt have no header line");
				br.close();
				return false;
			} 
			String line = br.readLine(); 					
			if ( line == null ) { 
				notifySagitarii( executor + ": output CSV data file sagi_output.txt have no data line");
				br.close();
				return false;
			} 
			br.close();
		} catch ( Exception e ) {
			notifySagitarii( executor + ": " + e.getMessage() );
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
		debug( message );
		try {
			String parameters = "macAddress=" + tm.getMacAddress() + "&errorLog=" + URLEncoder.encode( message, "UTF-8");
			comm.send("receiveErrorLog", parameters);
		} catch ( Exception e ) {
			error("cannot notify Sagitarii: " + e.getMessage() );
		}
	}
	
	/**
	 * Formata o comando da ativacao seguinte usando o resultado CSV da ativacao anterior
	 * 
	 * @return o comando da ativacao apos a substituicao das tags
	 */
	private String generateCommand( Activation activation ) {
		String wrapperFolder = configurator.getSystemProperties().getTeapotRootFolder() + "wrappers/";
		String command = "";
		String classPathParam = "-Djava.library.path=" + configurator.getSystemProperties().getJriPath();

		if ( activation.getExecutorType().equals("RSCRIPT") ) {
			String wrapperCommand = wrapperFolder + "r-wrapper.jar";
			String scriptFile = wrapperFolder + activation.getCommand();
			String workFolder = activation.getNamespace();
			
			command = "java "+classPathParam+" -jar "+ wrapperCommand + " " + scriptFile + " " + workFolder + " " + wrapperFolder;
			
		} else if ( activation.getExecutorType().equals("BASH") ) {
			command = wrapperFolder + activation.getCommand();
		} else {
			command = "java "+classPathParam+" -jar " + wrapperFolder + activation.getCommand() + " " + activation.getNamespace();
		}
		return command;
	}
	
	private void executeNext( Task task ) {
		debug("searching for instance tasks for task " + currentActivation.getExecutor() + " (index " + currentActivation.getOrder() + ") fragment " + currentActivation.getFragment() 
				+ " exit code: " + task.getExitCode() + " buffer size: " + task.getSourceData().size());
		Activation previousActivation = currentActivation;
		int nextOrder = previousActivation.getOrder() + 1;
		String fragmentId = previousActivation.getFragment();
		if ( (task.getExitCode() == 0) && ( task.getSourceData().size() > 1 ) ) {
			for ( Activation nextAct : executionQueue ) {
				debug(" > checking task " + nextAct.getExecutor() + " order " + nextAct.getOrder() + " fragment " + nextAct.getFragment() );
				if( (nextAct.getOrder() == nextOrder) && ( nextAct.getFragment().equals(fragmentId) ) ) {
					debug( " > accepted." );
					executionQueue.remove( nextAct );
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
						notifySagitarii( e.getMessage() );
					}
					return;
				} else {
					debug(" > not accepted.");
				}
			}
		} else {
			debug("task " + currentActivation.getExecutor() + " have empty buffer or error exit code.");
		}
	}
	
	
	/**
	 * Run a wrapper task
	 * BLOCKING
	 */
	private void runTask( Activation activation ) {
		String instanceId = activation.getInstanceSerial();
		int order = activation.getOrder();

		debug("start task " + activation.getTaskId() + "(" + activation.getType() + ") " + activation.getExecutor() + " ("+ instanceId + " :: " + order + ")");
        
		activation.setStatus( TaskStatus.RUNNING );
		
		Task task = new Task( activation, execLog );
		task.setSourceData( activation.getSourceData() );
		
		try {
			comm.send("activityManagerReceiver", "instanceId=" + activation.getInstanceSerial() + "&response=RUNNING&node=" + tm.getMacAddress() + "&executor" + activation.getExecutor() );
	        tasks.add(task);
	        currentTask = task;
	        
	        // Will Block Until Finished ...
	        task.run( configurator );
	        
	        // When finished...
	        notify( task );
	        
		} catch ( Exception e ) {
			notifySagitarii("Sagitarii not received task RUNNING response. Maybe offline.");
		}
	}
	
	/**
	* Implementation of ITaskObserver.notify()
	*/
	public synchronized void notify( Task task ) {
		debug("task " + task.getTaskId() + "("+ currentActivation.getExecutor() + ") finished. (" + task.getExitCode() + ")" );
		try {
			
			// TODO: Activation act = task.getActivation();  // Check if it is equals to currentActivation 
			
			Activation act = currentActivation;
			act.setStatus( TaskStatus.FINISHED );
			
			
			// Check output file
			if ( !validateProduct( act ) ) {
				error(currentActivation.getExecutor() + ": NO OUTPUT CSV DATA FOUND");
			}
				
			// Send data and files // Do not be tempted to simplify act parameter. It must be this way
			// because upload command line passes this parameters too
			new Uploader(configurator).uploadCSV("sagi_output.txt", act.getTargetTable(), act.getExperiment(), 
					act.getNamespace() , task, tm );
			
			// Run next task in same instance (if exists)
			executeNext( task );

			// Clean up
			sanitize( task );
			
		} catch ( Exception e ) {
			notifySagitarii("error finishing task " + task.getApplicationName() + " at " + currentActivation.getNamespace() + " : " + e.getMessage() );
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
	
	/** 
	 * Salva os dados iniciais em uma pasta para trabalho.
	 */
	private void saveInputData( Activation act ) throws Exception {
		debug("start data preparation for task " + act.getExecutor() + " (Activity: " + act.getActivitySerial() + "/ Task: " + act.getTaskId() + ")" );
		if ( act.getSourceData().size() < 2 ) {
			// We need at least 2 lines ( one line for header and one line of data )
			notifySagitarii( "Not enough input data. Aborting..." );
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
					String url = configurator.getHostURL() + "/getFile?idFile="+ file.getId()+"&macAddress=" + configurator.getSystemProperties().getMacAddress();
					String target = act.getNamespace() + "/" + "inbox" + "/" + file.getName();
					
					notifySagitarii("downloading " + file.getName() );
					dl.download(url, target, true);
				}
				notifySagitarii("");
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
	
	
	/**
	 * Eh chamado de tempos em tempos para enviar os dados da maquina ao Sagitarii.
	 * Ao fazer isso, o Sagitarii poderah enviar uma nova tarefa.
	 */
	public void process( String response ) throws Exception {
		String instanceSerial = "";
		try {
			
			List<Activation> acts = parser.parseActivations( response );
			executionQueue.addAll( acts );
			jobPool.addAll( acts );

			for ( Activation act : acts ) {
				if( act.getOrder() == 0 ) {
					currentActivation = act;
					notifySagitarii("starting executor " + act.getExecutor() );
					debug("execute first task in instance " + act.getInstanceSerial() );
					instanceSerial = act.getInstanceSerial();
					executionQueue.remove(act);
					String newCommand = generateCommand( act );
					act.setCommand( newCommand );
					saveInputData( act );
					saveXmlData( act );
					runTask( act );
					break;
				}
			}
			
		} catch (Exception e) {
			error( e.getMessage() );
			comm.send("activityManagerReceiver", "instanceId=" + instanceSerial + "&response=CANNOT_EXEC&node=" + tm.getMacAddress() ); 
			notifySagitarii( e.getMessage() );
		}
	}

	
}
