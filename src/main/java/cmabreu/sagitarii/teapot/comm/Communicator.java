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
	* Envia uma string contendo uma série de parametros e valores ao servidor
	* e recebe uma string de resposta.
	* 
	* Exemplo : targetAction = "pegaDados", parameters = "nome=foo&sobrenome=Bar"
	*
	* @param  targetAction  uma action Struts2 existente no servidor.
	* @param  parameters um conjunto de nomes e valores no formato URL GET 
	* @return uma string contendo a resposta do servidor
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
	* Envia os dados de configuração da máquina onde o Teapot está sendo executado.
	* Na resposta, o servidor envia uma tarefa, caso haja alguma.
	* 
	* @param  cpuLoad  a carga de CPU atual da máquina (em %)
	* @return uma string contendo a resposta do servidor.
	*/
	public synchronized String anuncia( Double cpuLoad ) {
		String parameters = "soName=" + soName + "&localIpAddress=" + localIpAddress + 
				"&machineName=" + machineName + "&macAddress=" + macAddress + "&cpuLoad=" + cpuLoad +
				"&availableProcessors=" + availableProcessors + "&soFamily=" + soFamily +
				"&javaVersion=" + java + "&maxAllowedTasks=" + maxAllowedTasks;
		
		return send( "announce", parameters);
	}
	

	
}
