package cmabreu.sagitarii.teapot;

public class LoadEqualizer {
	private static Logger logger = LogManager.getLogger( "cmabreu.sagitarii.teapot.LoadEqualizer" ); 
	private static final int TESTS_BEFORE_CHANGE = 3;
	private static final int ACCEPTABLE_UPPER_LIMIT = 95;
	private static final int ACCEPTABLE_LOWER_LIMIT = 90;
	private static final int MAXIMUN_RAM_TO_USE = 90;
	
	private static int INITIAL_TASK_LIMIT = 0;
	private static int defaultMaxLimit = 0;
	private static int testCount = 0;
	private static int noTaskCheckCount = 0;
	
	public static boolean tooHigh( double load, double ramLoad ) {
		boolean ramBelowLimit = ( ramLoad < MAXIMUN_RAM_TO_USE );
		return ( load >= ACCEPTABLE_UPPER_LIMIT ) && ramBelowLimit;
	}

	public static boolean tooLow( double load, double ramLoad ) {
		boolean ramBelowLimit = ( ramLoad < MAXIMUN_RAM_TO_USE );
		return ( load < ACCEPTABLE_LOWER_LIMIT ) && ramBelowLimit;
	}
	

	public synchronized static void equalize( Configurator configurator, int totalTasksRunning ) {
		boolean enforceTaskLimitToCores = configurator.enforceTaskLimitToCores();
		if ( enforceTaskLimitToCores ) return;
		
		double load = configurator.getSystemProperties().getCpuLoad();
		double ramLoad = configurator.getSystemProperties().getMemoryPercent();
		
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
		
		if ( tooHigh(load,ramLoad) ) {
			tooHigh = true;
		} else 
		
		if ( tooLow(load,ramLoad) ) {
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
	
			logger.debug( "[" + totalTasksRunning + "] RAM Load: " + ramLoad + "% | CPU Load: " + load + "% (" + where + ") : AML is now " + activationsMaxLimit);
			
		} else {
			//logger.debug(" Nothing was changed: " + testCount );
		}
		
	}
}
