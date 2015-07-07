package cmabreu.sagitarii.teapot;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import cmabreu.sagitarii.teapot.comm.FileUnity;

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

public class XMLParser {
	private Document doc;
	private Logger logger = LogManager.getLogger( this.getClass().getName() );

	
	private String getTagValue(String sTag, Element eElement) throws Exception{
		try {
			NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
	        Node nValue = (Node) nlList.item(0);
			return nValue.getNodeValue();
		} catch ( Exception e ) {
			throw e;
		}
	}

	
	private List<String> getSourceData( String sourceData ) {
		List<String> inputData = new ArrayList<String>();
		String line = "";
		for( int x = 0; x < sourceData.length(); x++  ) {
			String character = String.valueOf( sourceData.charAt(x) );
			if ( !character.equals("\n")  ) {
				line = line + character;
			} else {
				inputData.add( line );
				line = "";
			}
		}
		if ( line.length() > 0 ) {
			inputData.add( line );
		}
		return inputData;
	}
	
	public List<Activation> parseActivations( String xml ) throws Exception {
		logger.debug("parsing XML for tasks");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource( new StringReader(xml) );
			doc = dBuilder.parse( is );
			doc.getDocumentElement().normalize();
		} catch ( Exception e ) {
			logger.error( e.getMessage() );
			throw e;
		}
		
		NodeList pipeTag = doc.getElementsByTagName("instance");
		Node pipeConf = pipeTag.item( 0 );
		Element pipeElement = (Element) pipeConf;
		String pipeSerial = pipeElement.getAttribute("serial");
		String fragment = pipeElement.getAttribute("fragment");
		String experiment = pipeElement.getAttribute("experiment");
		String workflow = pipeElement.getAttribute("workflow");
		
		List<Activation> resp = new ArrayList<Activation>();
		NodeList mapconfig = doc.getElementsByTagName("activity");
		
		logger.debug( "found instance ID " + pipeSerial );
		
		for ( int x = 0; x < mapconfig.getLength(); x++ ) {
			try {
				Node mpconfig = mapconfig.item(x);
				Element mpElement = (Element) mpconfig;
				
				String sourceData = "";
				try { sourceData = getTagValue("inputData", mpElement); } catch ( Exception e3 ) {  }
				int order = Integer.valueOf( getTagValue("order", mpElement) );
				String serial = getTagValue("serial", mpElement);
				String command = getTagValue("command", mpElement);
				String type = getTagValue("type", mpElement);
				String executor = getTagValue("executor", mpElement);
				String executorType = getTagValue("executorType", mpElement);
				String targetTable = getTagValue("targetTable", mpElement);

				logger.debug(" > found task " + executor + " (execution order " + order + ")");
				
				Activation activation = new Activation();
				activation.setWorkflow(workflow);
				activation.setType(type);
				activation.setExperiment(experiment);
				activation.setInstanceSerial(pipeSerial);
				activation.setSourceData( getSourceData( sourceData ) );
				activation.setCommand(command);
				activation.setFragment(fragment);
				activation.setOrder(order);
				activation.setActivitySerial(serial);
				activation.setXmlOriginalData( xml );
				activation.setExecutor( executor );
				activation.setExecutorType( executorType );
				activation.setTargetTable( targetTable );
				
				try {
					NodeList nFileList = mpElement.getElementsByTagName("files").item(0).getChildNodes();
					for ( int y = 0; y < nFileList.getLength(); y++ ) {
						if( nFileList.item(y).getNodeType() == Node.ELEMENT_NODE){
							Element fileElement = (Element)nFileList.item(y);
							String fileName = fileElement.getAttribute("name");
							String table = fileElement.getAttribute("table");
							String attribute = fileElement.getAttribute("attribute");
							String index = fileElement.getAttribute("index");
							FileUnity fu = new FileUnity( fileName );
							fu.setId( Integer.valueOf( index ) );
							fu.setAttribute(attribute);
							fu.setSourceTable(table);
							activation.addFile( fu );
							
							logger.debug("found file " + fileName + " in XML instance for executor " + executor + " in field " + attribute + 
									"for executor " + executor + "(" + serial + ")" );
							
						} else {
							Node nFile = (Node) nFileList.item( y );
							logger.error("unknown node: " + nFile.getNodeName() );
						}
						
					}
				} catch ( Exception e ) {
					e.printStackTrace();
					logger.error( e.getMessage() );
				}
				
				resp.add(activation);
				
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		
		Collections.sort( resp );
		return resp;
		
	}
	
	

}
