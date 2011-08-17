package harvard.robobees.simbeeotic.example;


import static java.lang.Math.round;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.Boundary;
import harvard.robobees.simbeeotic.model.HWILBee;
import harvard.robobees.simbeeotic.model.HeliBehavior;
import harvard.robobees.simbeeotic.model.BaseHeliBehavior;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.LQIHeliBehavior;
import harvard.robobees.simbeeotic.model.Platform;
import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.Timer;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.util.PIDController;

import org.apache.log4j.Logger;

import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * Demonstration behavior for testbed helicopters.
 * Helicopter lifts off and completes a sine wave pattern to test
 * frequency response in the closed loop LQI system.
 * 
 * @author dpalmer
 */
public class HeliSine extends LQIHeliBehavior implements HeliBehavior
{
	private Timer timer;
	
	// parameters of sine wave
	private float frequency = 0.5f;
	private float amplitude = 1f;
	
	// state
	private float timeElapsed;
	private long t0;
	
	private static Logger logger = Logger.getLogger(HeliSine.class);

	/**
     * Helicopter completes a sine wave.
     *
     * @param platform The platform upon which the behavior is executing.
     * @param control The control interface for the helicopter.
     */
    public void start(final Platform platform, final HeliControl control, final Boundary bounds)
	{
    	// start low-level control
    	super.start(platform, control, bounds);
    	
    	t0 = System.currentTimeMillis();
    	
		timer = platform.createTimer(
			new TimerCallback()
			{
				public void fire(SimTime time)
				{
					// get time
					timeElapsed = (System.currentTimeMillis() - t0) / 1000.0f;
					
					// calculate reference signal
					setR(amplitude * Math.sin(timeElapsed * 2 * Math.PI * frequency));
					
					// increase frequency
					frequency *= 1.0005;
				}
			}, 2, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
	}
	
	/**
     * Stops the behavior.
     */
    public void stop()
	{
		timer.cancel();
		super.stop();
	}
}