package cmabreu.sagitarii.teapot;


public class LoadEqualizer {
	private static final int TESTS_BEFORE_CHANGE = 30;
	
	private static int defaultMaxLimit = 0;
	private static int testCount = 0;
	
	public static boolean tooHigh( double load ) {
		return load >=95;
	}

	public static boolean tooLow( double load ) {
		return load < 90;
	}

	public synchronized static void equalize( Configurator configurator, int totalTasksRunning ) {
		double load = configurator.getSystemProperties().getCpuLoad();
		boolean enforceTaskLimitToCores = configurator.enforceTaskLimitToCores();
		int activationsMaxLimit = configurator.getActivationsMaxLimit();

		boolean tooLow = false;
		boolean tooHigh = false;
		
		
		if ( defaultMaxLimit == 0 ) {
			defaultMaxLimit = activationsMaxLimit;
		}
		

		if ( enforceTaskLimitToCores || (totalTasksRunning == 0) ) {
			System.out.println("no tasks running");
			return;
		}

		System.out.print("Load: " + load + "% : ");
		
		boolean acceptable = false;
		
		if ( tooHigh(load) ) {
			System.out.print("too high" );
			tooHigh = true;
		} else 
		
		if ( tooLow(load) ) {
			System.out.print("too low" );
			tooLow = true;
		} else {
			acceptable = true;
			testCount = 0;
			System.out.print("acceptable" );
		}

		testCount++;
		if ( !acceptable && ( testCount >= TESTS_BEFORE_CHANGE ) ) {
				if ( tooLow ) {
					activationsMaxLimit++;
				}
				if ( tooHigh && ( activationsMaxLimit > 1 ) ) {
					activationsMaxLimit--;
				}
				
				testCount = 0;
				configurator.setActivationsMaxLimit( activationsMaxLimit );
		
				System.out.println("    AML set to: " + activationsMaxLimit );
		} else {
			System.out.println("     Nothing was changed: " + testCount );
		}
		
	}
}
