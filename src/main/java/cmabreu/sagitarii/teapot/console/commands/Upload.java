
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

import java.util.List;

import cmabreu.sagitarii.teapot.Main;
import cmabreu.sagitarii.teapot.comm.Uploader;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class Upload implements CommandLine.ICommand {

	@Override
	public boolean doIt(List<String> v) {

		if ( v.size() != 5 ) {
			System.out.println("upload <file.csv> <target_table> <experiment_tag> <work_folder>");
			return true;
		}
		
		String fileName = v.get(1);
		String relationName = v.get(2);
		String experimentSerial = v.get(3);
		String folderName = v.get(4);
		
		System.out.println("WARNING: This will block the main thread. All current tasks will continue.");
		System.out.println("Uploading file " + fileName + ". Wait...");
		
		try {
			new Uploader( Main.getConfigurator() ).uploadCSV(fileName, relationName, experimentSerial, folderName, null, Main.getConfigurator().getSystemProperties()  );
		} catch ( Exception e ) {
			e.printStackTrace();
		}

		System.out.println("Done uploading file " + fileName);
		return true;
	}
	
}
