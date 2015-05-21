package cmabreu.sagitarii.teapot;

public class SpeedEqualizer {
	private static Logger logger = LogManager.getLogger( "cmabreu.sagitarii.teapot.SpeedEqualizer" ); 
	private static int originalValue = -1;
	
	public static void equalize( Configurator configurator, int totalTasksRunning ) {
		
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
		// Do not set more than 4000ms or Sagitarii will think we are dead.
		if ( totalTasksRunning == 0 ) {
			configurator.setPoolIntervalMilliSeconds( 4000 );
			logger.debug("speed equalized to " + configurator.getPoolIntervalMilliSeconds() );
		}

		// Too few tasks ( less than half ): Speed up
		if ( ( totalTasksRunning > 0 ) && ( totalTasksRunning < ( configurator.getActivationsMaxLimit() / 2 ) ) ) {
			configurator.setPoolIntervalMilliSeconds( 200 );
			logger.debug("speed equalized to " + configurator.getPoolIntervalMilliSeconds() );
		}
		
		// Too close to limit: slow down
		if ( totalTasksRunning >= ( configurator.getActivationsMaxLimit() - ( configurator.getActivationsMaxLimit() / 5 ) ) ) {
			configurator.setPoolIntervalMilliSeconds( 2000 );
			logger.debug("speed equalized to " + configurator.getPoolIntervalMilliSeconds() );
		}

		// Near stress limit : push the brakes!
		if ( totalTasksRunning >= ( configurator.getActivationsMaxLimit() - ( configurator.getActivationsMaxLimit() / 7 ) ) ) {
			configurator.setPoolIntervalMilliSeconds( 3000 );
			logger.debug("speed equalized to " + configurator.getPoolIntervalMilliSeconds() );
		}
		
	}

}
