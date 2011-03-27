package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.sensor.PoseSensor;
import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.util.PIDController;
import org.apache.log4j.Logger;

import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;


/**
 * A base class that implements a simple movement abstraction on top of the
 * raw helicopter control. The abstraction provides a mechanism to move to an
 * arbitrary point in 3D space, reorient in place, and hover in place.
 *
 * @author bkate
 */
public abstract class BaseHeliBehavior implements HeliBehavior {

    private Timer controlTimer;

    // state
    private MoveState currState = MoveState.IDLE;
    private Vector3f currTarget = new Vector3f();
    private double currEpsilon = 0.1;   // m
    private MoveCallback currMoveCallback;

    // controllers and set points
    private PIDController throttlePID;
    private PIDController pitchPID;
    private PIDController rollPID;
    private double yawSetpoint = 0;  // rad

    // trim values for a helicoptor
    // todo -- make this a per helicopter parameter
    private double throttle_trim = (483-170.0)/682;
    private double roll_trim =0.5;
    private double pitch_trim = 0.6;
    private double yaw_trim = 0.5;

    // params

    private enum MoveState {IDLE, HOVER, MOVE}

    private static Logger logger = Logger.getLogger(BaseHeliBehavior.class);


    @Override
    public void start(final Platform platform, final HeliControl control) {

        final PositionSensor posSensor = platform.getSensor("position-sensor", PositionSensor.class);
        final PoseSensor orientSensor = platform.getSensor("pose-sensor", PoseSensor.class);

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
        control.setPitch(pitch_trim);
        control.setRoll(roll_trim);
        control.setYaw(yaw_trim);



        // a timer that implements the low level control of the heli altitude and bearing
        controlTimer = platform.createTimer(new TimerCallback() {

            private double throttle = control.getThrust();
            private double pitch = control.getPitch();

            @Override
            public void fire(SimTime time) {

                Vector3f pos = posSensor.getPosition();
                Vector3f euler = MathUtil.quaternionToEulerZYX(orientSensor.getPose());
                boolean updateCommands = true;

                // logger.info("In baseHeli fire with state:" + currState);

                switch(currState) {
                    case IDLE:
                        if (pos.z > 0.1) {
                            currTarget.z -= 0.003048; // about 6in per sec @ 50 Hz
                            if (currTarget.z < 0) {
                                currTarget.z = 0;
                            }
                        }
                        break;


                    case MOVE:

                        // get distance from desired location
                        Vector3f temp = new Vector3f(currTarget);
                        temp.sub(pos);
                        double dist = temp.length();

                        //logger.info("MOVE: target: " + temp + " Position: " + pos);
                        if (dist <= currEpsilon) {
                            
                            // made it! default to hovering
                            hover();

                            if (currMoveCallback != null) {
                                currMoveCallback.reachedDestination();
                            }

                            currMoveCallback = null;
                        }

                        break;


                    default:

                        // do nothing
                }

                if (updateCommands) {
                    Double currThrottle = control.getThrust();

                    // Handle the idle case
                    if (currState == MoveState.IDLE && currTarget.z == 0) {

                        if (currThrottle> 0) {
                            control.setThrust(0);
                        }
                    }
                    // Moving or Hovering
                    else {
                        // adjust the throttle
                        logger.info("Time is: " + time.getTime());
                        Float Diff = currTarget.z - pos.z;
                        Double throttleDiff = throttlePID.update(time.getTime(), pos.z);
                        if (throttleDiff == null) {
                            throttleDiff = 0.0;
                        }

                        Double newThrottle = currThrottle + throttleDiff;
                        if (newThrottle > throttle_trim + 0.25) {
                            newThrottle = throttle_trim + 0.25;
                        }
                        if (newThrottle < throttle_trim - 0.07) {
                            newThrottle = throttle_trim - 0.07;
                        }
                        control.setThrust(newThrottle);

                        // adjust the yaw
                        yawSetpoint = Math.atan2(currTarget.y - pos.y, currTarget.x - pos.x);
                        Double yaw = yaw_trim + (-0.3 * Math.sin(euler.z - yawSetpoint));
                        if (yaw > 0.75) {
                            yaw = 0.75;
                        }
                        if (yaw < 0.19) {
                            yaw = 0.19;
                        }
                        control.setYaw(yaw);

                        // adjust the pitch
                        if (currState == MoveState.MOVE) {
                            control.setPitch(pitch_trim + 0.3);
                        }

                        if (currState == MoveState.HOVER) {
                            control.setPitch(pitch_trim + 0.4);
                        }


                        //logger.info("height: " + pos.z + " Desired: " + currTarget.z + " Diff: " + Diff +
                        //        " Throttle: " +
                        //        ((newThrottle)*682+170) + " NewThrottle " + newThrottle);


                    }




                }

                if (logger.isDebugEnabled()) {

                    //logger.debug(time + " " + currState + " " + pos + " " + euler + " " +
                    //             throttle + " " + yaw + " " + pitch + " " + roll);
                    logger.debug(time + " " + currState + " " + pos + " " + euler + " " +
                                 throttle + " " + pitch + " " );

                }

                //control.setPitch(pitch);
                //control.setThrust(throttle);
            }
        }, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop() {
        controlTimer.cancel();
    }

    /**
     * Updates the throttle controllor
     *
     * @param desiredHeight The desired height of the heli (m).
     */
    protected void updateThrottle(double desiredHeight) {


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
    }


    /**
     * A callback that can be implemented by derived classes to be informed when the
     * heli has reached a desired destination.
     */
    protected static interface MoveCallback {

        public void reachedDestination();
    }
}
