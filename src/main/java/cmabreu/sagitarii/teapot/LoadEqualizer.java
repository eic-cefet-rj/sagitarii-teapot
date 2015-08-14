package cmabreu.sagitarii.teapot;

import java.util.ArrayList;
import java.util.List;

public class LoadEqualizer {
	private static final int TASKS_MEDIUM_SIZE = 10;
	private static final int LOADS_MEDIUM_SIZE = 50;
	private static List<Integer> mediumLimit = new ArrayList<Integer>();
	private static List<Double> mediumLoad = new ArrayList<Double>();
	private static int defaultMaxLimit = 0;
	
	public static double getLoadsMedium( double value ) {
		mediumLoad.add( value );
		if ( mediumLoad.size() > LOADS_MEDIUM_SIZE ) {
			mediumLoad.remove(0);
		}
		Double totalValue = 0.0;
		for ( Double val : mediumLoad ) {
			totalValue = totalValue + val;
		}
		return totalValue / mediumLoad.size();
	}
	
	public static int getTasksMedium( int value ) {
		mediumLimit.add( value );
		if ( mediumLimit.size() > TASKS_MEDIUM_SIZE ) {
			mediumLimit.remove(0);
		}

		int totalValue = 0;
		for ( int val : mediumLimit ) {
			totalValue = totalValue + val;
		}
		
		return totalValue / mediumLimit.size();
	}
	
	public static boolean tooHigh( double load ) {
		return load >=95;
	}

	public static boolean tooLow( double load ) {
		return load < 90;
	}

	public static boolean outOfBounds( double cpuLoad, double load ) {
		double value = (load - cpuLoad);
		return ( (value < -5) || (value > 5) );
	}
	
	public synchronized static void equalize( Configurator configurator, int totalTasksRunning ) {
		double cpuLoad = configurator.getSystemProperties().getCpuLoad();
		boolean enforceTaskLimitToCores = configurator.enforceTaskLimitToCores();
		int activationsMaxLimit = configurator.getActivationsMaxLimit();

		if ( defaultMaxLimit == 0 ) {
			defaultMaxLimit = activationsMaxLimit;
		}
		

		if ( enforceTaskLimitToCores || (totalTasksRunning == 0) ) {
			System.out.println("no tasks running");
			return;
		}

		double load = getLoadsMedium(cpuLoad);
		//double load = cpuLoad;
		//int oldActivationsMaxLimit = activationsMaxLimit;
		
		System.out.print("Load: " + load + " : ");
		
		boolean acceptable = false;
		
		if ( tooHigh(load) && ( activationsMaxLimit > defaultMaxLimit ) ) {
			System.out.print("too high: " +  outOfBounds(cpuLoad,load) );
			activationsMaxLimit--;
		} else 
		
		if ( tooLow(load) ) {
			System.out.print("too low: " + outOfBounds(cpuLoad,load) );
			activationsMaxLimit++;
		} else {
			acceptable = true;
			System.out.println("acceptable: " + outOfBounds(cpuLoad,load) );
		}

		if ( !acceptable ) {
			int finalValue = getTasksMedium(activationsMaxLimit);
			if( load < 50 ) {
				finalValue = activationsMaxLimit;
			}
			configurator.setActivationsMaxLimit( finalValue );
	
			System.out.println(" AML: " + finalValue );
		}
		
	}
}
