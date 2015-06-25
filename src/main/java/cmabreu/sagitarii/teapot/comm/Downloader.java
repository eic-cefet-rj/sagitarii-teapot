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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Logger;

public class Downloader {
	private Logger logger = LogManager.getLogger( this.getClass().getName() ); 

	/**
	 * Faz o download de um arquivo do servidor.
	 * 
	 * @param from URL do arquivo
	 * @param to pasta para salvar o arquivo apos o download.
	 */
	public void download( String from, String to, boolean decompress ) throws Exception {
		String fileName = to;
		if ( decompress ) {
			fileName = fileName + ".gz";
		}
		
		logger.debug("downloading " + fileName + "... may take some time.");
		
		URL link = new URL(from); 	
		InputStream in = new BufferedInputStream(link.openStream());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int n = 0;
		while (-1 != (n = in.read(buf) ) ) {
			out.write(buf, 0, n);
		}
		out.close();
		in.close();
		byte[] response = out.toByteArray();

		FileOutputStream fos = new FileOutputStream( fileName );
		fos.write(response);
		fos.close();
		
		File check = new File( fileName );
		if ( check.exists() ) {
			long size = check.length();
			logger.debug("done downloading " + fileName + ": " + size + " bytes");
			if ( size == 0 ) {
				throw new Exception(fileName + " is empty. Check original file.");
			}
			if ( decompress ) {
				decompress(fileName, to);
				new File( fileName ).delete();
			}
		} else {
			throw new Exception("File "+fileName+" was not received! Check file in Sagitarii repository.");
		}
	}

	public void decompress( String compressedFile, String decompressedFile ) throws Exception {
		logger.debug("uncompressing " + compressedFile + "...");
		byte[] buffer = new byte[1024];
		try {
			FileInputStream fileIn = new FileInputStream(compressedFile);
			GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn);
			FileOutputStream fileOutputStream = new FileOutputStream(decompressedFile);
			int bytes_read;
			while ((bytes_read = gZIPInputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, bytes_read);
			}
			gZIPInputStream.close();
			fileOutputStream.close();
			logger.debug("file was decompressed successfully");
		} catch (IOException ex) {
			logger.error("error decompressing file: " + ex.getMessage() );
			throw new Exception( ex.getMessage() );
		}
	}

}
