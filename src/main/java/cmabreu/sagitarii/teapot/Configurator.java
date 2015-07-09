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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cmabreu.sagitarii.teapot.comm.ProxyInfo;

public class Configurator {

	private String hostURL;
	private int poolIntervalMilliSeconds;
	private int activationsMaxLimit;
	private ProxyInfo proxyInfo;
	private int useProxy;
	private Document doc;
	private boolean showConsole;
	private boolean clearDataAfterFinish;
	private char CSVDelimiter; 
	private int fileSenderDelay;
	private String storageHost;
	private int storagePort;
	private boolean useSpeedEqualizer;
	private boolean enforceTaskLimitToCores;
	private SystemProperties systemProperties;
	private Logger logger = LogManager.getLogger( this.getClass().getName()  );

	public SystemProperties getSystemProperties() {
		return this.systemProperties;
	}
	
	public char getCSVDelimiter() {
		return CSVDelimiter;
	}

	public void setPoolIntervalMilliSeconds( int millis ) {
		poolIntervalMilliSeconds = millis;
	}
	
	public boolean enforceTaskLimitToCores() {
		return this.enforceTaskLimitToCores;
	}
	
	public int getFileSenderDelay() {
		return fileSenderDelay;
	}
	
	public String getStorageHost() {
		return storageHost;
	}
	
	public int getStoragePort() {
		return storagePort;
	}
	
	public boolean getClearDataAfterFinish() {
		return clearDataAfterFinish;
	}
	
	public void setClearDataAfterFinish(boolean clearDataAfterFinish) {
		this.clearDataAfterFinish = clearDataAfterFinish;
	}
	
	public boolean useSpeedEqualizer() {
		return useSpeedEqualizer;
	}
	
	public boolean getShowConsole() {
		return this.showConsole;
	}
	
	public ProxyInfo getProxyInfo() {
		return proxyInfo;
	}

	public boolean useProxy() {
		return this.useProxy == 1;
	}
	
	public String getHostURL() {
		return hostURL;
	}

	public int getPoolIntervalMilliSeconds() {
		if ( poolIntervalMilliSeconds < 200 ) {
			poolIntervalMilliSeconds = 200;
		}
		return poolIntervalMilliSeconds;
	}

	public int getActivationsMaxLimit() {
		return activationsMaxLimit;
	}

	private String getTagValue(String sTag, Element eElement) throws Exception{
		try {
			NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
	        Node nValue = (Node) nlList.item(0);
			return nValue.getNodeValue();
		} catch ( Exception e ) {
			logger.error("Element " + sTag + " not found in configuration file.");
			throw e;
		}
	 }
	
	public String getValue(String container, String tagName) {
		String tagValue = "";
		try {
			NodeList postgis = doc.getElementsByTagName(container);
			Node pgconfig = postgis.item(0);
			Element pgElement = (Element) pgconfig;
			tagValue = getTagValue(tagName, pgElement) ; 
		} catch ( Exception e ) {
		}
		return tagValue;
	}

	
	public Configurator(String file) throws Exception {
		logger.debug("loading XML data from " + file);
		
		File fil = new File( file );
		if ( !fil.exists() ) {
			System.out.println("xml config file not found at folder");
			System.out.println( file );
			System.exit(0);
		}
		
		try {
			InputStream is = new FileInputStream( file ); 
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("XML file " + file + " not found.");
		}	
		systemProperties = new SystemProperties();
	}
	
	
	public List<Wrapper> getRepositoryList() {
		List<Wrapper> resp = new ArrayList<Wrapper>();
		NodeList mapconfig = doc.getElementsByTagName("wrapper");
		for ( int x = 0; x < mapconfig.getLength(); x++ ) {
			try {
				Node mpconfig = mapconfig.item(x);
				Element mpElement = (Element) mpconfig;
				String version = mpElement.getAttribute("version");
				String type = mpElement.getAttribute("type");
				String wrapperName = mpElement.getAttribute("activity");
				String hash = mpElement.getAttribute("hash");
				String target = mpElement.getAttribute("target");
				
				Wrapper acc = new Wrapper();
				acc.fileName = wrapperName;
				acc.type = type;
				acc.version = version;
				acc.target = target;
				acc.hash = hash;
				resp.add(acc);
			} catch (Exception e){
				System.out.println( e.getMessage() );
			}
		}
		
		return resp;
	}
	
	public void loadMainConfig()  {
			
			NodeList mapconfig = doc.getElementsByTagName("cluster");
			Node mpconfig = mapconfig.item(0);
			Element mpElement = (Element) mpconfig;
			try {
				hostURL = getTagValue("hostURL", mpElement);
				CSVDelimiter = getTagValue("CSVDelimiter", mpElement).charAt(0);
				poolIntervalMilliSeconds = Integer.valueOf( getTagValue("poolIntervalMilliSeconds", mpElement) );
				activationsMaxLimit = Integer.valueOf( getTagValue("activationsMaxLimit", mpElement) );
				storageHost = getTagValue("storageHost", mpElement);
				storagePort = Integer.valueOf( getTagValue("storagePort", mpElement) );
				fileSenderDelay = Integer.valueOf( getTagValue("fileSenderDelay", mpElement) );
				showConsole = Boolean.parseBoolean( getTagValue("activationShowConsole", mpElement) );
				clearDataAfterFinish = Boolean.parseBoolean( getTagValue("clearDataAfterFinish", mpElement) );
				useSpeedEqualizer = Boolean.parseBoolean( getTagValue("useSpeedEqualizer", mpElement) );
				enforceTaskLimitToCores = Boolean.parseBoolean( getTagValue("enforceTaskLimitToCores", mpElement) );
				
				if ( enforceTaskLimitToCores ) {
					useSpeedEqualizer = false;
					poolIntervalMilliSeconds = systemProperties.getAvailableProcessors();
				}
				
				useProxy = Integer.parseInt( getValue("proxy", "useProxy") );
				
				if (useProxy == 1) {
					proxyInfo = new ProxyInfo();
					proxyInfo.setHost( getValue("proxy", "proxy-host") );
					proxyInfo.setPort( Integer.parseInt(getValue("proxy", "proxy-port"))  );
					proxyInfo.setPassword( getValue("proxy", "proxy-password") );
					proxyInfo.setUser( getValue("proxy", "proxy-user") );
				} 			
			} catch ( Exception e ) {
				System.out.println( e.getMessage() );
			}
			
			
	}
	
	
}
