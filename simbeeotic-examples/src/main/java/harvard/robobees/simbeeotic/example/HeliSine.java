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