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

import java.util.ArrayList;
import java.util.List;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import cmabreu.sagitarii.executors.TextConsole;
import cmabreu.sagitarii.teapot.console.CommandLine;

public class CheckREngine implements CommandLine.ICommand {

	@Override
	public boolean doIt(List<String> v) {
		List<String> console = new ArrayList<String>();
		String rScript; 
		String workFolder;
		XX
		Rengine rengine = new Rengine(new String [] {"--vanilla"}, false, new TextConsole( console ) );
        if ( !rengine.waitForR() ) {
            System.out.println("Cannot load R");
            System.exit(1);
        }
		
		if ( v != null  ) {
			rScript = v.get(1);
			workFolder = v.get(2);
	        rengine.eval("sagitariiWorkFolder <- \""+ workFolder +"\"");
	        rengine.eval( "source( '" + rScript + "') " );
	
	        REXP message = rengine.eval("messageToSagitarii");
	        if ( message != null ) {
	        	console.add( message.toString() );
	        }

		} else {

	        REXP message = rengine.eval("3 * 5 + 2");
	        if ( message != null ) {
	        	console.add( message.toString() );
	        }		
        
		}
		
		for ( String s : console ) {
			System.out.println( s );
		}
		
		return true;
	}
	
}
