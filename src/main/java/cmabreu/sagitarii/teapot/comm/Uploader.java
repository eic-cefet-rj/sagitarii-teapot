package cmabreu.sagitarii.teapot.comm;

import java.io.IOException;

import cmabreu.sagitarii.teapot.Configurator;
import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;
import cmabreu.sagitarii.teapot.SystemProperties;
import cmabreu.sagitarii.teapot.Task;

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
			String filesFolderName, Task task, SystemProperties tm) throws Exception {
		
		String macAddress = tm.getMacAddress();
		logger.debug( "uploading " + fileName + " to " + relationName + " for experiment " + experimentSerial );
		
		Client client = new Client( gf );
		client.sendFile( fileName, filesFolderName,	relationName, experimentSerial, macAddress, task );
		logger.debug( "done uploading " + fileName);
	}

}