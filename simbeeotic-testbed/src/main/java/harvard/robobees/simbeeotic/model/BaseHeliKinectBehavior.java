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


import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimEngine;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.util.PIDController;
import org.apache.log4j.Logger;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * A base class that implements a simple movement abstraction on top of the
 * raw helicopter control. The abstraction provides a mechanism to move to an
 * arbitrary point in 3D space, reorient in place, and hover in place.
 *
 * @author bkate
 */
public abstract class BaseHeliKinectBehavior implements HeliBehavior {

    protected SimEngine simEngine;
    
    // state
    private Timer controlTimer;
    private Vector3f lastPos = new Vector3f();
    private Quat4f lastPose = new Quat4f();
    private long lastTime = 0;
    private MoveState currState = MoveState.IDLE;
    private Vector3f currTarget = new Vector3f();
    private BufferedWriter logWriter;

    private HeliControl control;
    private Platform platform;
    private PositionSensor posSensor;
    private PoseSensor orientSensor;

    // controllers and set points
    private PIDController throttlePID;
    private PIDController pitchPID;
    private PIDController rollPID;
    private double yawSetpoint = 0;          // radians
    private MoveCallback currMoveCallback;

    // data logging parameters
    private boolean logData = false;
    private String logPath = "./heli_log.txt";

    private enum MoveState {IDLE, TAKEOFF, HOVER, LAND}

    private static Logger logger = Logger.getLogger(BaseHeliKinectBehavior.class);

    private static final long CONTROL_LOOP_PERIOD = 10;           // ms (50 Hz)
    private static final float LANDING_ALTITUDE = 0.1f;           // m, below which we can drop



    @Override
    public void start(final Platform platform, final HeliControl control, final Boundary bounds) {

        this.platform = platform;
        this.control = control;

        if (logData) {

            try {
                logWriter = new BufferedWriter(new FileWriter(logPath));
            }
            catch(IOException e) {
                throw new RuntimeException("Could not open log output file: " + logPath, e);
            }
        }

        SimpleHive hive = simEngine.findModelByType(SimpleHive.class);

        if (hive == null) {
            throw new RuntimeException("A hive must be present and initialized before any helicopters.");
        }

        posSensor = platform.getSensor("position-sensor", PositionSensor.class);
        orientSensor = platform.getSensor("pose-sensor", PoseSensor.class);

        if (posSensor == null) {
            throw new RuntimeModelingException("A position sensor is needed for BaseHeliBehavior.");
        }

        if (orientSensor == null) {
            throw new RuntimeModelingException("A pose sensor is needed for the BaseHeliBehavior.");
        }

        throttlePID = new PIDController(1, 0.4, 1e-2, 0.1);
        pitchPID = new PIDController(0, 0.4, 1e-2, 0.1);
        rollPID = new PIDController(0, 0.4, 1e-2, 0.1);

        // send an inital command to the heli to put in a neutral state
        control.setThrust(control.getThrust());
        control.setPitch(control.getPitchTrim());
        control.setRoll(control.getRollTrim());
        control.setYaw(control.getYawTrim());


        // a timer that implements the low level control of the heli altitude and bearing
        controlTimer = platform.createTimer(new TimerCallback() {

            public void fire(SimTime time){
                
            	Vector3f pos = posSensor.getPosition();
                Quat4f pose = orientSensor.getPose();
                long realTime = System.currentTimeMillis();
                
                logModelData(lastPos, pos, lastPose, pose, (realTime - lastTime) / 1000.0f);
                
                lastPos = pos;
                lastPose = pose;
                lastTime = realTime;
                
                Vector3f euler = MathUtil.quaternionToEulerZYX(pose);
                
            	Vector3f disp = new Vector3f(pos);
            	disp.sub(currTarget);
                
                Transform orient = new Transform();
                orient.setIdentity();
                orient.setRotation(pose);

                Vector3f bodyX = new Vector3f(1, 0, 0);
                orient.transform(bodyX);
                
                Vector3f bodyY = new Vector3f(0, 1, 0);
                orient.transform(bodyY);

                showState();

                switch(currState) {

                    case IDLE:

                        if (control.getThrust() > 0.0) {
                            control.setThrust(0.0);
                        }

                        break;

                    case TAKEOFF:
                        
                        if (pos.z >= currTarget.z) {

                            MoveCallback tmp = null;

                            if (currMoveCallback != null) {

                                tmp = currMoveCallback;
                                currMoveCallback.reachedDestination();
                            }

                            if ((tmp != null) && (currMoveCallback != null) && (tmp.equals(currMoveCallback))) {
                                currMoveCallback = null;
                            }
                        }
                        break;

                    case LAND:

                        // throttle down to land
                        control.setThrust(control.getThrustTrim() - 0.05);

                        if (pos.z < LANDING_ALTITUDE) {
                            currState = MoveState.IDLE;
                        }
                        else {

                            // try to stay on target while landing
                            updateYaw(euler.z);
                            updatePitch(time.getTime(), disp.dot(bodyX));
                            updateRoll(time.getTime(), disp.dot(bodyY));
                        }

                        break;

                    case HOVER:

                        updateThrottle(time.getTime(), pos);
                        updateYaw(euler.z);
//                        updatePitch(time.getTime(), disp.dot(bodyX));
//                        updateRoll(time.getTime(), disp.dot(bodyY));
                        
                        break;

                    default:
                        // do nothing
                }
            }
        }, 0, TimeUnit.MILLISECONDS, CONTROL_LOOP_PERIOD, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop() {

        control.setThrust(0.0);

        if (logData) {

            try {
                logWriter.close();
            }
            catch (IOException e) {
                // do nothing
            }
        }
        
        controlTimer.cancel();
    }


    /**
     * A convenience method for taking off.
     *
     * @param z The altitude to reach after takeoff.
     *
     */
    protected void takeoff(final double z) {

        // center servos to make takeoff straighter
        control.setRoll(control.getRollTrim());
        control.setPitch(control.getPitchTrim());
        control.setYaw(control.getYawTrim());
        
        // just add some juice and go
        currTarget.z = (float)z;
        currState = MoveState.TAKEOFF;

        currMoveCallback = new MoveCallback() {

            @Override
            public void reachedDestination() {
                hover(z);
            }
        };

        control.setThrust(0.6);
    }
    
    
    protected void takeoff(double z, MoveCallback callback) {
        
        currMoveCallback = callback;
        hover(z);
    }


    /**
     * Lands the helicopter at the current position.
     */
    protected void land() {

    	currTarget = posSensor.getPosition();
    	yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
        currState = MoveState.LAND;
    }


    /**
     * Turns the helicopter counter-clockwise about the body Z axis (yaw).
     *
     * @param angle The angle to turn (in radians).
     */
    protected void turn(double angle) {
        yawSetpoint += angle;
    }


    /**
     * Indicates that the helicopter should hover at the current altitude setpoint.
     */
    protected void hover() {

    	currTarget = posSensor.getPosition();
    	yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
        throttlePID.setSetpoint(currTarget.z);
        currState = MoveState.HOVER;
    }


    /**
     * Indicates that the helicopter should hover at the given altitude.
     *
     * @param altitude The hover altitude (m).
     */
    protected void hover(double altitude) {

        hover();

        currTarget.z = (float)altitude;
    	yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
        throttlePID.setSetpoint(altitude);
    }


    /**
     * Indicates that the helicopter should land and idle until given another command.
     */
    protected void idle() {
        currState = MoveState.IDLE;
        control.setThrust(0.0);
    }


    private void updateThrottle(long time, Vector3f pos)
    {
        Double throttleDelta = throttlePID.update(time, pos.z);

        // pid update can return null
        if (throttleDelta == null)
            throttleDelta = 0.0;

        double newThrottle = control.getThrustTrim() + throttleDelta;

        // Make sure we don't exceed min and max throttle
        if (newThrottle > control.getThrustTrim() + 0.5)
            newThrottle = control.getThrustTrim() + 0.5;

        if (newThrottle < control.getThrustTrim() - 0.1)
            newThrottle = control.getThrustTrim() - 0.1;

        control.setThrust(newThrottle);
    }

    private void updatePitch(long time, double xDisp)
    {
        Double pitchDelta = pitchPID.update(time, xDisp);

        // pid update can return null
        if (pitchDelta == null)
            pitchDelta = 0.0;

        double newPitch = control.getPitchTrim() + pitchDelta;

        control.setPitch(newPitch);
    }

    private void updateRoll(long time, double yDisp)
    {
        Double rollDelta = rollPID.update(time, yDisp);

        // pid update can return null
        if (rollDelta == null)
            rollDelta = 0.0;

        double newRoll = control.getRollTrim() + rollDelta;

        control.setRoll(newRoll);
    }

    private void updateYaw(double angle)
    {
        double yaw = control.getYawTrim() + (-0.3 * Math.sin(angle - yawSetpoint));

        if (yaw > 0.75) {
            yaw = 0.75;
        }

        if (yaw < 0.19) {
            yaw = 0.19;
        }

        control.setYaw(yaw);
    }


    private void showState() {

        if (logger.isDebugEnabled()) {

            logger.debug("State: " + currState + " Pos: " + posSensor.getPosition() +
                         " Target: " + currTarget + " Dist: " + getDistfromPosition3d(currTarget));
        }
    }

    private void logModelData(Vector3f pos1, Vector3f pos2, Quat4f pose1, Quat4f pose2, float dt) {

        if (logData) {

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

            int thrust = HWILBee.rawCommand(control.getThrust()) - HWILBee.rawCommand(control.getThrustTrim());
            int roll = HWILBee.rawCommand(control.getRoll()) - HWILBee.rawCommand(control.getRollTrim());
            int pitch = HWILBee.rawCommand(control.getPitch()) - HWILBee.rawCommand(control.getPitchTrim());
            int yaw = HWILBee.rawCommand(control.getYaw()) - HWILBee.rawCommand(control.getYawTrim());

            try {

                logWriter.write(dt + ", 0, " + thrust + ", " + roll +
                                ", " + pitch + ", " + yaw + ", " +
                                vel_x + ", " + vel_y + ", " + vel_z + ", " + dEuler.x +
                                ", " + dEuler.y + ", " + dEuler.z + "\n");
            }
            catch (IOException e) {
                // do nothing
            }
        }
    }


    private float getDistfromPosition3d(Vector3f value) {

        Vector3f pos = posSensor.getPosition();
        Vector3f temp = new Vector3f(value);
        temp.sub(pos);
        return(temp.length());
    }


    @Inject(optional = true)
    public final void setLogging(@Named("logging") final boolean logData) {
        this.logData = logData;
    }


    @Inject(optional = true)
    public final void setLogPath(@Named("log-path") final String logPath) {
        this.logPath = logPath;
    }


    @Inject
    public final void setSimEngine(@GlobalScope final SimEngine engine) {
        this.simEngine = engine;
    }


    /**
     * A callback that can be implemented by derived classes to be informed when the
     * heli has reached a desired destination.
     */
    protected static interface MoveCallback {

        public void reachedDestination();
    }
}