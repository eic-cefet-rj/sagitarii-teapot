package cmabreu.sagitarii.teapot.comm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;
 
public class Client {
	private List<String> filesToSend;
	private String storageAddress;
	private int storagePort;
	private String sessionSerial;
	private String sagiHost;
	private int fileSenderDelay;
	private Logger logger = LogManager.getLogger( this.getClass().getName() );

	
	public Client( String storageAddress, int storagePort, String sagitariiUrlPort, int fileSenderDelay) {
		filesToSend = new ArrayList<String>();
		this.storageAddress = storageAddress;
		this.storagePort = storagePort;
		this.sagiHost = sagitariiUrlPort;
		this.fileSenderDelay = fileSenderDelay;
	}
	
	
	public synchronized void sendFile( String fileName, String folder, String targetTable, String experimentSerial,  
			String macAddress, String pipelineSerial, String activity, String fragment ) throws Exception {

		getSessionKey();
		
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		
		xml.append("<session macAddress=\""+macAddress+"\" instance=\""+pipelineSerial+
				"\" activity=\""+activity+"\"  fragment=\""+fragment+"\" experiment=\""+experimentSerial+
				"\" id=\""+sessionSerial+"\" targetTable=\""+targetTable+"\">\n");
		
		xml.append("<file name=\""+fileName+"\" type=\"FILE_TYPE_CSV\" />\n");
		filesToSend.add( folder + File.separator + fileName );
		
		File filesFolder = new File( folder + File.separator + "outbox" );
	    for (final File fileEntry : filesFolder.listFiles() ) {
	        if ( !fileEntry.isDirectory() ) {
	    		xml.append("<file name=\""+fileEntry.getName()+"\" type=\"FILE_TYPE_FILE\" />\n");
	    		filesToSend.add( folder + File.separator + "outbox" + File.separator + fileEntry.getName() );
	        }
	    }
		
		xml.append("</session>\n");
		filesToSend.add( folder + File.separator + "session.xml" );
		PrintWriter writer = new PrintWriter( new FileOutputStream(folder + File.separator + "session.xml") );
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
	 * @param source_filepath source
	 * @param destinaton_zip_filepath target
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

        file.delete();
        logger.debug("done sending " + file.getName() );
        return file.length();
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