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
import java.util.ArrayList;
import java.util.List;

import cmabreu.sagitarii.teapot.comm.Downloader;

public class RepositoryManager {
	private String folderName;
	private String MANIFEST_FILE; 
	private boolean hasFolder = false;
	private Logger logger = LogManager.getLogger( this.getClass().getName()  );
	private Configurator configurator;

	public RepositoryManager( Configurator configurator ) {
		this.configurator = configurator;
		this.folderName = configurator.getSystemProperties().getTeapotRootFolder() + "/wrappers";
		this.MANIFEST_FILE = folderName + "/" + "manifesto.xml";
		this.hasFolder = createRepositoryFolder();
	}

	private void downloadActivity( String sagitariiHost, String file ) throws Exception {
		Downloader dl = new Downloader();
		dl.download( sagitariiHost + "repository/" + file , folderName + "/" + file, false );
	}
	
	public void downloadWrappers(  ) throws Exception  {
		String sagitariiHost = configurator.getHostURL();
		OsType osType = configurator.getSystemProperties().getOsType();
		if ( hasFolder ) {
			try {
				downloadManifest( sagitariiHost + "getManifest" );
			} catch ( Exception e ) {
				throw e;
			}
			logger.debug("Verifying wrappers...");
			try {
				Configurator gf;
				List<Wrapper> act = new ArrayList<Wrapper>();
				try {
					gf = new Configurator( MANIFEST_FILE );
					act =  gf.getRepositoryList();
				} catch ( Exception e2 ) {
					logger.error("Incorrect or empty Manifest file. Check if you have any Sagitarii Activity Executor registered.");
					System.exit(1);
				}
				
				for ( Wrapper acc : act ) {
					if ( ( acc.target.toUpperCase().equals( osType.toString() ) || acc.target.toUpperCase().equals("ANY") ) && !acc.type.equals("SELECT") ) {
						logger.debug( "Check " + acc.fileName + " " + acc.version + " " + acc.target );
						File theFile = new File( folderName + File.separator + acc.fileName );
						if ( ( !theFile.exists() ) || acc.reload  ) {
							logger.debug("Downloading " + sagitariiHost + acc.fileName + " " + acc.version + ". Wait...");
							try {
								downloadActivity( sagitariiHost, acc.fileName );
								logger.debug(acc.fileName + " ok.");
							} catch ( Exception e ) {
								logger.error(acc.fileName + " not found : " + e.getMessage() );
							}
						}
					}
				}
				logger.debug("Done verifying wrappers.");
			} catch ( Exception e) {
				throw e;
			}
		} 
	}

	private void downloadManifest(String from) throws Exception {
		if ( hasFolder ) {
			logger.debug("Downloading manifest.");
			Downloader dl = new Downloader();
			dl.download(from , MANIFEST_FILE, false );
		}
	}


	private boolean createRepositoryFolder() {
		boolean result = false;
		File theDir = new File( folderName );
		if ( !theDir.exists() ) {
			logger.debug("Creating repository folder.");
			try{
				theDir.mkdir();
				result = true;
			} catch(Exception se){
				logger.error("Error creating repository folder.");
			}        
		} else { result = true; }
		return result;
	}

	
}
