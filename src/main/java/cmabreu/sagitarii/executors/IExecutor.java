package cmabreu.sagitarii.executors;

import java.util.List;

public interface IExecutor {
	int execute(String rScript, String workFolder);
	List<String> getConsole();
}
