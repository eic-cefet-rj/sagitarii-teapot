package cmabreu.sagitarii.teapot.console.commands;

import java.util.List;

import cmabreu.sagitarii.teapot.console.CommandLine;

public class Quit implements CommandLine.ICommand {

	@Override
	public boolean doIt(List<String> v) {
		System.exit(0);
		return true;
	}
	
}
