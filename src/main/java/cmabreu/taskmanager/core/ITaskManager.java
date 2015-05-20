package cmabreu.taskmanager.core;

import java.util.List;

import cmabreu.sagitarii.teapot.Activation;

public interface ITaskManager {
	void notify( ITask task );
	List<ITask> getTasks();
	int getAvailableProcessors();
	String getSoName();
	String getLocalIpAddress();
	Double getCpuLoad();
	String getMachineName();
	String getMacAddress();
	int getRunningTaskCount();
	void setObserver(ITaskObserver taskObserver);
	String getJavaVersion();
	OsType getOsType();
	String startTask(Activation activation, String applicationName);
}
