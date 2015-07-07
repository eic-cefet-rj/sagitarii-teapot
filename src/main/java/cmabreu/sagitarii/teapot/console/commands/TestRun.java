package cmabreu.sagitarii.teapot.console.commands;
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

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import cmabreu.sagitarii.teapot.LogManager;
import cmabreu.sagitarii.teapot.Main;
import cmabreu.sagitarii.teapot.TaskRunner;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class TestRun implements CommandLine.ICommand {

	@Override
	public boolean doIt( List<String> v ) {

		if ( v.size() < 2 ) {
			System.out.println("usage: testrun <instance_file.xml>");
			return true;
		} else {
			String instanceFile = v.get(1);
			try {
				System.out.println("will run instance " + instanceFile );
				byte[] encoded = Files.readAllBytes( Paths.get( instanceFile) );
				String instanceXML = new String(encoded, Charset.forName("UTF-8") );
				
				System.out.println("loaded " + instanceXML.length() + " bytes");
				
				LogManager.enableLoggers();
				
				TaskRunner tr = new TaskRunner( instanceXML, Main.getCommunicator(), Main.getConfigurator() );
				tr.start();
				
				System.out.println("started");
				
			} catch ( Exception e ) {
				System.out.println("error: " + e.getMessage() );
			}
		}

		return true;
	}
	
}