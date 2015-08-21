package cmabreu.sagitarii.teapot;

import java.util.ArrayList;
import java.util.List;

public class LoadEqualizer {
	private static Logger logger = LogManager.getLogger( "cmabreu.sagitarii.teapot.LoadEqualizer" ); 
	private static final int TESTS_BEFORE_CHANGE = 3;
	private static final int ACCEPTABLE_UPPER_LIMIT = 95;
	private static final int ACCEPTABLE_LOWER_LIMIT = 90;
	private static final int LOADS_MEDIUM_SIZE = 50;
	private static int INITIAL_TASK_LIMIT = 0;
	private static List<Double> mediumLoad = new ArrayList<Double>();
	
	private static int defaultMaxLimit = 0;
	private static int testCount = 0;
	private static int noTaskCheckCount = 0;
	
	public static boolean tooHigh( double load ) {
		return load >= ACCEPTABLE_UPPER_LIMIT;
	}

	public static boolean tooLow( double load ) {
		return load < ACCEPTABLE_LOWER_LIMIT;
	}
	

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

	public synchronized static void equalize( Configurator configurator, int totalTasksRunning ) {
		boolean enforceTaskLimitToCores = configurator.enforceTaskLimitToCores();
		if ( enforceTaskLimitToCores ) return;
		
		double load = getLoadsMedium ( configurator.getSystemProperties().getCpuLoad() );
		//double ramLoad = configurator.getSystemProperties().getMemoryPercent();
		
		int activationsMaxLimit = configurator.getActivationsMaxLimit();

		if ( INITIAL_TASK_LIMIT == 0 ) {
			INITIAL_TASK_LIMIT = activationsMaxLimit;
		}
		
		boolean tooLow = false;
		boolean tooHigh = false;
		
		
		if ( defaultMaxLimit == 0 ) {
			defaultMaxLimit = activationsMaxLimit;
			logger.debug("AML default value is " + INITIAL_TASK_LIMIT );
		}
		

		if ( totalTasksRunning == 0) {
			noTaskCheckCount++;
			if ( noTaskCheckCount > TESTS_BEFORE_CHANGE ) {
				logger.debug("AML set to default value: " + INITIAL_TASK_LIMIT );
				activationsMaxLimit = INITIAL_TASK_LIMIT;
				noTaskCheckCount = 0;
			}
			return;
		}

		boolean acceptable = false;
		
		if ( tooHigh(load) ) {
			tooHigh = true;
		} else 
		
		if ( tooLow(load) ) {
			tooLow = true;
		} else {
			acceptable = true;
			testCount = 0;
		}

		testCount++;
		if ( !acceptable && ( testCount >= TESTS_BEFORE_CHANGE ) ) {
			
			String where = "too low ";
			if ( tooLow ) {
				activationsMaxLimit++;
			}
			if ( tooHigh && ( activationsMaxLimit > 1 ) ) {
				where = "too high";
				activationsMaxLimit--;
			}
			
			testCount = 0;
			configurator.setActivationsMaxLimit( activationsMaxLimit );
	
			logger.debug( "[" + totalTasksRunning + "] Load: " + load + "% (" + where + ") : AML is now " + activationsMaxLimit);
			
		} else {
			//logger.debug(" Nothing was changed: " + testCount );
		}
		
	}
}
