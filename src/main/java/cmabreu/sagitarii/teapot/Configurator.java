package cmabreu.sagitarii.teapot;

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
	private String rPath;
	private boolean clearDataAfterFinish;
	private char CSVDelimiter; 
	private int fileSenderDelay;
	private String storageHost;
	private int storagePort;
	
	private Logger logger = LogManager.getLogger( this.getClass().getName()  );

	public char getCSVDelimiter() {
		return CSVDelimiter;
	}

	public void setPoolIntervalMilliSeconds( int millis ) {
		poolIntervalMilliSeconds = millis;
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
	
	public String getrPath() {
		return rPath;
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
		if ( poolIntervalMilliSeconds < 250 ) {
			poolIntervalMilliSeconds = 250;
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
		try {
			InputStream is = this.getClass().getClassLoader().getResourceAsStream(file);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();
		  } catch (Exception e) {
				logger.error("XML file " + file + " not found.");
		  }			
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
				String wrapperName = getTagValue("activityFile", mpElement);
				String reload = getTagValue("reload", mpElement);
				String target = mpElement.getAttribute("target");
				
				Wrapper acc = new Wrapper();
				acc.fileName = wrapperName;
				acc.type = type;
				acc.version = version;
				acc.target = target;
				acc.reload = reload.equals("true");
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
				rPath = getTagValue("rPath", mpElement);
				CSVDelimiter = getTagValue("CSVDelimiter", mpElement).charAt(0);
				poolIntervalMilliSeconds = Integer.valueOf( getTagValue("poolIntervalMilliSeconds", mpElement) );
				activationsMaxLimit = Integer.valueOf( getTagValue("activationsMaxLimit", mpElement) );
				storageHost = getTagValue("storageHost", mpElement);
				storagePort = Integer.valueOf( getTagValue("storagePort", mpElement) );
				fileSenderDelay = Integer.valueOf( getTagValue("fileSenderDelay", mpElement) );
				showConsole = Boolean.parseBoolean( getTagValue("activationShowConsole", mpElement) );
				clearDataAfterFinish = Boolean.parseBoolean( getTagValue("clearDataAfterFinish", mpElement) );
				
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
