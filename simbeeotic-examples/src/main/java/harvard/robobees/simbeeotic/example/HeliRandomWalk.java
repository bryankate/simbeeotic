package harvard.robobees.simbeeotic.example;


import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.BaseHeliBehavior;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.Platform;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.Timer;
import org.apache.log4j.Logger;


/**
 * Demonstration behavior for testbed helicopters.
 * Helicopter lifts off, completes a random walk and subsequently lands.
 * 
 * @author dpalmer
 */
public class HeliRandomWalk extends BaseHeliBehavior {
    
	private PositionSensor sensor;
	private Timer timer;
	private Vector3f setpoint;
	private boolean reached = false;
	private int setpointCount = 0;
	private Random rand;
	private static Logger logger = Logger.getLogger(HeliRandomWalk.class);
	
	
	/**
     * Takes the helicopter for a random walk.
     *
     * @param platform The platform upon which the behavior is executing.
     * @param control The control interface for the helicopter.
     */
	@Override
    public void start(Platform platform, HeliControl control)
	{
		super.start(platform, control);
		
		rand = new Random(System.nanoTime());
		
		sensor = platform.getSensor("position-sensor", PositionSensor.class);
		Vector3f pos = sensor.getPosition();
		
		setpoint = new Vector3f(pos.x, pos.y, 0.5f);
		logger.info("\nTaking off!");
		moveToPoint(setpoint.x, setpoint.y, setpoint.z, 0.1,
			new MoveCallback() {
        		@Override
        		public void reachedDestination() {reached = true;}
			});
		
		timer = platform.createTimer(new WalkCallback(), 0, TimeUnit.MILLISECONDS,
							10, TimeUnit.MILLISECONDS);
	}


    /**
     * Stops the behavior.
     */
	@Override
    public void stop()
	{
		super.stop();
		
		timer.cancel();
	}
	
	private class WalkCallback implements TimerCallback
	{
		public void fire(SimTime time)
		{
			if(setpointCount > 20)
			{
				idle();
			}

			if(reached)
			{
				reached = false;
				setpointCount++;
				setpoint = new Vector3f(2*rand.nextFloat() - 1,
										4*rand.nextFloat() - 2,
										rand.nextFloat() + 0.2f);
				
				logger.info("\nWaypoint " + setpointCount + ":" + setpoint);
				
				moveToPoint(setpoint.x, setpoint.y, setpoint.z, 0.25,
					new MoveCallback() {
                    	@Override
                    	public void reachedDestination() {
                    		logger.info("\nReached waypoint " + setpointCount + ".");
                    		reached = true;
                    	}
					});
			}
		}
	}
}