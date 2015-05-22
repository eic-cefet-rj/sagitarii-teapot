package cmabreu.sagitarii.teapot.console.commands;

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
