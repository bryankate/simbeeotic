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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.Boundary;
import harvard.robobees.simbeeotic.model.HeliBehavior;
import harvard.robobees.simbeeotic.model.HeliControl;
import harvard.robobees.simbeeotic.model.Platform;
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
 * Helicopter lifts off, completes a circle several times
 * and subsequently lands.
 * 
 * @author dpalmer
 */
public class HeliCircle implements HeliBehavior
{
	private PositionSensor sensor1;
	private PoseSensor sensor2;
	private Timer timer;
	
	// data logging parameters
    private boolean logData = false;
    private String logPath = "./heli_log.txt";
	
	// parameters of circle
	private Vector3f center;
	private float targetRadius = 1;
	private int revolutions = 1;
	
	// motion modes
	private enum State {TAKEOFF, HOVER, LAND, TOUCHDOWN};

	// current state
	private State state;
    private Vector3f lastPos = new Vector3f();
    private Quat4f lastPose = new Quat4f();
    private long lastTime = 0;
	private boolean xPositive;
	
	private PIDController thrustPID;
	private PIDController rollPID;
	
	private static Logger logger = Logger.getLogger(HeliCircle.class);
    FileWriter stream;
    BufferedWriter out;

    private void logModelData(HeliControl control, Vector3f pos1, Vector3f pos2, Quat4f pose1, Quat4f pose2, float dt)
    {
    	if (logData)
    	{
	    	Vector3f vel = new Vector3f();
	    	vel.sub(pos2, pos1);
	    	vel.scale(1/dt);
	    	
	    	Transform orient = new Transform();
	    	orient.setIdentity();
	    	orient.setRotation(pose2);
	    	Vector3f x = new Vector3f(1, 0, 0);
	    	Vector3f y = new Vector3f(0, 1, 0);
	    	Vector3f z = new Vector3f(0, 0, 1);
	    	orient.transform(x);
	    	orient.transform(y);
	    	orient.transform(z);
	    	
	    	
	    	double vel_x = vel.dot(x);
	    	double vel_y = vel.dot(y);
	    	double vel_z = vel.dot(z);
	    	
	    	Quat4f dQ = new Quat4f();
	    	dQ.mulInverse(pose2, pose1);
	    	
	    	Vector3f dEuler = MathUtil.quaternionToEulerZYX(dQ);
	    	dEuler.scale(1/dt);
	    	
	    	Vector3f dEulerApprox = new Vector3f(dQ.x, dQ.y, dQ.z);
	    	dEulerApprox.scale(2/dt);
	    	
	    	int thrust = simToHeli(control.getThrust()) - simToHeli(control.getThrustTrim());
	    	int roll = simToHeli(control.getRoll()) - simToHeli(control.getRollTrim());
	    	int pitch = simToHeli(control.getPitch()) - simToHeli(control.getPitchTrim());
	    	int yaw = simToHeli(control.getYaw()) - simToHeli(control.getYawTrim());
	    	
	    	try
	    	{
	    		out.write(dt + ", 0, " + thrust + ", " + roll +
	    				  ", " + pitch + ", " + yaw + ", " +
	    				  vel_x + ", " + vel_y + ", " + vel_z + ", " + dEuler.x +
	    				  ", " + dEuler.y + ", " + dEuler.z + "\n");
	    	}
	    	catch (IOException e)
	    	{
	    		// do nothing
	    	}
    	}
    }
    
    /**
     * Convert a sim heli command value (0.0 - 1.0) to a raw heli command (170 - 852)
     * @param value sim heli command
     * @return raw heli command
     */
    private int simToHeli(double value) {
        return (170 + (int)round((value * 682)));
    }
	
	/**
     * Helicopter completes a circular route.
     *
     * @param platform The platform upon which the behavior is executing.
     * @param control The control interface for the helicopter.
     */
    public void start(final Platform platform, final HeliControl control, final Boundary bounds)
	{
    	// start file i/o
    	try
    	{
    		stream = new FileWriter(logPath);
    		out = new BufferedWriter(stream);
    	}
    	catch(IOException e)
    	{
    		// do nothing
    	}
    	
    	// connect to sensors
		sensor1 = platform.getSensor("position-sensor", PositionSensor.class);
		sensor2 = platform.getSensor("pose-sensor", PoseSensor.class);
		
		// set center of circle
		center = new Vector3f(0, 0, 1);
		logger.info("Center: " + center + ".");
		
		/* initialize PID controllers with appropriate
		 * gains -- behavior is sensitive to these values
		 */
		thrustPID = new PIDController(center.z, 0.4, 1e-2, 0.1);
		rollPID = new PIDController(targetRadius, -0.4, -1e-2, -0.1);
		
		// send initial neutral commands to the heli to initialize takeoff
        control.setThrust(control.getThrustTrim());
        control.setPitch(control.getPitchTrim());
        control.setRoll(control.getRollTrim());
        control.setYaw(control.getYawTrim());
		
		logger.info("Taking off!");
		takeoff();
		
		timer = platform.createTimer(
			new TimerCallback()
			{
				public void fire(SimTime time)
				{
					// get sensor data
					Vector3f pos = sensor1.getPosition();
					Quat4f rot = sensor2.getPose();
					long realTime = System.currentTimeMillis();
					
	                // log data for use in Ben's model
	                logModelData(control, lastPos, pos, lastPose, rot, (realTime - lastTime) / 1000.0f);
	                lastPos = pos;
	                lastPose = rot;
	                lastTime = realTime;
					
					// calculate current radius (distance from z-axis)
					double radius = Math.sqrt(pos.x * pos.x + pos.y * pos.y);
					
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
					
					/* use pose information to calculate body frame y-axis
					 * this uses quaternion multiplication to get the image
					 * of the vector (0, 1, 0) under the body rotation
					 */
					Quat4f headQ = new Quat4f(rot);
					headQ.mul(new Quat4f(0, 1, 0, 0));
					headQ.mulInverse(rot);
					
					/* angle between body-frame y-axis and radius vector
					 * (these should be parallel)
					 */
					double alpha = (Math.atan2(headQ.y, headQ.x) -
									Math.atan2(pos.y, pos.x)) % (2 * Math.PI);
					
					// update commands
					updateThrust(control, time.getTime(), pos.z);
					updateYaw(control, alpha);
					updatePitch(control);
					updateRoll(control, time.getTime(), radius);
				}
			}, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
	}
	
	/**
     * Stops the behavior.
     */
    public void stop()
	{
		timer.cancel();
	}
	
	private void updateThrust(final HeliControl control, long t, float z)
	{
		if(state == State.LAND && z < 0.05)
		{
			// stop motors when heli is close to ground
			state = State.TOUCHDOWN;
			timer.cancel();
		}
		else if(state == State.TAKEOFF && z > center.z - 0.05 && z < center.z + 0.05)
		{
			state = State.HOVER;
			logger.info("\nSwitching to HOVER mode.");
		}
		
		Double thrust = control.getThrustTrim();
		Double delta = 0.0;
		
		switch(state)
		{
		case TAKEOFF:
			delta = thrustPID.update(t, z);
			break;
		case HOVER:
			delta = thrustPID.update(t, z);
			break;
		case LAND:
			thrust = control.getThrustTrim() - 0.05;
			break;
		case TOUCHDOWN:
			thrust = 0.0;
			break;
		default:
			break;
		}
		
		if(delta == null)
			delta = 0.0;
		
		thrust += delta;
		
		// thrust may not vary too widely in hover mode
		if (state == State.HOVER && thrust > control.getThrustTrim() + 0.25)
            thrust = control.getThrustTrim() + 0.25;
		if (state == State.HOVER && thrust < control.getThrustTrim() - 0.1)
			thrust = control.getThrustTrim() - 0.1;
        
		control.setThrust(thrust);
	}

	private void updatePitch(final HeliControl control)
	{
		if(state == State.HOVER)
		{
			/* maintain a constant pitch
			 * (this method is not really necessary, but is maintained
			 * in case we want to add more interesting pitch control)
			 */
			control.setPitch(control.getPitchTrim() + 0.3);
		}
	}
	
	private void updateYaw(final HeliControl control, double a)
	{
		if(state == State.HOVER)
		{
			// adjust yaw using proportional control
			control.setYaw(control.getYawTrim() + 0.3 * Math.sin(a));
		}
	}
	
	private void updateRoll(final HeliControl control, long t, double r)
	{
		if(state == State.HOVER)
		{
			// adjust roll to maintain a constant radius, using PID controller
			Double delta = rollPID.update(t, r);
			if (delta == null)
				delta = 0.0;
			
			control.setRoll(control.getRollTrim() + delta);
		}
	}

	private void takeoff()
	{
		state = State.TAKEOFF;
	}
	
	private void land(HeliControl control)
	{
		// reset all commands before initiating landing sequence
		control.setPitch(control.getThrustTrim());
        control.setRoll(control.getRollTrim());
        control.setYaw(control.getYawTrim());
		state = State.LAND;
	}
	

    @Inject(optional = true)
    public final void setRevolutions(@Named("revolutions") final int revolutions) {
        this.revolutions = revolutions;
    }
    
    @Inject(optional = true)
    public final void setRadius(@Named("radius") final float radius) {
        this.targetRadius = radius;
    }
    
    @Inject(optional = true)
    public final void setLogging(@Named("logging") final boolean logData) {
        this.logData = logData;
    }
    
    @Inject(optional = true)
    public final void setLogPath(@Named("log-path") final String logPath) {
        this.logPath = logPath;
    }
}