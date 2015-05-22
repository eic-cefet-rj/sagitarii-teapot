package cmabreu.sagitarii.teapot.comm;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import cmabreu.sagitarii.teapot.Configurator;
import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;
import cmabreu.sagitarii.teapot.SystemProperties;

public class Communicator  {
	private WebClient webClient;
    private String soName;
    private String localIpAddress;
    private String machineName;
    private String macAddress;	
    private int availableProcessors;
    private String java;
    private String soFamily;
	private String maxAllowedTasks;
	private Logger logger = LogManager.getLogger( this.getClass().getName() );

	
	public Communicator( Configurator gf, SystemProperties tm ) throws Exception {
		
		webClient = new WebClient(gf);
		try {
			this.soName = URLEncoder.encode(tm.getSoName(), "UTF-8");
			this.localIpAddress =  URLEncoder.encode(tm.getLocalIpAddress(), "UTF-8");
			this.machineName = URLEncoder.encode(tm.getMachineName(), "UTF-8");
			this.macAddress = URLEncoder.encode(tm.getMacAddress(), "UTF-8");
			this.availableProcessors = tm.getAvailableProcessors();
			this.java = URLEncoder.encode(tm.getJavaVersion(), "UTF-8");
			this.soFamily = URLEncoder.encode(tm.getOsType().toString(), "UTF-8");
			this.maxAllowedTasks = URLEncoder.encode( String.valueOf( gf.getActivationsMaxLimit() ) , "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw e;
		}
	}


	/**
	* Send a GET request to Sagitarii
	* 
	* Exemple : targetAction = "myStrutsAction", parameters = "name=foo&sobrenome=Bar"
	*
	* @param  targetAction  a Struts2 action
	* @param  parameters the GET request URL 
	* @return Sagitarii response
	*/
	public String send( String targetAction, String parameters ) {
		String resposta = "COMM_ERROR";
		try { 
			resposta = webClient.doGet(targetAction, parameters);
		} catch ( Exception e ) {
			logger.error("Communication error: " + e.getMessage() );
		} 
		return resposta;
	}
	
	/**
	* Announce this node and request for more tasks do process
	* Sagitarii can send a special command instead (quit, restart, reload wrappers, etc...)
	* 
	* @param  cpuLoad (in %)
	* @return Sagitarii response
	*/
	public synchronized String announceAndRequestTask( Double cpuLoad, Long freeMemory, Long totalMemory ) {
		String parameters = "soName=" + soName + "&localIpAddress=" + localIpAddress + 
				"&machineName=" + machineName + "&macAddress=" + macAddress + "&cpuLoad=" + cpuLoad +
				"&availableProcessors=" + availableProcessors + "&soFamily=" + soFamily +
				"&javaVersion=" + java + "&maxAllowedTasks=" + maxAllowedTasks + "&freeMemory=" + freeMemory +
				"&totalMemory=" + totalMemory;
		
		return send( "announce", parameters);
	}
	

	
}
