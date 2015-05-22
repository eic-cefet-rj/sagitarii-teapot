package cmabreu.sagitarii.teapot;

public class SpeedEqualizer {
	private static Logger logger = LogManager.getLogger( "cmabreu.sagitarii.teapot.SpeedEqualizer" ); 
	private static int originalValue = -1;
	
	public static void equalize( Configurator configurator, int totalTasksRunning ) {
		// Do not set getPoolIntervalMilliSeconds to more than 4000ms or Sagitarii will think we are dead.

		double dangerousLimit = ( configurator.getActivationsMaxLimit() - ( configurator.getActivationsMaxLimit() / 7 ) );
		double redLineLimit = ( configurator.getActivationsMaxLimit() - ( configurator.getActivationsMaxLimit() / 5 ) );
		
		if ( !configurator.useSpeedEqualizer() ) {
			return;
		}
		
		// Store the user default value (from config file)
		if ( originalValue == -1 ) {
			originalValue = configurator.getPoolIntervalMilliSeconds();
		}
		
		// First choice: user default value (if we fall in no alternatives)
		configurator.setPoolIntervalMilliSeconds( originalValue );

		// Sagitarii may have no tasks for us
		if ( totalTasksRunning == 0 ) {
			configurator.setPoolIntervalMilliSeconds( 4000 );
			logger.debug("system is idle. sleep interval set to " + configurator.getPoolIntervalMilliSeconds() + "ms" );
		}

		// Too close to limit: slow down
		if ( (totalTasksRunning > redLineLimit) && ( totalTasksRunning < dangerousLimit )) {
			configurator.setPoolIntervalMilliSeconds( 2000 );
			logger.debug("task buffer is close to the limit. sleep interval set to " + configurator.getPoolIntervalMilliSeconds() + "ms" );
		}

		// Living in the edge: push the brakes!
		if ( totalTasksRunning > dangerousLimit ) {
			configurator.setPoolIntervalMilliSeconds( 3500 );
			logger.debug("task buffer is almost full. sleep interval set to " + configurator.getPoolIntervalMilliSeconds() +"ms" );
		}
		
	}

}
