package cmabreu.sagitarii.teapot.comm;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

import cmabreu.sagitarii.teapot.Configurator;
import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;
import cmabreu.sagitarii.teapot.Task;
 
public class Client {
	private List<String> filesToSend;
	private String storageAddress;
	private int storagePort;
	private String sessionSerial;
	private String sagiHost;
	private int fileSenderDelay;
	private Logger logger = LogManager.getLogger( this.getClass().getName() );

	
	public Client( Configurator configurator ) {
		filesToSend = new ArrayList<String>();
		this.storageAddress = configurator.getStorageHost();
		this.storagePort = configurator.getStoragePort();
		this.sagiHost = configurator.getHostURL();
		this.fileSenderDelay = configurator.getFileSenderDelay();
	}
	
	
	public void sendFile( String fileName, String folder, String targetTable, String experimentSerial,  
			String macAddress, Task task ) throws Exception {

		String instanceSerial = "";
		String activity = "";
		String fragment = "";
		String taskId = "";
		String exitCode = "0";
		String startTimeMillis = "";
		String finishTimeMillis = "";
		
		if ( task != null ) {
			instanceSerial = task.getActivation().getInstanceSerial();
			activity = task.getActivation().getActivitySerial();
			fragment = task.getActivation().getFragment();
			exitCode = String.valueOf( task.getExitCode() );
			taskId = task.getActivation().getTaskId();
			
			startTimeMillis = String.valueOf( task.getRealStartTime().getTime() );
			finishTimeMillis = String.valueOf( task.getRealFinishTime().getTime() );
		}			
		
		getSessionKey();
		
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		
		xml.append("<session macAddress=\""+macAddress+"\" instance=\""+instanceSerial+
				"\" activity=\""+activity+"\"  taskId=\""+taskId+"\" exitCode=\""+exitCode+"\" fragment=\""+fragment + 
				"\" startTime=\""+startTimeMillis + "\" finishTime=\""+finishTimeMillis +
				"\" experiment=\""+experimentSerial + "\" id=\""+sessionSerial+"\" targetTable=\""+targetTable+"\">\n");
		
		
		File fil = new File(folder + "/" + fileName);
		if ( fil.exists() ) {
			xml.append("<file name=\""+fileName+"\" type=\"FILE_TYPE_CSV\" />\n");
			filesToSend.add( folder + "/" + fileName );
		} else {
			logger.error("will not send sagi_output.txt in session.xml file: this activity instance produced no data");
		}
		
		File filesFolder = new File( folder + "/" + "outbox" );
	    for (final File fileEntry : filesFolder.listFiles() ) {
	        if ( !fileEntry.isDirectory() ) {
	    		xml.append("<file name=\""+fileEntry.getName()+"\" type=\"FILE_TYPE_FILE\" />\n");
	    		filesToSend.add( folder + "/" + "outbox" + "/" + fileEntry.getName() );
	        }
	    }
		
	    xml.append("<file name=\"session.xml\" type=\"FILE_TYPE_SESSION\" />\n");
	    
	    xml.append("<console><![CDATA[");
	    if ( task != null ) {
	    	for ( String line : task.getConsole() ) {
	    		byte pline[] = line.getBytes("UTF-8");
	    		xml.append( new String(pline, "UTF-8") + "\n" );
	    	}
	    }
	    xml.append("]]></console>");

	    
	    xml.append("<execLog><![CDATA[");
	    if ( task != null ) {
	    	for ( String line : task.getExecLog() ) {
	    		byte pline[] = line.getBytes("UTF-8");
	    		xml.append( new String(pline, "UTF-8") + "\n" );
	    	}
	    }
	    xml.append("]]></execLog>");
	    
		xml.append("</session>\n");
		filesToSend.add( folder + "/" + "session.xml" );
		
		Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(folder + "/" + "session.xml"), "UTF-8"));
		writer.write( xml.toString() );
		writer.close();

		long totalBytesSent = 0;
		if ( filesToSend.size() > 0 ) {
			logger.debug("need to send " + filesToSend.size() + " files to Sagitarii...");
			int indexFile = 1;
			for ( String toSend : filesToSend ) {
				logger.debug("[" + indexFile + "] will send " + toSend );
				indexFile++;
				totalBytesSent = totalBytesSent + uploadFile( toSend, targetTable, experimentSerial, sessionSerial );
			}
			logger.debug("total bytes sent: " + totalBytesSent );
		}
		commit();
	}
	
	
	/**
	 * Compress a file
	 */
	public void compress(String source_filepath, String destinaton_zip_filepath) {
		logger.debug("compressing file ...");
		byte[] buffer = new byte[1024];
		try {
			FileOutputStream fileOutputStream =new FileOutputStream(destinaton_zip_filepath);
			GZIPOutputStream gzipOuputStream = new GZIPOutputStream(fileOutputStream);
			FileInputStream fileInput = new FileInputStream(source_filepath);
			int bytes_read;
			while ((bytes_read = fileInput.read(buffer)) > 0) {
				gzipOuputStream.write(buffer, 0, bytes_read);
			}
			fileInput.close();
			gzipOuputStream.finish();
			gzipOuputStream.close();
			fileOutputStream.close();
			
			logger.debug( "file was compressed successfully" );

		} catch (IOException ex) {
			logger.error("error compressing file: " + ex.getMessage() );
		}
	}	
	
	
	private synchronized long uploadFile( String fileName, String targetTable, String experimentSerial, String sessionSerial ) throws Exception {
		String newFileName = fileName + ".gz";
		
		compress(fileName, newFileName);
		File file = new File(newFileName);

        logger.debug("sending " + file.getName() + " with size of " + file.length() + " bytes..." );
		
		@SuppressWarnings("resource")
		Socket socket = new Socket( storageAddress, storagePort);
        ObjectOutputStream oos = new ObjectOutputStream( socket.getOutputStream() );
        oos.flush();
 
        oos.writeObject( file.getName().replace(".gz", "") );
        
        oos.writeObject( sessionSerial );
        oos.writeObject( file.length() );
        oos.writeObject( targetTable );
        oos.writeObject( experimentSerial );
 
        FileInputStream fis = new FileInputStream(file);
        
        byte [] buffer = new byte[100];
        Integer bytesRead = 0;
 
        while ( (bytesRead = fis.read(buffer) ) > 0) {
            oos.writeUnshared(bytesRead);
            oos.writeUnshared(Arrays.copyOf(buffer, buffer.length));
        }
        
        try {
        	Thread.sleep( fileSenderDelay );
        } catch ( Exception e ) {
        	
        }
        
        oos.close();
        fis.close();

        long size = file.length();
        logger.debug("done sending " + file.getName() );
        file.delete();
        return size;
	}

	private void commit() throws Exception {
		URL url = new URL( sagiHost + "/sagitarii/transactionManager?command=commit&sessionSerial=" + sessionSerial );
		Scanner s = new Scanner( url.openStream() );
		String response = s.nextLine();
		logger.debug("session "+sessionSerial+" commit: " + response);
		s.close();
	}
	
	private void getSessionKey() throws Exception {
		URL url = new URL( sagiHost + "/sagitarii/transactionManager?command=beginTransaction");
		Scanner s = new Scanner( url.openStream() );
		sessionSerial = s.nextLine();
		logger.debug("open session " + sessionSerial );
		s.close();
	}
	
}