package cmabreu.sagitarii.executors;

import java.util.List;

import cmabreu.sagitarii.teapot.Activation;

public interface IExecutor {
	int execute( Activation activation );
	List<String> getConsole();
}
