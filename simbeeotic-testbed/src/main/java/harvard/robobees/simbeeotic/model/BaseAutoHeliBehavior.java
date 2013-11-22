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


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimEngine;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.util.MedianPIDController;
import org.apache.log4j.Logger;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A base class that implements a simple movement abstraction on top of the
 * raw helicopter control. The abstraction provides a mechanism to move to an
 * arbitrary point in 3D space, reorient in place, and hover in place.
 *
 * @author bkate
 * @author kar
 */
public abstract class BaseAutoHeliBehavior implements HeliBehavior {

    protected SimEngine simEngine;
    
    // state
    private Timer controlTimer;
    private Vector3f lastPos = new Vector3f();
    private Quat4f lastPose = new Quat4f();
    private long lastTime = 0;
    private MoveState currState = MoveState.IDLE;
    private MoveState nextState = MoveState.IDLE;
    private Vector3f currTarget = new Vector3f();
    private double currEpsilon = 0.1;        // in meters
    private MoveCallback currMoveCallback;
    private List<AbstractHeli> allHelis;
    private Vector3f calcTarget;
    private Vector3f rVec;                   // repulsive vector for obst. avoidance
    private int myHeliId;
    private Vector3f landingSpot;            // where this helicopter should land
    private Vector3f hiveLocation;           // the center of the hive
    private double hiveRadius = 0.55;        // in meters; calculated later
    private BufferedWriter logWriter;

    private HeliControl control;
    private Platform platform;
    private PositionSensor posSensor;
    private PoseSensor orientSensor;
    private HeliDataStruct heliData;

    // controllers and set points
    private MedianPIDController throttlePID, pitchPID, rollPID, yawPID, flowRollPID, flowPitchPID;
    private double[] yawDiffs;
    private int yawHistPtr;
    private double currYaw = 0, prevYaw = 0, idYaw = 0, fHeading = 0;
    private boolean goodOrientation = false;
    private double prevRoll = -999, prevPitch = -999;
    private double controlMaxDelta = 0.1;
    private Vector3f beginPathPos = new Vector3f(0,0,1);
    private int landCntrMax = 25;
    private int landCntr = landCntrMax;

    private double crossTrackError = 0;
    private double pathAngle = 0;
    private double rollError = 0;
    private double targetBX = 0, targetBY = 0;

    private double pitchSetPoint = 0, yawSetpoint = 0.0;  // radians

    private int currStateInt = 0;

    private Vector3f takeoff_currTarget = new Vector3f();
    private double takeoff_yawSetpoint = 0.0;

    int heli_auto;
    double heli_lflow, heli_rflow, heli_phase, heli_speed, heli_heading;

    private int shutdown_cnt = 0;

    // data logging parameters
    private boolean logData = true;
    private String logPath = "./heli_log.txt";

    private enum MoveState {IDLE, TAKEOFF, HOVER, RUN, MOVE, LAND, TERMINATED}

    private static Logger logger = Logger.getLogger(BaseAutoHeliBehavior.class);

//    private static final long CONTROL_LOOP_PERIOD = 10;           // ms (50 Hz)
//    private static final float COLLISION_BUFF = 1.0f;             // m
//    private static final float COLLISION_BUFF_HIVE = 0.4f;        // m
//    private static final float BOUNDARY_BUFF = 0.5f;              // m, within which we will avoid walls
//    private static final float DESTINATION_EPSILON = 0.3f;        // m
//    private static final float SLOWDOWN_DISTANCE = 0.8f;          // m
//    private static final float FLYING_ALTITUDE = 0.1f;            // m, below which we do not consider obstacles
    private static final float LANDING_EPSILON = 0.3f;            // m
    private static final float LANDING_ALTITUDE = 0.1f;          // m, below which we can drop
    private static final float LANDING_STAGING_ALTITUDE = 0.5f;   // m
//    private static final long LANDING_STAGING_TIME = 1;           // s
    private static final float TAKEOFF_ALTITUDE = 0.25f;
    private static final float TURNING_CIRCLE_RADIUS = 0.5f;        // don't update heading when inside this distance to target

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

        allHelis = simEngine.findModelsByType(AbstractHeli.class);
        myHeliId = control.getHeliId();

        SimpleHive hive = simEngine.findModelByType(SimpleHive.class);

        if (hive == null) {
            throw new RuntimeException("A hive must be present and initialized before any helicopters.");
        }

        hiveLocation = hive.getTruthPosition();
        hiveRadius = hive.getSize() / 2;
        landingSpot = calcHiveLocation();

        posSensor = platform.getSensor("position-sensor", PositionSensor.class);
        orientSensor = platform.getSensor("pose-sensor", PoseSensor.class);
        heliData = control.receiveData();   // init heliData

        if (posSensor == null) {
            throw new RuntimeModelingException("A position sensor is needed for BaseAutoHeliBehavior.");
        }

        if (orientSensor == null) {
            throw new RuntimeModelingException("A pose sensor is needed for the BaseAutoHeliBehavior.");
        }

        throttlePID = new MedianPIDController(0.0, 0.4, 0.4, 0.4, 1.0, 0.75, 1.0, false);
//        pitchPID = new MedianPIDController(0.0, 0.7, 0.15, 0.3, 0.5, 1.0, 1.0, true);
        pitchPID = new MedianPIDController(0.0, 0.7, 0.15, 0.5, 1.0, 1.0, 1.0, false);
//        rollPID = new MedianPIDController(0.0, 0.6, 0.15, 0.3, 0.5, 1.0, 1.0, true);
        rollPID = new MedianPIDController(0.0, 0.7, 0.15, 0.5, 1.0, 1.0, 1.0, false);
        yawPID = new MedianPIDController(0.0, 0.25, 0.0, 0.0, 1.0, 1.0, 1.0, false);
//        flowRollPID = new MedianPIDController(0.1, 3.0, 0.25, 1.5, 0.25, 1.0, 1.0, true);     // 1 wall roll control
        flowRollPID = new MedianPIDController(0.0, 2.0, 0.05, 0.0, 0.25, 1.0, 1.0, true);
        flowPitchPID = new MedianPIDController(0.2, 0.0, 2.0, 0.5, 0.4, 1.0, 1.0, true);

        // send an inital command to the heli to put in a neutral state
        control.setThrust(0);
        control.setPitch(control.getPitchTrim());
        control.setRoll(control.getRollTrim());
        control.setYaw(control.getYawTrim());
        ((AutoHeliBee)control).disableHeliAutoAll();    // no onboard control
        control.sendCommands();

        // for heading filtering
        yawDiffs = new double[3];
        yawHistPtr = 0;

        // a timer that implements the low level control of the heli altitude and bearing
        controlTimer = new Timer();

        final long firstTime = System.currentTimeMillis();

        controlTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run(){
                
            	Vector3f pos = posSensor.getPosition();
                Quat4f pose = orientSensor.getPose();

                Vector3f euler = MathUtil.quaternionToEulerZYX(pose);

                goodOrientation = ( (Math.abs(euler.z) > 0.001) && ((Math.PI - Math.abs(euler.z)) > 0.001) );

                while( !goodOrientation ) {
                    pose = orientSensor.getPose();
                    euler = MathUtil.quaternionToEulerZYX(pose);
                    goodOrientation = ( (Math.abs(euler.z) > 0.001) && ((Math.PI - Math.abs(euler.z)) > 0.001) );

//                    System.out.println("Dropping orientation!");
                }

                // initialise heading to VICON
                if( lastTime == 0 ) {
                    idYaw = euler.getZ();
                }

                long realTime = System.currentTimeMillis();

                if((realTime - lastTime) == 0)
                    return;

                float dt = (realTime - lastTime) / 1000.0f;

                // emergency shutdown
                if( (Math.abs(pos.x + 1*(pos.x-lastPos.x)/dt) > 2.35) || (Math.abs(pos.y + 1*(pos.y-lastPos.y)/dt) > 3.75) ) {
                    shutdown_cnt++;
                } else {
                    shutdown_cnt = 0;
                }
                if( shutdown_cnt >= 3 ) {
                    // terminate
                    stop();
                }

                logModelData(currState, lastPos, pos, lastPose, pose, (realTime - firstTime)/1000.0f, dt);
                
                lastPos = pos;
                lastPose = pose;
                lastTime = realTime;

                fHeading = filterHeading(euler.getZ());

//                System.out.println("Filtered heading: " + fHeading);

                // world frame cartesian position error
                double targetWX = currTarget.getX()-pos.getX();
                double targetWY = currTarget.getY()-pos.getY();

                // body frame cartesian posiion error
                targetBX = 1 * (targetWX * Math.cos(-fHeading) + targetWY * -Math.sin(-fHeading));
                targetBY = 1 * (targetWX * Math.sin(-fHeading) + targetWY * Math.cos(-fHeading));

                // take off specific
                // world frame cartesian position error
                double takeoff_targetWX = takeoff_currTarget.getX()-pos.getX();
                double takeoff_targetWY = takeoff_currTarget.getY()-pos.getY();

                // body frame cartesian posiion error
                double takeoff_targetBX = 1 * (takeoff_targetWX * Math.cos(-fHeading) + takeoff_targetWY * -Math.sin(-fHeading));
                double takeoff_targetBY = 1 * (takeoff_targetWX * Math.sin(-fHeading) + takeoff_targetWY * Math.cos(-fHeading));

                // path control - assume fast heading dynamics!
                // (x2-x1)*(y1-y0) - (x1-x0)*(y2-y1) / sqrt((x2-x1)^2 + (y2-y1)^2)
                double x0 = pos.getX(), y0 = pos.getY(), x1 = beginPathPos.getX(), y1 = beginPathPos.getY(), x2 = currTarget.getX(), y2 = currTarget.getY();
                double pathLength = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
                if( pathLength != 0 ) {
                    crossTrackError = ((x2-x1)*(y1-y0) - (x1-x0)*(y2-y1)) / pathLength;
                    pathAngle = Math.atan2(y2-y1,x2-x1);
                    rollError = Math.cos(fHeading - pathAngle) * crossTrackError;
                } else {
                    crossTrackError = 0;
                    pathAngle = 0;
                    rollError = targetBY;
                }

                showState();

                long time = System.nanoTime();
                double dist = getDistfromPosition3d(currTarget);
                double dist2d = getDistfromPosition2d(currTarget);

                // from heli
                heliData = control.receiveData();   // update heli data packet
                heli_auto = heliData.cntl[0];
                heli_lflow = heliData.cntl[1]/255.0;
                heli_rflow = heliData.cntl[2]/255.0;
                heli_phase = heliData.cntl[3]*360.0/256.0;
                heli_speed = (heliData.cntl[4]-127.5)/127.5;
                heli_heading = heliData.cntl[5]*360.0/255.0;

                // old control was (? / 8 / 127)

                System.out.println("Recv auto: " + heli_auto + "  lflow: " + heli_lflow + "  rflow: " + heli_rflow + "  phase: " + heli_phase + "  speed: " + heli_speed + "  heading: " + heli_heading);

                switch(currState) {

                    case TERMINATED:     // for safety
                        currStateInt = -1;
                        // no break
                    case IDLE:
                        currStateInt = 0;

                        ((AutoHeliBee)control).disableHeliAutoAll();
                        control.setThrust(0.0);
                        control.setRoll(control.getRollTrim());
                        control.setPitch(control.getPitchTrim());
                        control.setYaw(control.getYawTrim());
                        break;

                    case TAKEOFF:
                        currStateInt = 1;

                        ((AutoHeliBee)control).disableHeliAutoAll();
                        if( pos.z > TAKEOFF_ALTITUDE) {
                            currState = nextState;
                            nextState = MoveState.IDLE;
                        }
                        control.setThrust(1.0);
                        updateYaw(time, fHeading);
                        updatePitch(time, -takeoff_targetBX);
                        updateRoll(time, takeoff_targetBY);
                        break;

                    case LAND:
                        currStateInt = 2;

                        // throttle down to land
                        ((AutoHeliBee)control).disableHeliAutoAll();
                        currTarget.setZ(currTarget.getZ()-0.02f);
                        throttlePID.setSetpointSoft(currTarget.getZ());     // don't reset PID when updating setpoint

                    	if (pos.z < LANDING_ALTITUDE) {
                            currState = MoveState.IDLE;
                            if (currMoveCallback != null) {
                                currMoveCallback.reachedDestination();
                                currMoveCallback = null;
                            }
                        }
                    	else {
                            // try to stay on target while landing
                            updateThrottle(time, pos.z);
                            updateYaw(time, fHeading);
                            updatePitch(time, -targetBX);
                            updateRoll(time, targetBY);
                    	}
                        break;

                    case RUN:
                        currStateInt = 3;

                        if (pos.getY() > currTarget.getY()) {
                            if (currMoveCallback != null) {
                                currMoveCallback.reachedDestination();
                                currMoveCallback = null;
                            }
                        }

//                        if( dist2d > TURNING_CIRCLE_RADIUS ) {
//                            yawSetpoint = Math.atan2(targetWY, targetWX);
//                            //targetBY /= dist2d;
//                        }
                        yawSetpoint = pathAngle;

                        updateThrottle(time, pos.z);
//                        updateYaw(time, fHeading);
//                        updateRoll(time, rollError);    // vicon roll
//                        updatePitch(time, -targetBX);   // vicon pitch

//                        updatePitchFlow(time, fFlowDiff);       // flow pitch
//                        updateRollFlow(time, -fAbsFlowDiff);    // flow roll
//                        updateYawFlow(time, fScaledFlowSum);    // flow yaw
                        break;

                    case MOVE:
                        currStateInt = 4;

                        if (dist <= currEpsilon) {
                            if (currMoveCallback != null) {
                                currMoveCallback.reachedDestination();
                                currMoveCallback = null;
                            }
                        }

                        // compute desired heading
//                        if( dist2d > TURNING_CIRCLE_RADIUS ) {
//                            yawSetpoint = Math.atan2(targetWY, targetWX);
//                            //targetBY /= dist2d;
//                        }

                        yawSetpoint = pathAngle;

                        updateThrottle(time, pos.z);
                        updateYaw(time, fHeading);
                        updatePitch(time, -targetBX);
//                        updateRoll(time, targetBY);
                        updateRoll(time, rollError);
                        break;

                    case HOVER:
                        currStateInt = 5;

                        ((AutoHeliBee)control).disableHeliAutoAll();
                        updateThrottle(time, pos.z);
                        updateYaw(time, fHeading);
                        updatePitch(time, -targetBX);
                        updateRoll(time, targetBY);
                        break;

                    default:
                        currStateInt = 9;

                        // do nothing
                }
                control.sendCommands();     // send updated commands to heli
           }
        }, 0, 100);        // start with delay of 0 ms and run every 100 ms
    }

    @Override
    public void stop() {

        logger.warn("Heli-" + myHeliId + " is stopping...\n");

        if (logData) {
            try {
                logWriter.close();
            }
            catch (IOException e) {
                // do nothing
            }
        }

        // send last command to heli?
        ((AutoHeliBee)control).disableHeliAutoAll();
        control.setThrust(0.0);
        control.setRoll(control.getRollTrim());
        control.setPitch(control.getPitchTrim());
        control.setYaw(control.getYawTrim());
        control.sendCommands();

        controlTimer.cancel();
        currState = MoveState.TERMINATED;
    }

    
    /**
     * Moves the helicopter to a point in space.
     *
     * @param x The coordinate in the global X axis (m).
     * @param y The coordinate in the global Y axis (m).
     * @param z The coordinate in the global Z axis (m).
     * @param epsilon The radius around the desired point that is considered acceptable (m).
     */
    protected void moveToPoint(double x, double y, double z, double epsilon) {
        moveToPoint(x, y, z, epsilon, null);
    }


    /**
     * Moves the helicopter to a point in space.
     *
     * @param x The coordinate in the global X axis (m).
     * @param y The coordinate in the global Y axis (m).
     * @param z The coordinate in the global Z axis (m).
     * @param epsilon The radius around the desired point that is considered acceptable (m).
     * @param callback An optional callback to be executed once the helicopter reaches the specified point.
     */
    protected void moveToPoint(double x, double y, double z, double epsilon, MoveCallback callback) {
        if(currState == MoveState.TERMINATED)
            return;

        Vector3f pos = posSensor.getPosition();
        if( pos.getZ() < LANDING_ALTITUDE ) {
            takeoff_currTarget = posSensor.getPosition();  // take off where we are
            takeoff_yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
            currState = MoveState.TAKEOFF;
            nextState = MoveState.MOVE;
        } else {
            currState = MoveState.MOVE;
        }

        currTarget = new Vector3f((float)x, (float)y, (float)z);
        throttlePID.setSetpoint(z);
        rollPID.setSetpoint(0.0);
        pitchPID.setSetpoint(0.0);

        // compute path
        beginPathPos = new Vector3f(pos);

        currEpsilon = epsilon;
        currMoveCallback = callback;
    }

    protected void runToPoint(double x, double y, double z, double epsilon, MoveCallback callback) {
        if(currState == MoveState.TERMINATED)
            return;

        currState = MoveState.RUN;
        currTarget = new Vector3f((float)x, (float)y, (float)z);
        throttlePID.setSetpoint(z);
        rollPID.setSetpoint(0.0);
        pitchPID.setSetpoint(0.0);

        // compute path
        Vector3f pos = posSensor.getPosition();
        beginPathPos = new Vector3f(pos);

        currEpsilon = epsilon;
        currMoveCallback = callback;
    }

    /**
     * Lands the helicopter at the current position.
     */
    protected void land() {

    	currTarget = posSensor.getPosition();
//    	yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
        currState = MoveState.LAND;
        rollPID.setSetpoint(0.0);
        pitchPID.setSetpoint(0.0);
        throttlePID.setSetpoint(currTarget.getZ());
    }

    /**
     * Lands the helicopter at the hive.
     */
    protected void landAtHive() {

        moveToPoint(landingSpot.x, landingSpot.y, landingSpot.z, LANDING_EPSILON,
                    new MoveCallback() {

                        @Override
                        public void reachedDestination() {
                            land();
//                            hoverAtPoint(landingSpot);
//
//                            platform.createTimer(new TimerCallback() {
//
//                                @Override
//                                public void fire(SimTime time) {
//                                    landAtPoint(landingSpot);
//                                }
//                            }, LANDING_STAGING_TIME, TimeUnit.SECONDS);
                        }
                    });

    }


    /**
     * Lands the helicopter at the hive and informs the caller when the maneuver is complete.
     *
     * @param callback The callback to be invoked when the helicopter has landed.
     */
    protected void landAtHive(final MoveCallback callback) {

        moveToPoint(landingSpot.x, landingSpot.y, landingSpot.z, LANDING_EPSILON,
                    new MoveCallback() {

                        @Override
                        public void reachedDestination() {
                            land(); // ignores callback

//                            hoverAtPoint(landingSpot);
//
//                            platform.createTimer(new TimerCallback() {
//
//                                @Override
//                                public void fire(SimTime time) {
//
//                                    currMoveCallback = callback;
//                                    landAtPoint(landingSpot);
//                                }
//                            }, LANDING_STAGING_TIME, TimeUnit.SECONDS);
                        }
                    });
    }


    /**
     * Turns the helicopter counter-clockwise about the body Z axis (yaw).
     *
     * @param angle The angle to turn (in radians).
     */
    protected void face(double angle) {
        if(currState == MoveState.TERMINATED)
            return;

        yawSetpoint = angle;

//        System.out.println("facing: " + yawSetpoint*180.0/Math.PI);
    }

    protected void face(Vector3f point)
    {
        if(currState == MoveState.TERMINATED)
            return;

        Vector3f pos = posSensor.getPosition();
        face(Math.atan2(point.getY() - pos.getY(), point.getX() - pos.getX()));

//        System.out.println("face: " + point.getX() + " " + point.getY());
//        System.out.println("from: " + pos.getX() + " " + pos.getY());
    }

    /**
     * Indicates that the helicopter should hover at the current altitude setpoint.
     */
    protected void hover() {
        if(currState == MoveState.TERMINATED)
            return;

        Vector3f pos = posSensor.getPosition();
        if( pos.getZ() < LANDING_ALTITUDE ) {
            takeoff_currTarget = posSensor.getPosition();  // take off where we are
            takeoff_yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
            currState = MoveState.TAKEOFF;
            nextState = MoveState.HOVER;
        } else {
            currState = MoveState.HOVER;
        }

    	currTarget = posSensor.getPosition();
        throttlePID.setSetpoint(currTarget.getZ());
        rollPID.setSetpoint(0.0);
        pitchPID.setSetpoint(0.0);
        yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
    }


    /**
     * Indicates that the helicopter should hover at the given altitude.
     *
     * @param altitude The hover altitude (m).
     */
    protected void hover(double altitude) {

        hover();

        currTarget.z = (float)altitude;
        throttlePID.setSetpoint(altitude);
    }


    /**
     * Indicates that the helicopter should hover about a given target point.
     *
     * @param target The point at which the heli should hover.
     */
    protected void hover(Vector3f target)
    {
        hover();

        currTarget = target;
        throttlePID.setSetpoint(target.z);
    }


    /**
     * Indicates that the helicopter should land and idle until given another command.
     */
    protected void idle() {
        currState = MoveState.IDLE;
        control.setThrust(0.0);
    }


    private void updateThrottle(long time, double alt)
    {
        Double throttleDelta = throttlePID.update(time, alt);

        // pid update can return null
        if (throttleDelta == null)
            throttleDelta = 0.0;

        control.setThrust(control.getThrustTrim() + throttleDelta);
    }

    private void updatePitch(long time, double xDisp)
    {
        Double newPitch = pitchPID.update(time, xDisp);

        // pid update can return null
        if (newPitch == null)
            newPitch = 0.0;

//        if( newPitch > 0.35 ) { newPitch = 0.35; }
//        if( newPitch < -0.35 ) { newPitch = -0.35; }

        if( prevPitch == -999 ) { prevPitch = newPitch; }
        double pitchDelta = newPitch - prevPitch;
        if( pitchDelta > controlMaxDelta ) { pitchDelta = controlMaxDelta; }
        if( pitchDelta < -controlMaxDelta ) { pitchDelta = -controlMaxDelta; }
        newPitch = prevPitch + pitchDelta;

        control.setPitch(control.getPitchTrim() + newPitch);

        prevPitch = newPitch;
    }

    private void updatePitchFlow(long time, double flowDiff)
    {
        Double newPitch = flowPitchPID.update(time, flowDiff);

        // pid update can return null
        if (newPitch == null)
            newPitch = 0.0;

//        if( newPitch > 0.35 ) { newPitch = 0.35; }
//        if( newPitch < -0.35 ) { newPitch = -0.35; }

//        double pitchDelta = newPitch - prevPitch;
//        if( pitchDelta > controlMaxDelta ) { pitchDelta = controlMaxDelta; }
//        if( pitchDelta < -controlMaxDelta ) { pitchDelta = -controlMaxDelta; }
//        newPitch = prevPitch + pitchDelta;

        control.setPitch(control.getPitchTrim() + newPitch);

        prevPitch = newPitch;
    }

    private void updateRoll(long time, double yDisp)
    {
        Double newRoll = rollPID.update(time, yDisp);

        // pid update can return null
        if (newRoll == null)
            newRoll = 0.0;

//        if( newRoll > 0.5 ) { newRoll = 0.5; }
//        if( newRoll < -0.5 ) { newRoll = -0.5; }

        if( prevRoll == -999 ) { prevRoll = newRoll; }
        double rollDelta = newRoll - prevRoll;
        if( rollDelta > controlMaxDelta ) { rollDelta = controlMaxDelta; }
        if( rollDelta < -controlMaxDelta ) { rollDelta = -controlMaxDelta; }
        newRoll = prevRoll + rollDelta;

        control.setRoll(control.getRollTrim() + newRoll);

        prevRoll = newRoll;
    }

    private void updateRollFlow(long time, double absFlowDiff)
    {
        Double newRoll = flowRollPID.update(time, absFlowDiff);

        // pid update can return null
        if (newRoll == null)
            newRoll = 0.0;

//        if( newRoll > 0.5 ) { newRoll = 0.5; }
//        if( newRoll < -0.5 ) { newRoll = -0.5; }

//        double rollDelta = newRoll - prevRoll;
//        if( rollDelta > 0.02 ) { rollDelta = 0.02; }
//        if( rollDelta < -0.02 ) { rollDelta = -0.02; }
//        newRoll = prevRoll + rollDelta;

        control.setRoll(control.getRollTrim() + newRoll);

        prevRoll = newRoll;
    }

    private double filterHeading(double heading)
    {
        if( goodOrientation ) {
            prevYaw = currYaw;
            currYaw = heading;

            double yawDiff = currYaw - prevYaw;
            while( yawDiff >= Math.PI )
                yawDiff -= 2.0 * Math.PI;
            while( yawDiff < -Math.PI )
                yawDiff += 2.0 * Math.PI;

            if( yawDiff > 0.5 )
                yawDiff = 0.5;
            if( yawDiff < -0.5 )
                yawDiff = -0.5;

            yawDiffs[yawHistPtr] = yawDiff;
            yawHistPtr = (yawHistPtr + 1)%3;
        }

        double mn = yawDiffs[0], md = yawDiffs[1], mx = yawDiffs[2];
        if( mn > md ) { md = mn; mn = yawDiffs[1]; }
        if( md > mx ) { mx = md; md = yawDiffs[2]; }
        if( mn > md ) { double tmp = md; md = mn; mn = tmp; }

        double dYaw = md;

        idYaw += dYaw;

        if( goodOrientation ) {
            double yawErr = currYaw - idYaw;
            if( yawErr >= Math.PI )
                yawErr -= 2.0 * Math.PI;
            else if( yawErr < -Math.PI )
                yawErr += 2.0 * Math.PI;

            double yawCorrection = 0.1*yawErr;
            if( yawCorrection > 0.05 ) { yawCorrection = 0.05; }
            if( yawCorrection < -0.05 ) { yawCorrection = -0.05; }

            idYaw += yawCorrection;
        }

        if( idYaw >= Math.PI )
            idYaw -= 2 * Math.PI;
        else if( idYaw < -Math.PI )
            idYaw += 2 * Math.PI;

//        System.out.println("Heading: " + heading + " yawDiffs: " + yawDiffs[0] + " " + yawDiffs[1] + " " + yawDiffs[2] + " dYaw: " + dYaw + " idYaw: " + idYaw);

        return idYaw;
    }

    private void updateYaw(long time, double heading)
    {
//        if(currState == MoveState.MOVE)
//            yawSetpoint = Math.atan2((calcTarget.y - pos.y),(calcTarget.x - pos.x));
//        else if(currState == MoveState.HOVER)
//             yawSetpoint = 0.0;
//        yawSetpoint = 1.0 * Math.PI / 2.0;

        double yawDiff = yawSetpoint - heading;
        while( yawDiff >= Math.PI )
            yawDiff -= 2 * Math.PI;
        while( yawDiff < -Math.PI )
            yawDiff += 2 * Math.PI;

       Double yawDelta = yawPID.update(time, yawDiff);

        // pid update can return null
        if(yawDelta == null)
            yawDelta = 0.0;

        double yaw_clamp = 0.2;
        double yaw_clamp_neg = -1.0 * yaw_clamp;
        if(yawDelta > yaw_clamp)
            yawDelta = yaw_clamp;
        else if(yawDelta < yaw_clamp_neg)
            yawDelta = yaw_clamp_neg;

        control.setYaw(control.getYawTrim() + yawDelta);

//        System.out.println("Heading: " + heading + " yawDiff: " + yawDiff + " yawDelta: " + yawDelta);
    }

    private void updateYawFlow(long time, double scaledFlowSum)
    {
//        if(currState == MoveState.MOVE)
//            yawSetpoint = Math.atan2((calcTarget.y - pos.y),(calcTarget.x - pos.x));
//        else if(currState == MoveState.HOVER)
//             yawSetpoint = 0.0;
//        yawSetpoint = 1.0 * Math.PI / 2.0;

        double yawDiff = -3.5*scaledFlowSum;
        while( yawDiff >= Math.PI )
            yawDiff -= 2 * Math.PI;
        while( yawDiff < -Math.PI )
            yawDiff += 2 * Math.PI;

        Double yawDelta = yawPID.update(time, yawDiff);

        // pid update can return null
        if(yawDelta == null)
            yawDelta = 0.0;

        double yaw_clamp = 0.2;
        double yaw_clamp_neg = -1.0 * yaw_clamp;
        if(yawDelta > yaw_clamp)
            yawDelta = yaw_clamp;
        else if(yawDelta < yaw_clamp_neg)
            yawDelta = yaw_clamp_neg;

        control.setYaw(control.getYawTrim() + yawDelta);

//        System.out.println("Heading: " + heading + " yawDiff: " + yawDiff + " yawDelta: " + yawDelta);
    }

    private void showState() {

        if (logger.isDebugEnabled()) {

            logger.debug("State: " + currState + " Pos: " + posSensor.getPosition() +
                         " Target: " + currTarget + " Dist: " + getDistfromPosition3d(currTarget));
        }
    }

    private void logModelData(MoveState s, Vector3f pos1, Vector3f pos2, Quat4f pose1, Quat4f pose2, float time, float dt) {

        if (logData) {

            Vector3f pose = MathUtil.quaternionToEulerZYX(pose2);

            int thrust = AutoHeliBee.rawCommand(control.getThrust());
            int roll = AutoHeliBee.rawCommand(control.getRoll());
            int pitch = AutoHeliBee.rawCommand(control.getPitch());
            int yaw = AutoHeliBee.rawCommand(control.getYaw());
            byte auto_mask = ((AutoHeliBee)control).getHeliAutoMask();

            try {

                logger.info(s.toString() + " " + dt + " " + pos2.getX() + " " + pos2.getY() + " " + pos2.getZ() + " " + thrust + " " + roll + " " + pitch + " " + yaw + " " + auto_mask + "\n");

                logWriter.write(time + " " + dt + " " + currStateInt + " "
                        + pos2.getX() + " " + pos2.getY() + " " + pos2.getZ() + " "
                        + pose.getX() + " " + pose.getY() + " " + pose.getZ() + " "
                        + currTarget.getX() + " " + currTarget.getY() + " " + currTarget.getZ() + " "
                        + thrust + " " + roll + " " + pitch + " " + yaw + " " + " " + auto_mask + " "
                        + throttlePID.getSetPoint() + " " + throttlePID.getIErr() + " " + throttlePID.getDErr() + " "
                        + rollPID.getSetPoint() + " " + rollPID.getIErr() + " " + rollPID.getDErr() + " "
                        + pitchPID.getSetPoint() + " " + pitchPID.getIErr() + " " + pitchPID.getDErr() + " "
                        + yawSetpoint + " " + yawPID.getIErr() + " " + yawPID.getDErr() + " "
                        + flowRollPID.getSetPoint() + " " + flowRollPID.getIErr() + " " + flowRollPID.getDErr() + " "
                        + flowPitchPID.getSetPoint() + " " + flowPitchPID.getIErr() + " " + flowPitchPID.getDErr() + " "
                        + heliData.frameCount + " " + heliData.gyros[0] + " " + heliData.gyros[1] + " " + heliData.gyros[2] + " "
                        + (heliData.process[0]) + " " + (heliData.process[1]) + " " + (heliData.process[2]) + " " + (heliData.process[3]) + " "
                        + (heliData.process[4]) + " " + (heliData.process[5]) + " " + (heliData.process[6]) + " " + (heliData.process[7]) + " "
                        + (heliData.process[8]) + " " + (heliData.process[9]) + " " + (heliData.process[10]) + " " + (heliData.process[11]) + " "
                        + (heliData.process[12]) + " " + (heliData.process[13]) + " " + (heliData.process[14]) + " " + (heliData.process[15]) + " "
                        + fHeading + " " + crossTrackError + " " + pathAngle + " " + rollError + " " + targetBX  + " " + targetBY + " "
                        + heli_auto + " " + heli_lflow + " " + heli_rflow + " " + heli_phase + " " + heli_speed + " " + heli_heading + " "
                        + "\n");
            }
            catch (IOException e) {
                // do nothing
            }
        }
    }


    private Vector3f calcHiveLocation() {

        int numHelis = allHelis.size();
        Vector3f hive = null;

        if (numHelis < 1) {
            return null;
        }
        else if (numHelis == 1) {
            hive =  new Vector3f(hiveLocation.x, hiveLocation.y, LANDING_STAGING_ALTITUDE);
        }
        else {

            double angle = 0; // in radians
            float x,y;

            for(AbstractHeli h: allHelis)
            {
                if (h.getHeliId() == myHeliId)
                {
                    x = hiveLocation.x + (float)(hiveRadius * Math.cos(angle));
                    y = hiveLocation.y + (float)(hiveRadius * Math.sin(angle));
                    hive = new Vector3f(x, y, LANDING_STAGING_ALTITUDE);
                }
                else {
                    angle += 2 * Math.PI / numHelis;
                }
            }
        }

        return hive;
    }


    private AbstractHeli findClosestHeli(List<AbstractHeli> helis, float threshold, float thresholdHive) {

        AbstractHeli closestHeli = null;
        Vector3f otherPos;
        float dist;
        float minDist = Float.MAX_VALUE;
        float thresh;

        for(AbstractHeli h: helis) {

            if (h.getHeliId() != myHeliId) {

                otherPos = h.getTruthPosition();
                dist = getDistfromPosition2d(otherPos);

                if (dist < minDist) {

                    thresh = threshold;

                    if (otherPos.z <= LANDING_ALTITUDE) {
                        thresh = thresholdHive;
                    }

                    if (dist <= thresh) {
                        
                        closestHeli = h;
                        minDist = dist;
                    }
                }
            }
        }

        return closestHeli;
    }


    private float getDistfromPosition2d(Vector3f value) {

        Vector3f pos = posSensor.getPosition();
        Vector3f temp = new Vector3f(value);
        temp.sub(pos);
        return((float)Math.sqrt(temp.x*temp.x + temp.y*temp.y));

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
