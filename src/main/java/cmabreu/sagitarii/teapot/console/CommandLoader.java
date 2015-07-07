package cmabreu.sagitarii.teapot.console;

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

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;

public class CommandLoader extends Thread {
	
	@Override
	public void run() {
		loadCommands();
	}
	
	public void loadCommands() {
		Reader inputSrc = null;
		inputSrc = new InputStreamReader(System.in);

		CommandLine jr = new CommandLine();
		jr.setCommandLinePrompt("teapot> ");
		jr.setCommandLineVersion("Welcome to Teapot Command Line");
		
		jr.assignClassToCommnd("logger", "cmabreu.sagitarii.teapot.console.commands.LoggerManager");
		jr.assignClassToCommnd("show", "cmabreu.sagitarii.teapot.console.commands.Show");
		jr.assignClassToCommnd("quit", "cmabreu.sagitarii.teapot.console.commands.Quit");
		jr.assignClassToCommnd("exit", "cmabreu.sagitarii.teapot.console.commands.Quit");
		jr.assignClassToCommnd("system", "cmabreu.sagitarii.teapot.console.commands.CoreSystem");
		jr.assignClassToCommnd("testrun", "cmabreu.sagitarii.teapot.console.commands.TestRun");

		System.out.println("Interactive Mode Activated");
		System.out.println("");
		
		jr.init();
		jr.parseStream(new StreamTokenizer(inputSrc));
	}
	
}
