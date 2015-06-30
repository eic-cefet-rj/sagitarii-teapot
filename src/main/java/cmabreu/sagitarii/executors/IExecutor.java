package cmabreu.sagitarii.executors;

import java.util.List;

public interface IExecutor {
	int run(String rScript, String workFolder);
	List<String> getConsole();
}
