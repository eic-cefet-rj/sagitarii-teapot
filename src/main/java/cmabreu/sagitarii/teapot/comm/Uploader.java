package cmabreu.sagitarii.teapot.comm;

import java.io.IOException;

import cmabreu.sagitarii.teapot.Configurator;
import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;
import cmabreu.taskmanager.core.ITask;
import cmabreu.taskmanager.core.TaskManager;

public class Uploader {
	private Configurator gf; 
	private Logger logger = LogManager.getLogger( this.getClass().getName() );

	public Uploader( Configurator gf  ) {
		this.gf = gf;
	}

	
	/**
	 * Envia um arquivo CSV ao servidor
	 * @param fileName nome e caminho do arquivo CSV
	 * @param relationName noma da tabela que irá receber o arquivo (já deverá existir)
	 * @param experimentSerial tag do experimento associado a estes dados (já deverá existir)
	 * 
	 * @throws IOException
	 */
	public void uploadCSV(String fileName, String relationName, String experimentSerial, 
			String filesFolderName, ITask task, TaskManager tm) throws Exception {
		
		String macAddress = tm.getMacAddress();
		
		logger.debug( "uploading " + fileName + " to " + relationName + " for experiment " + experimentSerial );
		
		String pipelineSerial = "";
		String activity = "";
		String fragment = "";
		
		if ( task != null ) {
			pipelineSerial = task.getActivation().getPipelineSerial();
			activity = task.getActivation().getActivitySerial();
			fragment = task.getActivation().getFragment();
		}		
		
		Client client = new Client(gf.getStorageHost(), gf.getStoragePort(), gf.getHostURL(), gf.getFileSenderDelay() );
		client.sendFile( fileName, filesFolderName,	relationName, experimentSerial, macAddress, 
				pipelineSerial, activity, fragment );

		logger.debug( "done uploading " + fileName);

	}

}