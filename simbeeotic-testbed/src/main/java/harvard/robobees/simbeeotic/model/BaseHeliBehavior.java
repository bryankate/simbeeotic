package harvard.robobees.simbeeotic.model;


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

import javax.vecmath.Vector3f;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.*;


/**
 * A base class that implements a simple movement abstraction on top of the
 * raw helicopter control. The abstraction provides a mechanism to move to an
 * arbitrary point in 3D space, reorient in place, and hover in place.
 *
 * @author bkate
 */
public abstract class BaseHeliBehavior implements HeliBehavior {

    private Timer controlTimer;
    private SimEngine simEngine;

    // state
    private MoveState currState = MoveState.IDLE;
    private Vector3f currTarget = new Vector3f();
    private double currEpsilon = 0.1;   // m
    private MoveCallback currMoveCallback;
    private List<AbstractHeli> allHelis;
    private Vector3f calcTarget;
    private Vector3f rVec; // repulsive vector for obst. avoidance
    private int myHeliId;
    private Vector3f hiveLocation; // where this helicopter should land
    private double hiveRadius = 0.45; // in meters

    // controllers and set points
    private PIDController throttlePID;
    private double yawSetpoint = 0;  // rad

    // trim values for a helicoptor
    private double throttleTrim = 0.5;
    private double rollTrim = 0.5;
    private double pitchTrim = 0.5;
    private double yawTrim = 0.5;

    private PositionSensor posSensor;
    private PoseSensor orientSensor;

    private HeliControl control; // handle to helicopter control

    private enum MoveState {IDLE, HOVER, MOVE, LAND}

    private static Logger logger = Logger.getLogger(BaseHeliBehavior.class);

    private void showState() {

        if (logger.isDebugEnabled()) {

            logger.debug("State: " + currState + " Location " +  posSensor.getPosition() +
                         " Target: " + currTarget + " Dist " + getDistfromPosition3d(currTarget) +
                         " PitchTrim: " + simToHeli(pitchTrim));
        }
    }


    private Vector3f calcHiveLocation() {

        int numHelis = allHelis.size();
        Vector3f hive = null;

        if (numHelis < 1) {
           return null;
        }

        if (numHelis == 1) {
            hive =  new Vector3f(0,0,0);
        }

        else {

            double angle = 0; // in radians
            float x,y;

            for(AbstractHeli h: allHelis) {

                if (h.getHeliId() == myHeliId) {
                    x = (float)(hiveRadius*Math.sin(angle));
                    y = (float)(hiveRadius*Math.cos(angle));
                    hive = new Vector3f(x,y,0);
                }
                else {
                    angle += 2*Math.PI/numHelis;
                }
            }
        }

        logger.debug("Heli: " + myHeliId + " Num Helis: " + numHelis + " Hive: " + hive);

        return hive;
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


    private void updateThrottle(long time, Vector3f pos) {

        Double currThrottle = control.getThrust();
        Double throttleDiff = throttlePID.update(time, pos.z);

        // pid update can return null
        if (throttleDiff == null) {
            throttleDiff = 0.0;
        }

        // Make sure we don't exceed min and max throttle
        Double newThrottle = currThrottle + throttleDiff;
        if (newThrottle > throttleTrim + 0.25) {
            newThrottle = throttleTrim + 0.25;
        }
        if (newThrottle < throttleTrim - 0.07) {
            newThrottle = throttleTrim - 0.07;
        }
        control.setThrust(newThrottle);
    }

    private void updateYaw(Vector3f pos, Vector3f euler) {
        yawSetpoint = Math.atan2(calcTarget.y - pos.y, calcTarget.x - pos.x);

//        for(AbstractHeli h: allHelis) {
//            logger.debug(h.getTruthPosition());
//            h.getHeliId()
//        }

        Double yaw = yawTrim + (-0.3 * Math.sin(euler.z - yawSetpoint));

        if (yaw > 0.75) {
            yaw = 0.75;
        }

        if (yaw < 0.19) {
            yaw = 0.19;
        }

        control.setYaw(yaw);
    }

    
    /**
     * Convert a raw heli command value (170-852) and return the normalized command (0.0 - 1.0)
     * @param value raw heli command
     * @return sim heli command
     */
    private double heliToSim(int value) {
        return ((value-170)/682.0);
    }


    /**
     * Convert a sim heli command value (0.0 - 1.0) to a raw heli command (170 - 852)
     * @param value sim heli command
     * @return raw heli command
     */
    private int simToHeli(double value) {
        return (170 + (int)round((value * 682)));
    }


    private AbstractHeli findClosestHeli(List<AbstractHeli> helis, float threshold) {
        AbstractHeli closestHeli = null;
        float dist;
        float minDist = Float.MAX_VALUE;
        
        for(AbstractHeli h: helis) {
            if (h.getHeliId() != myHeliId) {
                dist = getDistfromPosition2d(h.getTruthPosition());
                if (dist < minDist && dist <= threshold) {
                    closestHeli = h;
                    minDist = dist;
                }
            }
        }
        return closestHeli;
    }

    private AbstractHeli findClosestHeli(List<AbstractHeli> helis) {
        return findClosestHeli(helis, -1.0f);
    }

    protected void landHeli() {

        moveToPoint(hiveLocation.x, hiveLocation.y, 0.05, 0.2,
                    new MoveCallback() {

                        @Override
                        public void reachedDestination() {
                            idle();
                        }
                    });

    }


    protected void landHeli(final MoveCallback callback) {

        moveToPoint(hiveLocation.x, hiveLocation.y, 0.05, 0.2,
                    new MoveCallback() {

                        @Override
                        public void reachedDestination() {
                            idle();
                            callback.reachedDestination();
                        }
                    });

    }

    @Override
    public void start(final Platform platform, final HeliControl control) {


        allHelis = simEngine.findModelsByType(AbstractHeli.class);
        posSensor = platform.getSensor("position-sensor", PositionSensor.class);
        orientSensor = platform.getSensor("pose-sensor", PoseSensor.class);
        this.control = control;
        boolean init = false;

        if (posSensor == null) {
            throw new RuntimeModelingException("A position sensor is needed for BaseHeliBehavior.");
        }

        if (orientSensor == null) {
            throw new RuntimeModelingException("A pose sensor is needed for the BaseHeliBehavior.");
        }

        // todo: params for gains
        throttlePID = new PIDController(1, 100.0, 0.0, 100.0);

        // send an inital command to the heli to put in a neutral state

        control.setThrust(0.0);
        control.setPitch(pitchTrim);
        control.setRoll(rollTrim);
        control.setYaw(yawTrim);

        myHeliId = control.getHeliId();

        hiveLocation = calcHiveLocation();


        // a timer that implements the low level control of the heli altitude and bearing
        controlTimer = platform.createTimer(new TimerCallback() {

            private double throttle = control.getThrust();
            private double pitch = control.getPitch();

            @Override
            public void fire(SimTime time) {

                Vector3f pos = posSensor.getPosition();
                Vector3f euler = MathUtil.quaternionToEulerZYX(orientSensor.getPose());
                Vector3f attractiveForce;
                Vector3f repulsiveForce;

                calcTarget = new Vector3f(currTarget);

                // logger.info("In baseHeli fire with state:" + currState);
                showState();

                switch(currState) {

                    case IDLE:

                        if (control.getThrust() > 0.0) {
                            control.setThrust(0.0);
                        }

                        break;

                    case LAND:
                        break;

                    case MOVE:

                        double dist = getDistfromPosition2d(currTarget);

//                        logger.debug("Heli: " + myHeliId + " Dist to target: " + dist + " pos: " + pos +
//                                     " Target: " + currTarget);

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
                        }

                        // give the walls a repulsive force
                        if (pos.x > 1.75) {
                            calcTarget.x -= 1.0/(2.0 - min(pos.x, 2.0f) + 0.2);
                        }
                        if (pos.x < -1.75) {
                            calcTarget.x += 1.0/(2.0 + max(pos.x, -2.0f) + 0.2);
                        }
                        if (pos.y > 1.75) {
                            calcTarget.y -= 1.0/(2.0 - min(pos.y, 2.0f) + 0.2);
                        }
                        if (pos.y < -1.75) {
                            calcTarget.y += 1.0/(2.0 + max(pos.y, -2.0f) + 0.2);

                        }

                        AbstractHeli closestHeli = findClosestHeli(allHelis, 1.2f);

                        if (closestHeli != null && pos.z > 0.25) {

                            dist = getDistfromPosition2d(closestHeli.getTruthPosition());
                            rVec = new Vector3f(pos);
                            rVec.sub(closestHeli.getTruthPosition());
                            rVec.scale(1/(float)(dist + 0.1));
                            calcTarget.add(rVec);
                        }

                        updateThrottle(time.getTime(), pos);
                        updateYaw(pos, euler);
                        control.setPitch(pitchTrim + 0.15);

                        //double dist = temp.length();
                        //double dist = Math.sqrt(temp.x*temp.x + temp.y*temp.y);
                        //double newPitch = pitchTrim + (0.3 * Math.atan(dist));
                        break;

                    case HOVER:

                        updateThrottle(time.getTime(), pos);
                        updateYaw(pos, euler);
                        control.setPitch(pitchTrim + 0.05);

                    default:

                        // do nothing
                }
            }
        }, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop() {

        control.setThrust(0.0);
        controlTimer.cancel();
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

        currState = MoveState.MOVE;

        currTarget = new Vector3f((float)x, (float)y, (float)z);
        currEpsilon = epsilon;
        currMoveCallback = callback;
        throttlePID.setSetpoint(z);
    }


    protected void land() {
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
        currState = MoveState.HOVER;
    }


    /**
     * Indicates that the helicopter should hover at the given altitude.
     *
     * @param altitude The hover altitude (m).
     */
    protected void hover(double altitude) {
        currTarget.z = (float)altitude;
        hover();
    }


    /**
     * Indicates that the helicopter should land and idle until given another command.
     */
    protected void idle() {
        currState = MoveState.IDLE;
        control.setThrust(0.0);
    }


    @Inject(optional = true)
    public final void setThrottleTrim(@Named("trim-throttle") final int trim) {
        this.throttleTrim = HWILBee.normCommand(trim);
    }


    @Inject(optional = true)
    public final void setYawTrim(@Named("trim-yaw") final int trim) {
        this.yawTrim = HWILBee.normCommand(trim);
    }


    @Inject(optional = true)
    public final void setRollTrim(@Named("trim-roll") final int trim) {
        this.rollTrim = HWILBee.normCommand(trim);
    }


    @Inject(optional = true)
    public final void setPitchTrim(@Named("trim-pitch") final int trim) {
        this.pitchTrim = HWILBee.normCommand(trim);
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
