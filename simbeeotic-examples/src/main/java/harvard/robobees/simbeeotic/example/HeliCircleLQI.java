/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package harvard.robobees.simbeeotic.example;


import static java.lang.Math.round;

import java.util.concurrent.TimeUnit;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.Boundary;
import harvard.robobees.simbeeotic.model.HeliBehavior;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.LQIHeliBehavior;
import harvard.robobees.simbeeotic.model.Platform;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.model.Timer;
import harvard.robobees.simbeeotic.util.PIDController;

import org.apache.log4j.Logger;

import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * Demonstration behavior for testbed helicopters.
 * Helicopter lifts off, completes a circle several times
 * and subsequently lands.
 * 
 * @author dpalmer
 */
public class HeliCircleLQI extends LQIHeliBehavior implements HeliBehavior
{
	private PositionSensor s1;
	private PoseSensor s2;
	private Timer timer;
	
	// parameters of circle
	private Vector3f center;
	private float targetRadius = 1;
	private int revolutions = 4;
	
	// motion modes
	private enum State {TAKEOFF, HOVER, LAND, TOUCHDOWN};

	// current state
	private State state;
	private boolean xPositive;

	private PIDController vPID;
	private PIDController wPID;
    
	private static Logger logger = Logger.getLogger(HeliCircleLQI.class);

	/**
     * Helicopter completes a circular route.
     *
     * @param platform The platform upon which the behavior is executing.
     * @param control The control interface for the helicopter.
     */
    public void start(final Platform platform, final HeliControl control, final Boundary bounds)
	{
    	// start low-level control
    	super.start(platform, control, bounds);
    	
    	// connect to sensors
		s1 = platform.getSensor("position-sensor", PositionSensor.class);
		s2 = platform.getSensor("pose-sensor", PoseSensor.class);
		
		// set center of circle
		center = new Vector3f(0, 0, 1);
		logger.info("Center: " + center + ".");
		
		/* initialize PID controllers with appropriate
		 * gains -- behavior is sensitive to these values
		 */
		vPID = new PIDController(targetRadius, -0.4, 0, 0.01);
		wPID = new PIDController(center.z, 0.5, 0, 0);
		
		// initialize takeoff
		logger.info("Taking off!");
		takeoff();
		
		timer = platform.createTimer(
			new TimerCallback()
			{
				public void fire(SimTime time)
				{
					// get sensor data
					Vector3f pos = s1.getPosition();
					Quat4f pose = s2.getPose();
					
					// calculate current radius (distance from z-axis)
					double radius = Math.hypot(pos.x, pos.y);
					
					if(state == State.HOVER)
						logger.info("Current radius: " + radius + " m.");
					
					// record sign changes in x-coordinate to count revolutions
					if (state == State.HOVER && !xPositive && pos.x > 0)
					{
						xPositive = true;
						revolutions--;
						
						if (revolutions < 0)
						{
							logger.info("Landing!");
							land(control);
						}
					}
					else if (state == State.HOVER && xPositive && pos.x < 0)
						xPositive = false;
					
					/* use pose information to calculate body frame r:
					 * this uses quaternion multiplication to get the radius
					 * vector in body-frame coordinates
					 */
					pose.inverse();
					Vector3f r = new Vector3f(pos);
					r.negate();
					r.normalize();
					Transform orient = new Transform();
					orient.setIdentity();
					orient.setRotation(pose);
					orient.transform(r);
					
					/* angle between body-frame y-axis and radius vector
					 * (these should be parallel)
					 */
					//double alpha = (Math.atan2(r.y, r.x) - Math.PI / 2) % (2 * Math.PI);
					double alpha = r.x;
					
					// update commands
					updateU();
					updateV(time.getTime(), radius);
					updateW(time.getTime(), pos.z);
					updateR(alpha);
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

	private void takeoff()
	{
		state = State.TAKEOFF;
	}
	
	private void land(HeliControl control)
	{
		// reset all commands before initiating landing sequence
		setU(0);
		setV(0);
		setW(-0.5);
		setP(0);
		setQ(0);
		setR(0);
		state = State.LAND;
	}

	private void updateU()
	{
		if(state == State.HOVER)
		{
			/* maintain a constant forward velocity
			 * (this method is not really necessary, but is maintained
			 * in case we want to add more interesting pitch control)
			 */
			setU(0.5);
		}
	}
	
	private void updateV(long t, double r)
	{
		if(state == State.HOVER)
		{
			// move sideways to maintain a constant radius, using PID controller
			Double v = vPID.update(t, r);
			if (v == null)
				v = 0.0;
			
			setV(v);
		}
	}
	
	private void updateW(long t, float z)
	{
		if(state == State.LAND && z < 0.05)
		{
			// stop motors when heli is close to ground
			state = State.TOUCHDOWN;
			stop();
		}
		else if(state == State.TAKEOFF && z > center.z - 0.1 && z < center.z + 0.1)
		{
			state = State.HOVER;
			logger.info("\nSwitching to HOVER mode.");
		}
		
		Double w = 0.0;
		
		switch(state)
		{
		case TAKEOFF:
		case HOVER:
			w = wPID.update(t, z);
			break;
		case LAND:
			w = -0.2;
			break;
		case TOUCHDOWN:
			super.stop();
			break;
		default:
			break;
		}
		
		if(w == null)
			w = 0.0;
        
		setW(w);
	}
	
	private void updateR(double a)
	{
		if(state == State.HOVER)
		{
			// adjust yaw using proportional control
			setR(-5 * a);
		}
	}
    
    @Inject(optional = true)
    public final void setRevolutions(@Named("revolutions") final int revolutions) {
        this.revolutions = revolutions;
    }
    
    @Inject(optional = true)
    public final void setRadius(@Named("radius") final float radius) {
        this.targetRadius = radius;
    }
}