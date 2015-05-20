package cmabreu.sagitarii.teapot.console.commands;

import java.util.List;

import cmabreu.sagitarii.teapot.Activation;
import cmabreu.sagitarii.teapot.Main;
import cmabreu.sagitarii.teapot.comm.FileUnity;
import cmabreu.sagitarii.teapot.console.CommandLine;
import cmabreu.taskmanager.core.ITask;

public class Show implements CommandLine.ICommand {

	@Override
	public boolean doIt( List<String> v ) {

		if ( v.get(1).equals("tasks") ) {
			System.out.println("Running tasks:");
			for ( ITask task : Main.getTaskManager().getTasks() ) {
				Activation act = task.getActivation();
				System.out.println(" > " + act.getActivitySerial() + " " + act.getExecutorType() + " " + act.getExecutor() );
			}
		}

		if ( v.get(1).equals("recorded") ) {
			System.out.println("Recorded tasks:");
			for ( ITask task : Main.getTaskManager().getRecordedTasks() ) {
				Activation act = task.getActivation();
				System.out.println(" > " + act.getActivitySerial() + " " + act.getExecutorType() + " " + act.getExecutor() );
				System.out.println(" > Command: " + act.getCommand() );
				System.out.println(" > Namespace: " + act.getNamespace() );
				System.out.println(" > Instance: " + act.getPipelineSerial() );
				System.out.println(" > Target: " + act.getTargetTable() );
				System.out.println(" > Files: " );
				for ( FileUnity fu : act.getFiles() ) {
					System.out.println("    > " + fu.getName() );
				}
				
			}
		}
		
		if ( v.get(1).equals("fragments") ) {
			System.out.println("Running fragments:");
			for ( ITask task : Main.getTaskManager().getTasks() ) {
				Activation act = task.getActivation();
				System.out.println(" > " + act.getFragment() );
			}
		}
		
		if ( v.get(1).equals("workflows") ) {
			System.out.println("Running workflows:");
			for ( ITask task : Main.getTaskManager().getTasks() ) {
				Activation act = task.getActivation();
				System.out.println(" > " + act.getWorkflow() );
			}
		}
		
		return true;
	}
	
}