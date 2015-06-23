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
	private Configurator gf;
	private XMLParser parser;
	private List<Activation> executionQueue;
	private List<Activation> jobPool;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	private List<Task> tasks = new ArrayList<Task>();
	private Task currentTask = null;
	
	public Task getCurrentTask() {
		return currentTask;
	}
	
	public List<Activation> getJobPool() {
		return new ArrayList<Activation>( jobPool );
	}
	
	public List<Task> getTasks() {
		return new ArrayList<Task>( tasks );
	}
	
	public Teapot(SystemProperties tm, Communicator comm, Configurator gf) {
		this.tm = tm;
		this.comm = comm;
		this.gf = gf;
		this.parser = new XMLParser();
		this.executionQueue = new ArrayList<Activation>();
		this.jobPool = new ArrayList<Activation>();
	}
	

	private void sanitize( Task task ) {
		if ( gf.getClearDataAfterFinish() ) {
			try {
				FileUtils.deleteDirectory( new File( task.getActivation().getNamespace() ) ); 
			} catch ( IOException e ) {
				notifySagitarii( e.getMessage() );
			}
		}
	}

	/**
	 * Check if sagi_output have some data
	 * 
	 * @param sagiOutput the complete path to sagi_output.txt
	 * @return true if the file have some data
	 */
	private void validateProduct( String taskFolder ) {
		String sagiOutput = taskFolder + "/" + "sagi_output.txt";
		String outbox = taskFolder + "/" + "outbox";

		logger.debug("check output folder " + outbox );
		
		try {
			File file = new File(sagiOutput);
			if( !file.exists() ) { 
				notifySagitarii("output CSV data file sagi_output.txt not found");
				return;
			} 	
			if ( file.length() == 0 ) { 
				notifySagitarii("output CSV data file sagi_output.txt is empty");
				return;
			}		
			BufferedReader br = new BufferedReader( new FileReader( file ) );
			String header = br.readLine(); 					
			if ( header == null ) { 
				notifySagitarii("output CSV data file sagi_output.txt have no header line");
				br.close();
				return;
			}
			String line = br.readLine(); 					
			if ( line == null ) { 
				notifySagitarii("output CSV data file sagi_output.txt have no data line");
				br.close();
				return;
			}
			br.close();
		} catch ( Exception e ) {
			notifySagitarii("validateProduct: " + e.getMessage() );
			return;
		}
		
		File outboxDir = new File( outbox );
		if( outboxDir.list().length == 0 ){
			logger.warn("no files found in outbox");
		}
		outboxDir = null;
	}
	
	
	public void notifySagitarii( String message ) {
		logger.debug( message );
		try {
			String parameters = "macAddress=" + tm.getMacAddress() + "&errorLog=" + URLEncoder.encode( message, "UTF-8");
			comm.send("receiveErrorLog", parameters);
		} catch ( Exception e ) {
			logger.error("cannot notify Sagitarii: " + e.getMessage() );
		}
	}
	
	/**
	 * Formata o comando da ativacao seguinte usando o resultado CSV da ativacao anterior
	 * 
	 * @return o comando da ativacao após a substituicao das tags
	 */
	private String generateCommand( Activation activation ) {
		String command = "";
		if ( activation.getExecutorType().equals("RSCRIPT") ) {
			String rpath = gf.getrPath();
			command = "java -Djava.library.path="+rpath+" -jar r-wrapper.jar " + activation.getCommand() + " " + activation.getNamespace();
		} else {
			command = "java -jar " + activation.getCommand() + " " + activation.getNamespace();
		}
		return command;
	}
	
	

	private void executeNext( Task task ) {
		logger.debug("searching for instance tasks for task " + task.getActivation().getExecutor() + " (index " + task.getActivation().getOrder() + ") fragment " + task.getActivation().getFragment() 
				+ " exit code: " + task.getExitCode() + " buffer size: " + task.getSourceData().size());
		Activation previousActivation = task.getActivation();
		int nextOrder = previousActivation.getOrder() + 1;
		String fragmentId = previousActivation.getFragment();
		if ( (task.getExitCode() == 0) && ( task.getSourceData().size() > 1 ) ) {
			for ( Activation nextAct : executionQueue ) {
				logger.debug(" > checking task " + nextAct.getExecutor() + " order " + nextAct.getOrder() + " fragment " + nextAct.getFragment() );
				if( (nextAct.getOrder() == nextOrder) && ( nextAct.getFragment().equals(fragmentId) ) ) {
					logger.debug( " > accepted." );
					executionQueue.remove( nextAct );
					String newCommand = generateCommand( nextAct );
					nextAct.setCommand( newCommand );
					nextAct.setSourceData( task.getSourceData() );
					nextAct.setPreviousActivation( previousActivation );
					try {
						saveInputData( nextAct );
						runTask( nextAct );
					} catch ( Exception e ) {
						logger.error( e.getMessage() );
						comm.send("activityManagerReceiver", "instanceId=" + nextAct.getInstanceSerial() + "&response=CANNOT_EXEC&node=" + tm.getMacAddress() ); 
						notifySagitarii( e.getMessage() );
					}
					return;
				} else {
					logger.debug(" > not accepted.");
				}
			}
		} else {
			logger.debug("task " + task.getActivation().getExecutor() + " have empty buffer or error exit code.");
		}
	}
	
	
	/**
	 * Run a wrapper task
	 * BLOCKING
	 * 
	 * @param activation an activation
	 */
	private void runTask( Activation activation ) {
		String applicationName = activation.getCommand();
		String instanceId = activation.getInstanceSerial();
		int order = activation.getOrder();

		logger.debug("start task " + activation.getTaskId() + "(" + activation.getType() + ") " + activation.getActivitySerial() + " ("+ instanceId + "-" + order + "):");
		logger.debug( applicationName );
        
		activation.setStatus( TaskStatus.RUNNING );
		
		Task task = new Task( activation, applicationName );
		task.setSourceData( activation.getSourceData() );
		
		try {
			comm.send("activityManagerReceiver", "instanceId=" + activation.getInstanceSerial() + "&response=RUNNING&node=" + tm.getMacAddress() + "&executor" + activation.getExecutor() );
	        tasks.add(task);
	        currentTask = task;
	        
	        // Will Block Until Finished ...
	        task.run();
	        
	        // When finished...
	        notify( task );
	        
		} catch ( Exception e ) {
			notifySagitarii("Sagitarii not received task RUNNING response. Maybe offline.");
		}
	}
	
	/**
	* Implementacao de ITaskObserver.notify()
	* Recebe uma noficacao quando uma tarefa é concluída.
	* Este método é propagado desde ITask (quando termina a thread) e passa pelo TaskManager.notify()
	* que dispara ITaskObserver.notify() caso tenha sido chamado ITaskObserver.setObserver com um 
	* observer válido (neste caso, o observer é esta classe).
	*/
	public synchronized void notify( Task task ) {
		logger.debug("task " + task.getTaskId() + "("+ task.getActivation().getExecutor() + ") finished. (" + task.getExitCode() + ")" );
		try {
			Activation act = task.getActivation();
			act.setStatus( TaskStatus.FINISHED );
			
			// Check output file
			validateProduct( act.getNamespace() );

			// Send data and files
			new Uploader(gf).uploadCSV("sagi_output.txt", act.getTargetTable(), act.getExperiment(), 
					act.getNamespace() , task, tm );
			
			// Run next task in same instance (if exists)
			executeNext( task );
			// Clean up
			sanitize( task );
			
		} catch ( Exception e ) {
			notifySagitarii("error finishing task " + task.getApplicationName() + " at " + task.getActivation().getNamespace() + " : " + e.getMessage() );
		}
		
	}
	

	/**
	 * Cria a pasta para os dados de trabalho da tarefa e caixas de
	 * entrada e saída para os arquivos.
	 * 
	 * @param act
	 * @throws Exception
	 */
	private void createWorkFolder( Activation act ) throws Exception {
		File outputFolder = new File( act.getNamespace() + "/" + "outbox" );
		outputFolder.mkdirs();

		File inputFolder = new File( act.getNamespace() + "/" + "inbox" );
		inputFolder.mkdirs();
	}
	
	/** 
	 * Salva os dados iniciais em uma pasta para trabalho.
	 * 
	 * @param act 
	 * @throws Exception
	 */
	private void saveInputData( Activation act ) throws Exception {
		logger.debug("start data preparation for task " + act.getExecutor() + " (Activity: " + act.getActivitySerial() + "/ Task: " + act.getTaskId() + ")" );
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
			logger.debug( " > input data file sagi_input.txt saved with " + act.getSourceData().size() + " lines");
			
			// Copy files from previous task's output to this input box.
			File source = new File( previousOutbox );
			File dest = new File( destInbox );
			if ( !isDirEmpty( source.toPath() )  ) {
				logger.debug(" > will copy files from previous task " + previous.getTaskId() + "..." );
				logger.debug("   from > " + previousOutbox );
				logger.debug("   to   > " + destInbox );
				
				FileUtils.copyDirectory( source, dest );
			}
		} else {
			// This is the first task in instance
			FileWriter writer = new FileWriter( act.getNamespace() + "/" + "sagi_input.txt"); 
			for(String str: act.getSourceData() ) {
			  writer.write( str + "\n" );
			}
			writer.close();
			logger.debug( " > input data file sagi_input.txt saved with " + act.getSourceData().size() + " lines");
			
			// Check if Sagitarii ask us to download some files...
			if ( act.getFiles().size() > 0 ) {
				logger.debug(" > this task needs to download " + act.getFiles().size() + " files");
				Downloader dl = new Downloader();
				for ( FileUnity file : act.getFiles() ) {
					logger.debug(" > will need file " + file.getName() + " for attribute " + file.getAttribute() );
					String url = gf.getHostURL() + "/getFile?idFile="+ file.getId();
					String target = act.getNamespace() + "/" + "inbox" + "/" + file.getName();
					
					notifySagitarii("Downloading file " + file.getName() );
					
					dl.download(url, target, true);
				}
			} else {
				logger.debug("no need to download files.");
			}
	
		}
		
		notifySagitarii( "" );
		logger.debug("done preparing task " + act.getExecutor() + " (" + act.getActivitySerial() + "/" + act.getTaskId() + ")" );
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
		logger.debug("XML source data file saved");
	}
	
	
	/**
	 * Eh chamado de tempos em tempos para enviar os dados da maquina ao Sagitarii.
	 * Ao fazer isso, o Sagitarii poderah enviar uma nova tarefa.
	 */
	public void process( String response ) throws Exception {
		logger.debug("process");
		String instanceSerial = "";
		try {
			
			List<Activation> acts = parser.parseActivations( response );
			executionQueue.addAll( acts );
			jobPool.addAll( acts );

			for ( Activation act : acts ) {
				if( act.getOrder() == 0 ) {
					notifySagitarii("starting executor " + act.getExecutor() );
					logger.debug("execute first task in instance " + act.getInstanceSerial() );
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
			logger.error( e.getMessage() );
			comm.send("activityManagerReceiver", "instanceId=" + instanceSerial + "&response=CANNOT_EXEC&node=" + tm.getMacAddress() ); 
			notifySagitarii( e.getMessage() );
		}
	}

	
}
