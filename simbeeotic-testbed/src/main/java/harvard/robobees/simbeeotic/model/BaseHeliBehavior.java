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

    // params

    private enum MoveState {IDLE, TAKEOFF, HOVER, MOVE}

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
        throttlePID = new PIDController(0, 0.005, 1.5e-8, 0.05);
        pitchPID = new PIDController(0, 0.001, 0, 0);
        rollPID = new PIDController(0, 0.001, 0, 0);

        // a timer that implements the low level control of the heli altitude and bearing
        controlTimer = platform.createTimer(new TimerCallback() {

            private double throttle = control.getThrust();
            private double pitch = control.getPitch();
            private double roll = control.getRoll();
            private double yaw = control.getYaw();

            @Override
            public void fire(SimTime time) {

                Vector3f pos = posSensor.getPosition();
                Vector3f euler = MathUtil.quaternionToEulerZYX(orientSensor.getPose());
                boolean updateCommands = true;

                switch(currState) {

                    case IDLE:

                        if (pos.z < 0.1) {

                            throttle = 0;
                            
                            updateCommands = false;
                        }
                        
                        break;

                    case TAKEOFF:

                        if (pos.z > 0.5) {
                            hover();
                        }
                        else {

                            throttle = 0.6;
                            pitch = 0.5;
                            roll = 0.5;

                            updateCommands = false;
                        }

                        break;

                    case MOVE:

                        Vector3f temp = new Vector3f(currTarget);
                        temp.sub(pos);

                        double dist = temp.length();

                        if (dist <= currEpsilon) {
                            
                            // made it! default to hovering
                            hover();

                            if (currMoveCallback != null) {
                                currMoveCallback.reachedDestination();
                            }

                            currMoveCallback = null;
                        }
                        else {

                            // orient to face target and pitch forward
                            yawSetpoint = Math.atan2(currTarget.y - pos.y, currTarget.x - pos.x);
                        }

                        break;

                    default:

                        // do nothing
                }

                if (updateCommands) {

                    Double throttleDiff = throttlePID.update(time.getTime(), pos.z);

                    if (throttleDiff != null) {

                        // do not move too much in one timestep
                        if (throttleDiff > 0.1) {
                            throttleDiff = 0.1;
                        }

                        if (throttleDiff < -0.01) {
                            throttleDiff = -0.01;
                        }

                        // gravity compensation
                        if (throttleDiff < 0) {
                            throttleDiff /= 2;
                        }

                        // set new throttle
                        throttle += throttleDiff;
                    }

                    yaw = 0.5 + (-0.3 * Math.sin(euler.z - yawSetpoint));

                    Double pitchDiff = pitchPID.update(time.getTime(), euler.x);
                    Double rollDiff = rollPID.update(time.getTime(), euler.y);

                    if (pitchDiff != null) {
                        pitch += pitchDiff;
                    }

                    if (rollDiff != null) {
                        roll += rollDiff;
                    }
                }

                if (logger.isDebugEnabled()) {

                    logger.debug(time + " " + currState + " " + pos + " " + euler + " " +
                                 throttle + " " + yaw + " " + pitch + " " + roll);
                }

                control.setYaw(yaw);
                control.setPitch(pitch);
                control.setRoll(roll);
                control.setThrust(throttle);
            }
        }, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop() {
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
        pitchPID.setSetpoint(Math.PI / 8);
        rollPID.setSetpoint(0);
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

        pitchPID.setSetpoint(0);
        rollPID.setSetpoint(0);
    }


    /**
     * Indicates that the helicopter should hover at the given altitude.
     *
     * @param altitude The hover altitude (m).
     */
    protected void hover(double altitude) {

        throttlePID.setSetpoint(altitude);
        hover();
    }


    /**
     * Sets the throttle to a constant amount to takeoff.
     */
    protected void takeoff() {

        currState = MoveState.TAKEOFF;

        pitchPID.setSetpoint(0);
        rollPID.setSetpoint(0);
    }


    /**
     * Indicates that the helicopter should land and idle until given another command.
     */
    protected void idle() {

        currState = MoveState.IDLE;

        throttlePID.setSetpoint(0);
        pitchPID.setSetpoint(0);
        rollPID.setSetpoint(0);
    }


    /**
     * A callback that can be implemented by derived classes to be informed when the
     * heli has reached a desired destination.
     */
    protected static interface MoveCallback {

        public void reachedDestination();
    }
}
