package cmabreu.sagitarii.teapot;

public class SpeedEqualizer {
	private static Logger logger = LogManager.getLogger( "cmabreu.sagitarii.teapot.SpeedEqualizer" ); 
	private static int originalValue = -1;
	
	public static void equalize( Configurator gf, int totalTasksRunning ) {
		// Store the user default value (from config file)
		if ( originalValue == -1 ) {
			originalValue = gf.getPoolIntervalMilliSeconds();
		}
		
		// First choice: user default value (if we fall in no alternatives)
		gf.setPoolIntervalMilliSeconds( originalValue );

		// Sagitarii may have no tasks for us
		// Do not set more than 4000ms or Sagitarii will think we are dead.
		if ( totalTasksRunning == 0 ) {
			gf.setPoolIntervalMilliSeconds( 4000 );
			logger.debug("speed equalized to " + gf.getPoolIntervalMilliSeconds() );
		}

		// Too few tasks ( less than half ): Speed up
		if ( ( totalTasksRunning > 0 ) && ( totalTasksRunning < ( gf.getActivationsMaxLimit() / 2 ) ) ) {
			gf.setPoolIntervalMilliSeconds( 200 );
			logger.debug("speed equalized to " + gf.getPoolIntervalMilliSeconds() );
		}
		
		// Too close to limit: slow down
		if ( totalTasksRunning >= ( gf.getActivationsMaxLimit() - ( gf.getActivationsMaxLimit() / 5 ) ) ) {
			gf.setPoolIntervalMilliSeconds( 2000 );
			logger.debug("speed equalized to " + gf.getPoolIntervalMilliSeconds() );
		}

		// Near stress limit : push the brakes!
		if ( totalTasksRunning >= ( gf.getActivationsMaxLimit() - ( gf.getActivationsMaxLimit() / 7 ) ) ) {
			gf.setPoolIntervalMilliSeconds( 3000 );
			logger.debug("speed equalized to " + gf.getPoolIntervalMilliSeconds() );
		}
		
	}

}
