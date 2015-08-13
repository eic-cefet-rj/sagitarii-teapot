package cmabreu.sagitarii.teapot;

import java.util.ArrayList;
import java.util.List;

public class LoadEqualizer {
	private static int originalMaxLimit = 0;
	private static final int MEDIUM_SIZE = 10;
	private static List<Integer> mediumLimit = new ArrayList<Integer>();
	
	
	public static int getMedium( int value ) {
		mediumLimit.add( value );
		if ( mediumLimit.size() > MEDIUM_SIZE ) {
			mediumLimit.remove(0);
		}

		int totalValue = 0;
		for ( int val : mediumLimit ) {
			totalValue = totalValue + val;
		}
		
		System.out.println("Value: " + value + " Size: " + mediumLimit.size() + " Total: " + totalValue + " Medium: " + totalValue / mediumLimit.size() );
		
		return totalValue / mediumLimit.size();
	}
	
	public synchronized static void equalize( Configurator configurator, int totalTasksRunning ) {
		
		if ( totalTasksRunning == 0 ) {
			return;
		}
		
		double load = configurator.getSystemProperties().getCpuLoad();
		boolean enforceTaskLimitToCores = configurator.enforceTaskLimitToCores();
		int activationsMaxLimit = configurator.getActivationsMaxLimit();
		
		if ( originalMaxLimit == 0) {
			originalMaxLimit = activationsMaxLimit;
		}
		
		if ( enforceTaskLimitToCores ) {
			return;
		}

		if ( load < 97 ) {
			activationsMaxLimit++;
		} else {
			activationsMaxLimit--;
		}
		
		int finalValue = getMedium(activationsMaxLimit);
		if( load < 50 ) {
			finalValue = activationsMaxLimit;
		}
		
		configurator.setActivationsMaxLimit( finalValue );
		
	}
}
