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
	private List<Wrapper> wrappers = new ArrayList<Wrapper>();

	public RepositoryManager( Configurator configurator ) {
		this.configurator = configurator;
		this.folderName = configurator.getSystemProperties().getTeapotRootFolder() + "wrappers";
		this.MANIFEST_FILE = folderName + "/" + "manifesto.xml";
		this.hasFolder = createRepositoryFolder();
	}

	private void runInstaller( String script ) {
		logger.warn(" > run install routine from " + folderName + "/" + script );
	}
	
	private void downloadActivity( String sagitariiHost, String file ) throws Exception {
		Downloader dl = new Downloader();
		dl.download( sagitariiHost + "repository/" + file , folderName + "/" + file, false );
	}
	
	private boolean checkHash( String hash ) {
		for ( Wrapper wrapper : wrappers ) {
			logger.debug("checking wrapper "+wrapper.fileName+" hash: " + wrapper.hash + " > " + hash);
			if ( wrapper.hash.equals( hash ) ) {
				return true;
			}
		}
		return false;
	}
	
	public void downloadWrappers(  ) throws Exception  {
		String sagitariiHost = configurator.getHostURL();
		OsType osType = configurator.getSystemProperties().getOsType();
		if ( hasFolder ) {
			logger.debug("Verifying wrappers...");

			File manifest = new File( MANIFEST_FILE );
			if ( manifest.exists() ) {
				try {
					Configurator gf = new Configurator( MANIFEST_FILE );
					wrappers =  gf.getRepositoryList();
					logger.debug("current manifest have " + wrappers.size() + " files");
				} catch ( Exception e2 ) {
					logger.error("Error reading existent repository manifest.");
				}
			}
			
			try {
				downloadManifest( sagitariiHost + "getManifest" );
			} catch ( Exception e ) {
				throw e;
			}

			List<Wrapper> newWrappers = new ArrayList<Wrapper>();
			try {
				 
				try {
					Configurator gf = new Configurator( MANIFEST_FILE );
					newWrappers =  gf.getRepositoryList();
				} catch ( Exception e2 ) {
					logger.error("Incorrect or empty Manifest file. Check if you have any Sagitarii Activity Executor registered.");
					System.exit(1);
				}
				
				for ( Wrapper wrapper : newWrappers ) {
					if ( ( wrapper.target.toUpperCase().equals( osType.toString() ) || wrapper.target.toUpperCase().equals("ANY") ) ) {
						logger.debug( "Check " + wrapper.fileName + " [" + wrapper.hash + "]" );

						if ( !checkHash( wrapper.hash ) ) {
							logger.debug("Downloading " + sagitariiHost + wrapper.fileName + " " + wrapper.version + ". Wait...");
							try {
								downloadActivity( sagitariiHost, wrapper.fileName );
								logger.debug(wrapper.fileName + " ok.");
								if ( wrapper.type.equals("RSCRIPT") ) {
									runInstaller( wrapper.fileName );
								}
							} catch ( Exception e ) {
								logger.error(wrapper.fileName + " not found : " + e.getMessage() );
							}
						} else {
							logger.debug("already have wrapper " + wrapper.fileName + " with this hash.");
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
