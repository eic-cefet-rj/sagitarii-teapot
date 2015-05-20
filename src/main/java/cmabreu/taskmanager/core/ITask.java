package cmabreu.taskmanager.core;

import java.util.List;

import cmabreu.sagitarii.teapot.Activation;

public interface ITask extends Runnable{
	String getTaskId();
	String getApplicationName();
	TaskStatus getTaskStatus();
	int getExitCode();
	Activation getActivation();
	List<String> getSourceData();
	void setSourceData(List<String> sourceData);
}
