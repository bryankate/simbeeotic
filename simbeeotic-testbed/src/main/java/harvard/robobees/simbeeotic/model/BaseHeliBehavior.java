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
    
    // data logging parameters
    private boolean logData = false;
    private String logPath = "./heli_log.txt";
    
    // state
    private Vector3f lastPos = new Vector3f();
    private Quat4f lastPose = new Quat4f();
    private long lastTime = 0;
    private MoveState currState = MoveState.IDLE;
    private Vector3f currTarget = new Vector3f();
    private double currEpsilon = 0.1;   // in meters
    private MoveCallback currMoveCallback;
    private List<AbstractHeli> allHelis;
    private Vector3f calcTarget;
    private Vector3f rVec; // repulsive vector for obst. avoidance
    private int myHeliId;
    private Vector3f hiveLocation; // where this helicopter should land
    private double hiveRadius = 0.55; // in meters; calculated later

    // controllers and set points
    private PIDController throttlePID;
    private PIDController pitchPID;
    private PIDController rollPID;
    private double yawSetpoint = 0;  // radians

    // trim values for a helicopter
    private double throttleTrim = 0.5;
    private double rollTrim = 0.5;
    private double pitchTrim = 0.5;
    private double yawTrim = 0.5;

    private PositionSensor posSensor;
    private PoseSensor orientSensor;

    private HeliControl control; // handle to helicopter control

    private enum MoveState {IDLE, HOVER, MOVE, LAND}

    private static Logger logger = Logger.getLogger(BaseHeliBehavior.class);
    FileWriter stream;
    BufferedWriter out;

    private void showState() {

        if (logger.isDebugEnabled()) {

            logger.debug("State: " + currState + " Location " +  posSensor.getPosition() +
                         " Target: " + currTarget + " Dist " + getDistfromPosition3d(currTarget) +
                         " PitchTrim: " + simToHeli(pitchTrim));
        }
    }
    
    private void logModelData(Vector3f pos1, Vector3f pos2, Quat4f pose1, Quat4f pose2, float dt)
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
	    	
	    	int thrust = simToHeli(control.getThrust()) - simToHeli(throttleTrim);
	    	int roll = simToHeli(control.getRoll()) - simToHeli(rollTrim);
	    	int pitch = simToHeli(control.getPitch()) - simToHeli(pitchTrim);
	    	int yaw = simToHeli(control.getYaw()) - simToHeli(yawTrim);
	    	
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


    private Vector3f calcHiveLocation()
    {
        int numHelis = allHelis.size();
        Vector3f hive = null;

        if (numHelis < 1)
           return null;

        else if (numHelis == 1)
            hive =  new Vector3f(0,0,0);

        else
        {
            double angle = 0; // in radians
            int height_index = 1;
            float x,y;
            
            for(AbstractHeli h: allHelis)
            {
                if (h.getHeliId() == myHeliId)
                {
                    x = (float)(hiveRadius*Math.cos(angle));
                    y = (float)(hiveRadius*Math.sin(angle));
                    hive = new Vector3f(x, y, 2 * height_index / allHelis.size());
                }
                else
                {
                    angle += 2 * Math.PI / numHelis;
                    height_index++;
                }
            }
        }
        
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


    private void updateThrottle(long time, Vector3f pos)
    {
        Double throttleDelta = throttlePID.update(time, pos.z);

        // pid update can return null
        if (throttleDelta == null)
            throttleDelta = 0.0;

        double newThrottle = throttleTrim + throttleDelta;
        
        // Make sure we don't exceed min and max throttle
        if (newThrottle > throttleTrim + 0.25)
            newThrottle = throttleTrim + 0.25;
        
        if (newThrottle < throttleTrim - 0.1)
            newThrottle = throttleTrim - 0.1;
        
        control.setThrust(newThrottle);
    }
    
    private void updatePitch(long time, double xDisp)
    {
    	Double pitchDelta = pitchPID.update(time, xDisp);

        // pid update can return null
        if (pitchDelta == null)
            pitchDelta = 0.0;

        double newPitch = pitchTrim + pitchDelta;
        
        control.setPitch(newPitch);
    }
    
    private void updateRoll(long time, double yDisp)
    {
    	Double rollDelta = rollPID.update(time, yDisp);

        // pid update can return null
        if (rollDelta == null)
            rollDelta = 0.0;

        double newRoll = rollTrim + rollDelta;
        
        control.setRoll(newRoll);
    }

    private void updateYaw(Vector3f pos, Vector3f euler)
    {
    	if (currState == MoveState.MOVE)
    		yawSetpoint = Math.atan2(calcTarget.y - pos.y, calcTarget.x - pos.x);
        
        double yaw = yawTrim + (-0.3 * Math.sin(euler.z - yawSetpoint));

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

    protected void landHeli() {

        moveToPoint(hiveLocation.x, hiveLocation.y, hiveLocation.z, 0.1,
                    new MoveCallback() {

                        @Override
                        public void reachedDestination() {
                            land();
                        }
                    });

    }


    protected void landHeli(final MoveCallback callback) {

        moveToPoint(hiveLocation.x, hiveLocation.y, hiveLocation.z, 0.1,
                    new MoveCallback() {

                        @Override
                        public void reachedDestination() {
                            
                            land();
                            callback.reachedDestination();
                        }
                    });

    }

    @Override
    public void start(final Platform platform, final HeliControl control)
    {
    	try
    	{
    		stream = new FileWriter(logPath);
    		out = new BufferedWriter(stream);
    	}
    	catch(IOException e)
    	{
    		// do nothing
    	}
    	
        allHelis = simEngine.findModelsByType(AbstractHeli.class);
        hiveRadius = (allHelis.size() * 0.2) / (2 * Math.PI);
        
        posSensor = platform.getSensor("position-sensor", PositionSensor.class);
        orientSensor = platform.getSensor("pose-sensor", PoseSensor.class);
        this.control = control;

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
        control.setPitch(pitchTrim);
        control.setRoll(rollTrim);
        control.setYaw(yawTrim);

        myHeliId = control.getHeliId();

        hiveLocation = calcHiveLocation();


        // a timer that implements the low level control of the heli altitude and bearing
        controlTimer = platform.createTimer(new TimerCallback()
        {
            public void fire(SimTime time)
            {
            	Vector3f pos = posSensor.getPosition();
                Quat4f pose = orientSensor.getPose();
                long realTime = System.currentTimeMillis();
                
                // data for use in Ben's model
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

                calcTarget = new Vector3f(currTarget);

                // logger.info("In baseHeli fire with state:" + currState);
                showState();
                

                switch(currState)
                {
                    case IDLE:
                        if (control.getThrust() > 0.0)
                            control.setThrust(0.0);
                        break;

                    case LAND:
                    	if (pos.z < 0.05)
                    		currState = MoveState.IDLE;
                    	else
                    	{
                    		updateYaw(pos, euler);
                    		updatePitch(time.getTime(), disp.dot(bodyX));
                    		updateRoll(time.getTime(), disp.dot(bodyY));
                    	}
                        break;

                    case MOVE:
                        double dist = getDistfromPosition3d(currTarget);
                        double dist2 = getDistfromPosition3d(calcTarget);

                        if (Math.min(dist, dist2) <= currEpsilon) {
                            
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
                        
                        
                        // give the walls, ceiling, and floor a repulsive force
                        calcTarget.x -= Math.tan(pos.x * Math.PI / 4) * Math.abs(calcTarget.x) / 5;
                        calcTarget.y -= Math.tan(pos.y * Math.PI / 4) * Math.abs(calcTarget.y) / 5;
                        calcTarget.z -= Math.tan((pos.z - 1) * Math.PI / 2) * Math.abs(calcTarget.z - 1) / 10;
                        
                        
                        /*if (pos.x > 1.75) {
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
                        if (pos.z > 1.75) {
                        	calcTarget.z -= 1.0/(2.0 - min(pos.z, 2.0f) + 0.2);
                        }*/

                        AbstractHeli closestHeli = findClosestHeli(allHelis, 1.2f);

                        if (closestHeli != null && pos.z > 0.25)
                        {
                            dist = getDistfromPosition3d(closestHeli.getTruthPosition());
                            rVec = new Vector3f(pos);
                            rVec.sub(closestHeli.getTruthPosition());
                            rVec.scale((float) (1 / Math.pow(dist, 3)));
                            calcTarget.add(rVec);
                        }
                        
                        //logger.info("Calculated Target: " + calcTarget);

                        updateThrottle(time.getTime(), pos);
                        updateYaw(pos, euler);
                        control.setPitch(pitchTrim + 0.15);
                        break;

                    case HOVER:
                        updateThrottle(time.getTime(), pos);
                        updateYaw(pos, euler);
                        updatePitch(time.getTime(), disp.dot(bodyX));
                        updateRoll(time.getTime(), disp.dot(bodyY));
                        
                        break;

                    default:
                        // do nothing
                }
            }
        }, 0, TimeUnit.MILLISECONDS, 20, TimeUnit.MILLISECONDS);
    }


    @Override
    public void stop() {

        control.setThrust(0.0);
        
        try
        {
        	out.close();
        }
        catch (IOException e)
        {
        	// do nothing
        }
        
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


    protected void land()
    {
    	control.setThrust(throttleTrim - 0.08);
    	control.setPitch(pitchTrim);
    	control.setYaw(yawTrim);
    	control.setRoll(rollTrim);
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
    protected void hover()
    {
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
    protected void hover(double altitude)
    {
    	System.out.println("Hovering");
        hover();
        currTarget.z = (float)altitude;
    	yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
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
    	yawSetpoint = MathUtil.quaternionToEulerZYX(orientSensor.getPose()).z;
        throttlePID.setSetpoint(target.z);
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
