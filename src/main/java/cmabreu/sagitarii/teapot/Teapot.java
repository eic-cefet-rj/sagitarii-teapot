package cmabreu.sagitarii.teapot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import cmabreu.taskmanager.core.ITask;
import cmabreu.taskmanager.core.ITaskObserver;
import cmabreu.taskmanager.core.TaskManager;

/**
 * Copyright 2015 Carlos Magno Abreu
 * magno.mabreu@gmail.com 
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please ee the following page for the LGPL license:
 * http://www.gnu.org/licenses/lgpl.txt
 * 
 */

public class Teapot implements ITaskObserver {
	private TaskManager tm;
	private Communicator comm;
	private Configurator gf;
	private XMLParser parser;
	private List<Activation> executionQueue;
	private boolean restarting = false;
	private boolean reloading = false;
	private boolean quiting = false;
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 
	
	public List<Activation> getExecutionQueue() {
		return new ArrayList<Activation>( executionQueue );
	}
	
	public Teapot(TaskManager tm, Communicator comm, Configurator gf) {
		tm.setObserver(this);
		this.tm = tm;
		this.comm = comm;
		this.gf = gf;
		this.parser = new XMLParser();
		this.executionQueue = new ArrayList<Activation>();
		logger.debug("cleaning workspace...");
		cleanUp();
		logger.debug("done.");
	}
	

	private void sanitize( ITask task ) {
		
		if ( ( task.getExitCode() == 0 ) && gf.getClearDataAfterFinish() ) {
			try {
				FileUtils.deleteDirectory( new File( task.getActivation().getNamespace() ) ); 
			} catch ( IOException e ) {
				sendErrorLog( e.getMessage() );
			}
		}
	}

	/**
	 * Remover o diretório raiz do namespace
	 * Chamado antes de iniciar os trabalhos para sempre ter um namespace limpo.
	 */
	private void cleanUp() {
		try {
			FileUtils.deleteDirectory( new File( "namespaces" ) ); 
		} catch ( IOException e ) {
			sendErrorLog( e.getMessage() );
		}
	}

	/**
	 * Check if sagi_output have some data
	 * 
	 * @param sagiOutput the complete path to sagi_output.txt
	 * @return true if the file have some data
	 */
	private void validateProduct( String taskFolder ) {
		String sagiOutput = taskFolder + File.separator + "sagi_output.txt";
		String outbox = taskFolder + File.separator + "outbox";

		logger.debug("check output folder " + outbox );
		
		try {
			File file = new File(sagiOutput);
			if( !file.exists() ) { 
				sendErrorLog("output CSV data file sagi_output.txt not found");
				return;
			} 	
			if ( file.length() == 0 ) { 
				sendErrorLog("output CSV data file sagi_output.txt is empty");
				return;
			}		
			BufferedReader br = new BufferedReader( new FileReader( file ) );
			String header = br.readLine(); 					
			if ( header == null ) { 
				sendErrorLog("output CSV data file sagi_output.txt have no header line");
				br.close();
				return;
			}
			String line = br.readLine(); 					
			if ( line == null ) { 
				sendErrorLog("output CSV data file sagi_output.txt have no data line");
				br.close();
				return;
			}
			br.close();
		} catch ( Exception e ) {
			sendErrorLog("validateProduct: " + e.getMessage() );
			return;
		}
		
		File outboxDir = new File( outbox );
		if( outboxDir.list().length == 0 ){
			logger.warn("no files found in outbox");
		}
		outboxDir = null;
	}
	
	
	public void sendErrorLog( String errorLog ) {
		logger.error( errorLog );
		String parameters = "macAddress=" + tm.getMacAddress() + "&errorLog=" + errorLog;
		comm.send("receiveErrorLog", parameters);
	}
	
	/**
	* Implementacao de ITaskObserver.notify()
	* Recebe uma noficacao quando uma tarefa é concluída.
	* Este método é propagado desde ITask (quando termina a thread) e passa pelo TaskManager.notify()
	* que dispara ITaskObserver.notify() caso tenha sido chamado ITaskObserver.setObserver com um 
	* observer válido (neste caso, o observer é esta classe).
	*/
	@Override
	public synchronized void notify( ITask task ) {
		logger.debug("task " + task.getTaskId() + "("+ task.getActivation().getExecutor() + ") finished. (" + task.getExitCode() + ")" );
		try {
			Activation act = task.getActivation();
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
			sendErrorLog("error finishing task " + task.getApplicationName() + " at " + task.getActivation().getNamespace() + " : " + e.getMessage() );
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
	
	
	/**
	 * Procura a proxima ativacao da fila de pipelines recebidos que tenha numero de ordem 
	 * imediatamente superior à informada e que esteja na mesma instância.
	 * Caso não encontre, a instância foi toda executada. Não faz nada.
	 * 
	 * @param task Última tarefa executada
	 */
	private synchronized void executeNext( ITask task ) {
		logger.debug("searching for pipelined tasks for task " + task.getActivation().getExecutor() + " (index " + task.getActivation().getOrder() + ") fragment " + task.getActivation().getFragment() 
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
						e.printStackTrace();
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
	 * Executa uma tarefa e avisa ao Sagitarii.
	 * @param activation
	 */
	private void runTask( Activation activation ) {
		tm.startTask( activation, activation.getCommand() );
		try {
			comm.send("activityManagerReceiver", "pipelineId=" + activation.getPipelineSerial() + "&response=RUNNING&node=" + tm.getMacAddress() + "&executor" + activation.getExecutor() );
		} catch ( Exception e ) {
			sendErrorLog("Sagitarii not received task RUNNING response. Maybe offline.");
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
		File outputFolder = new File( act.getNamespace() + File.separator + "outbox" );
		outputFolder.mkdirs();

		File inputFolder = new File( act.getNamespace() + File.separator + "inbox" );
		inputFolder.mkdirs();
	}
	
	/** 
	 * Salva os dados iniciais em um diretório para trabalho.
	 * O diretório criado é referente ao código do pipeline, o serial da atividade
	 * e o serial da tarefa.
	 * 
	 * @param act 
	 * @throws Exception
	 */
	private void saveInputData( Activation act ) throws Exception {
		logger.debug("start data preparation for task " + act.getExecutor() + " (Activity: " + act.getActivitySerial() + "/ Task: " + act.getTaskId() + ")" );
		if ( act.getSourceData().size() < 2 ) {
			// We need at least 2 lines ( one line for header and one line of data )
			sendErrorLog( "Not enough input data. Aborting..." );
			throw new Exception ("Not enough data in input CSV for Task " + act.getActivitySerial() );
		}
		createWorkFolder(act);
		
		Activation previous = act.getPreviousActivation(); 
		if ( previous != null ) {
			// So this is not the first task inside instance. 
			String previousOutbox = previous.getNamespace() + File.separator + "outbox";
			String destInbox = act.getNamespace() + File.separator + "inbox";

			// Copy sagi_output.txt from previous task to this task source data.
			act.setSourceData( readFile( previous.getNamespace() + File.separator + "sagi_output.txt" ) );

			// Save previous output as this input
			FileWriter writer = new FileWriter( act.getNamespace() + File.separator + "sagi_input.txt"); 
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
			FileWriter writer = new FileWriter( act.getNamespace() + File.separator + "sagi_input.txt"); 
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
					String url = gf.getHostURL() + "/getFile?idFile="+ file.getId() + "&experiment=" + act.getExperiment();
					String target = act.getNamespace() + File.separator + "inbox" + File.separator + file.getName();
					dl.download(url, target, true);
				}
			} else {
				logger.debug("no need to download files.");
			}
	
		}
		
		logger.debug("all data were prepared for task " + act.getExecutor() + " (" + act.getActivitySerial() + "/" + act.getTaskId() + ")" );
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
		FileWriter writer = new FileWriter( act.getNamespace() + File.separator + "sagi_source_data.xml"); 
		String xml = act.getXmlOriginalData();
		xml = xml.replaceAll("><", ">\n<");
		writer.write( xml );
		writer.close();		
	}
	
	/**
	 * Will restart Teapot
	 * It is a Sagitarii command
	 */
	public void restartApplication() {
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
	
	private void restart() {
		restarting = true;
		if ( tm.getRunningTaskCount() > 0 ) {
			logger.debug("cannot restart now. " + tm.getRunningTaskCount() + " tasks still runnig");
		} else {
			logger.debug("restart now.");
			restartApplication();
		}
	}
	
	/**
	 * Baixa novamente os wrappers
	 */
	private void reloadWrappers() {
		reloading = true;
		if ( tm.getRunningTaskCount() > 0 ) {
			logger.debug("cannot reload wrappers now. " + tm.getRunningTaskCount() + " tasks still runnig");
		} else {
			logger.debug("reload all wrappers now.");
			try {
				RepositoryManager rm = new RepositoryManager();
				rm.downloadWrappers( gf.getHostURL(), tm.getOsType() );
				logger.debug("all wrappers reloaded.");
			} catch ( Exception e ) {
				sendErrorLog("cannot reload wrappers: " + e.getMessage() );
			}
			reloading = false;
		}
	}
	

	private void quit() {
		quiting = true;
		if ( tm.getRunningTaskCount() > 0 ) {
			logger.debug("cannot quit now. " + tm.getRunningTaskCount() + " tasks still runnig");
		} else {
			logger.debug("quit now.");
			 System.exit(0);
		}
	}
	
	/**
	 * É chamado de tempos em tempos para enviar os dados da máquina ao Sagitarii.
	 * Ao fazer isso, o Sagitarii poderá enviar uma nova tarefa.
	 */
	public synchronized void process( String resposta ) throws Exception {
		
		if ( quiting ) {
			// If we're here, is becaus the first call not finished (have tasks still running),
			// so we'll try again
			quit();
			return;
		}
		if ( restarting ) {
			// If we're here, is becaus the first call not finished (have tasks still running), 
			// so we'll try again
			restart();
			return;
		}
		if ( reloading ) {
			// If we're here, is becaus the first call not finished (have tasks still running), 
			// so we'll try again
			reloadWrappers();
			return;
		}
		
		if ( ( !resposta.equals( "NO_ANSWER" ) ) && ( !resposta.equals( "COMM_ERROR" ) ) && ( !resposta.equals( "" ) ) ) {
			if ( resposta.equals( "COMM_RESTART" ) ) {
				logger.debug("get restart command from Sagitarii");
				restart();
			} else
			if ( resposta.equals( "RELOAD_WRAPPERS" ) ) {
				logger.debug("get reload wrappers command from Sagitarii");
				reloadWrappers();
			} else
			if ( resposta.equals( "COMM_QUIT" ) ) {
				logger.debug("get quit command from Sagitarii");
				quit();
			} else
			if ( resposta.equals( "COMM_CLEAN_WORKSPACE" ) ) {
				logger.debug("get clean workspace command from Sagitarii");
				if ( tm.getRunningTaskCount() > 0 ) {
					logger.debug("will not clean workspace. " + tm.getRunningTaskCount() + " tasks still runnig");
				} else {
					cleanUp();
					logger.debug("workspace cleaned");
				}
			} else {
				// Pipeline XML received from Sagitarii. Its time to work!
				
				String pipelineSerial = "";
				try {
					
					List<Activation> acts = parser.parseActivations( resposta );
					executionQueue.addAll( acts );

					for ( Activation act : acts ) {
						if( act.getOrder() == 0 ) {
							
							logger.debug("execute first activation in pipeline " + act.getPipelineSerial() );
							pipelineSerial = act.getPipelineSerial();
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
					comm.send("activityManagerReceiver", "pipelineId=" + pipelineSerial + "&response=CANNOT_EXEC&node=" + tm.getMacAddress() ); 
					sendErrorLog( e.getMessage() );
				}
			}
			
		}
		
	}

	
}
