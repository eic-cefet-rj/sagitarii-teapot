package cmabreu.sagitarii.teapot.console;

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
		jr.setCommandLineVersion("Command Line v.01");
		
		jr.assignClassToCommnd("logger", "cmabreu.sagitarii.teapot.console.commands.LoggerManager");
		jr.assignClassToCommnd("show", "cmabreu.sagitarii.teapot.console.commands.Show");
		jr.assignClassToCommnd("quit", "cmabreu.sagitarii.teapot.console.commands.Quit");
		jr.assignClassToCommnd("system", "cmabreu.sagitarii.teapot.console.commands.CoreSystem");

		System.out.println("Interactive Mode Activated");
		System.out.println("");
		
		jr.init();
		jr.parseStream(new StreamTokenizer(inputSrc));
	}
	
}
