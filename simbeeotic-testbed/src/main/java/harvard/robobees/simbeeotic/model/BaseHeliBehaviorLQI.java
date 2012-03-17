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
package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.util.PIDController;

import org.apache.log4j.Logger;

import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * A base class that implements a global movement abstraction on top of an optimal
 * body-frame model-based controller. The abstraction implements motion to an
 * arbitrary point in 3D space, turning, hovering, and landing.
 *
 * @author dpalmer
 */
public abstract class BaseHeliBehaviorLQI extends LQIHeliBehavior implements HeliBehavior
{
	// sensors
	private PositionSensor s1;
	private PoseSensor s2;
	
    private enum MoveState {IDLE, HOVER, MOVE, LAND}
    
    // parameters
    private float landHeight = 0.3f; // in meters
    private float landRadius = 0.08f; // in meters
    
    // state
    private MoveState currState = MoveState.IDLE;
    private Vector3f currTarget = new Vector3f();
    private Vector3f lastTerminus = new Vector3f();
    private float currEpsilon = 0.1f;   // in meters
    private MoveCallback currMoveCallback;
    private Vector3f hiveLocation = new Vector3f(0, 0, 1); // where this helicopter should land

    // control and set points
    private Timer planTimer;
    private PIDController uPID;
    private PIDController vPID;
    private PIDController wPID;
    private double yawSetpoint = 0;  // radians
    
    // data collection and logging
    private static Logger logger = Logger.getLogger(BaseHeliBehaviorLQI.class);
    private double routeError = 0;
    private int numSteps = 0;
    

    private float getDistfromPosition2d(Vector3f value)
    {
        Vector3f pos = s1.getPosition();
        Vector3f temp = new Vector3f(value);
        temp.sub(pos);
        return((float)Math.hypot(temp.x, temp.y));

    }


    private float getDistfromPosition3d(Vector3f value)
    {
        Vector3f pos = s1.getPosition();
        Vector3f temp = new Vector3f(value);
        temp.sub(pos);
        return(temp.length());
    }

    private void updateU(long time, double dx)
    {
    	Double u = uPID.update(time, dx);
        
        // PID update can return null
        if (u == null)
            u = 0.0;
        
        setU(u);
    }
    
    private void updateV(long time, double dy)
    {
    	Double v = vPID.update(time, dy);
        
        // PID update can return null
        if (v == null)
            v = 0.0;
        
        setV(v);
    }

    private void updateW(long time, Vector3f pos)
    {
        Double w = wPID.update(time, pos.z);
        
        // PID update can return null
        if (w == null)
            w = 0.0;
        
        setW(w);
    }

    private void updateR(Vector3f heading)
    {
    	setR(2 * Math.atan2(-heading.y, -heading.x));
    }

    protected void landHeli() {

        moveToPoint(hiveLocation.x, hiveLocation.y, hiveLocation.z, 0.1f,
                    new MoveCallback() {

                        @Override
                        public void reachedDestination() {
                            land();
                        }
                    });

    }


    protected void landHeli(final MoveCallback callback) {

        moveToPoint(hiveLocation.x, hiveLocation.y, hiveLocation.z, 0.1f,
                    new MoveCallback() {

                        @Override
                        public void reachedDestination() {
                            
                            land();
                            callback.reachedDestination();
                        }
                    });

    }

    @Override
    public void start(final Platform platform, final HeliControl control, final Boundary bounds)
    {
    	// start low-level control
    	super.start(platform, control, bounds);
    	
    	// initialize sensors
    	s1 = platform.getSensor("position-sensor", PositionSensor.class);
        s2 = platform.getSensor("pose-sensor", PoseSensor.class);
        if (s1 == null)
            throw new RuntimeModelingException("A position sensor is needed for BaseHeliBehaviorLQI.");
        if (s2 == null)
            throw new RuntimeModelingException("A pose sensor is needed for BaseHeliBehaviorLQI.");

        // initialize controllers
        uPID = new PIDController(0.0, 0.3, 0.0, 0.0);
        vPID = new PIDController(0.0, 0.1, 0.0, 0.0);
        wPID = new PIDController(1.0, 0.5, 0.0, 0.0);

        // store starting point
        lastTerminus = s1.getPosition();
        
        // a timer that implements high level control given a body-frame
        // motion abstraction
        planTimer = platform.createTimer(new TimerCallback()
        {
            public void fire(SimTime time)
            {
            	Vector3f pos = s1.getPosition();
            	Quat4f pose = s2.getPose();
                
            	// distances between current position, target, and last terminus
            	double r0 = getDistfromPosition2d(lastTerminus);
            	double r1 = getDistfromPosition2d(currTarget);
            	double d = Math.hypot(currTarget.x - lastTerminus.x, currTarget.y - lastTerminus.y);
            	
            	// use Heron's formula to calculate square distance from optimal (straight-line) path
            	// and integrate
            	if (d > 0)
            	{
            		double s = (r0 + r1 + d) / 2; // semiperimeter
            		double sqerr = (4 / (d * d)) * (s * (s - r0) * (s - r1) * (s - d));
            		numSteps++;
            		routeError += sqerr;
            	}
            	
            	// log rms tracking error (standard deviation)
            	logger.info("Route tracking error: " + getTrackingError());
            	
            	// displacement from target
            	Vector3f disp = new Vector3f(pos);
            	disp.sub(currTarget);
            	
            	// put displacement from target in body-frame coordinates
            	pose.inverse();
            	Transform orient = new Transform();
            	orient.setIdentity();
            	orient.setRotation(pose);
            	orient.transform(disp);

                switch(currState)
                {
                    case IDLE:
                        if (control.getThrust() > 0.0)
                            control.setThrust(0.0);
                        break;

                    case LAND:
                    	if (pos.z < landHeight && getDistfromPosition2d(currTarget) < landRadius)
                    		idle();
                    	else
                    		updateU(time.getTime(), disp.x);
                    		updateV(time.getTime(), disp.y);
                    		updateR(disp);
                        break;

                    case MOVE:
                        double dist = getDistfromPosition3d(currTarget);

                        if (dist <= currEpsilon) {
                            
                            // made it! go to the hovering state
                            hover();

                            MoveCallback tmp = null;

                            if (currMoveCallback != null) {

                                tmp = currMoveCallback;
                                currMoveCallback.reachedDestination();
                            }

                            if ((tmp != null) && (currMoveCallback != null) && (tmp.equals(currMoveCallback))) {
                                currMoveCallback = null;
                            }

                            // we reached the destination, so break out of this iteration of the loop
                            break;
                        }

                        updateU(time.getTime(), disp.x);
                        updateV(time.getTime(), disp.y);
                        updateW(time.getTime(), pos);
                        updateR(disp);
                        break;

                    case HOVER:
                    	// correct drift
                    	updateU(time.getTime(), disp.x);
                    	updateV(time.getTime(), disp.y);
                    	updateW(time.getTime(), pos);
                        break;

                    default:
                        // do nothing
                }
            }
        }, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop()
    {
    	planTimer.cancel();
        super.stop();
    }

    public double getTrackingError()
    {
    	if (numSteps > 0)
    		return Math.sqrt(routeError / numSteps);
    	else
    		return 0;
    }
    
    
    private void setTarget(Vector3f target)
    {
    	// store terminus
    	lastTerminus = s1.getPosition();
    	
    	currTarget = target;
    	if (wPID != null)
    		wPID.setSetpoint(currTarget.z);
    	appendLoggingData(target.x + ", " + target.y + ", " + target.z);
    }
    
    /**
     * Moves the helicopter to a point in space.
     *
     * @param x The coordinate in the global X axis (m).
     * @param y The coordinate in the global Y axis (m).
     * @param z The coordinate in the global Z axis (m).
     * @param epsilon The radius around the desired point that is considered acceptable (m).
     */
    protected void moveToPoint(float x, float y, float z, float epsilon) {
        moveToPoint(x, y, z, epsilon, null);
    }


    /**
     * Moves the helicopter to a point in space with a callback.
     *
     * @param x The coordinate in the global X axis (m).
     * @param y The coordinate in the global Y axis (m).
     * @param z The coordinate in the global Z axis (m).
     * @param epsilon The radius around the desired point that is considered acceptable (m).
     * @param callback An optional callback to be executed once the helicopter reaches the specified point.
     */
    protected void moveToPoint(float x, float y, float z, float epsilon, MoveCallback callback) {

        currState = MoveState.MOVE;
        setU(1);
        setTarget(new Vector3f(x, y, z));
        currEpsilon = epsilon;
        currMoveCallback = callback;
    }


    protected void land()
    {
    	setU(0);
    	setV(0);
    	setW(-0.1);
    	setP(0);
    	setQ(0);
    	setR(0);
        currState = MoveState.LAND;
    }

    /**
     * Turns the helicopter counter-clockwise about the Z axis.
     *
     * @param angle The angle to turn (in radians).
     */
    protected void turn(double angle) {
        yawSetpoint += angle;
    }


    /**
     * Indicates that the helicopter should hover at its current position.
     */
    protected void hover()
    {
    	setTarget(s1.getPosition());
    	setU(0);
    	setV(0);
    	setW(0);
    	setP(0);
    	setQ(0);
    	setR(0);
        currState = MoveState.HOVER;
    }


    /**
     * Indicates that the helicopter should land and idle until given another command.
     */
    protected void idle()
    {
    	super.stop();
        currState = MoveState.IDLE;
    }

    /**
     * A callback that can be implemented by derived classes to be informed when the
     * helicopter has reached a desired destination.
     */
    protected static interface MoveCallback {

        public void reachedDestination();
    }
    
    @Inject(optional = true)
    public final void setLandHeight(@Named("land-height") final int landHeight) {
        this.landHeight = landHeight;
    }
    
    @Inject(optional = true)
    public final void setLandRadius(@Named("land-radius") final int landRadius) {
        this.landRadius = landRadius;
    }
}
