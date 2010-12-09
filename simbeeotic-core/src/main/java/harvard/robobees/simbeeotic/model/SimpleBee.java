package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.environment.PhysicalConstants;
import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.weather.WeatherModel;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.concurrent.TimeUnit;
import java.awt.*;


/**
 * A simple bee platform that can be used as a starting point for creating
 * more complicated bee models. This base class creates a physical body in
 * the world (a sphere) and has primitive flight controls to simplify
 * movement. Sensors and a radio can be attached at runtime and the logic is
 * supplied as user-defined behavior.
 *
 * @author bkate
 */
public abstract class SimpleBee extends GenericModel {

    // parameters
    private float length = 0.2f;    // m
    private float mass = 0.128f;    // g
    private float maxAccel = 1.0f;  // m/s^2
    private long kinematicUpdateRate = 100;  // ms
    private double hoverEnergy = 500;        // mA
    private double actuationEnergy = 250;    // mA
    private float actuationErrTurnVar = 0;   // radians
    private float actuationErrDirVar = 0;    // percent (0-1 scale)
    private float actuationErrVelVar = 0;    // percent (0-1 scale)
    private boolean useWind = false;

    protected Timer kinematicTimer;

    // physical state
    private RigidBody body;
    private Vector3f desiredLinVel = new Vector3f();
    private Vector3f hoverForce;
    private boolean hovering = false;


    /** {@inheritDoc} */
    @Override
    public void initialize() {

        super.initialize();

        // get the weather model, if one exists
        final WeatherModel weather = getSimEngine().findModelByType(WeatherModel.class);

        // setup a timer that handles the details of using the simple flight abstraction
        if (kinematicUpdateRate > 0) {

            kinematicTimer = createTimer(new TimerCallback() {

                public void fire(SimTime time) {

                    clearForces();

                    updateKinematics(time);

                    float availThrust = maxAccel * mass; // N
                    Vector3f windForce = new Vector3f();
                    Vector3f totalNonHoverForce = new Vector3f();

                    // account for gravity (or not)
                    if (hovering) {

                        body.activate();
                        applyForce(hoverForce);

                        getAggregator().addValue("energy", "actuation", hoverEnergy * kinematicUpdateRate / TimeUnit.SECONDS.toMillis(1));
                    }

                    // apply wind effects
                    if (useWind && (weather != null)) {

                        Vector3f windVel = weather.getWindVelocity(time, getTruthPosition());
                        windForce = new Vector3f(windVel);

                        windForce.normalize();

                        double speed = windVel.length();

                        double area = length / 2;
                        area *= area * Math.PI;

                        // calculate wind force
                        double force = 0.5 * PhysicalConstants.AIR_DENSITY * speed * speed * area;

                        windForce.scale((float)force);
                        totalNonHoverForce.add(windForce);
                    }

                    if (hovering || (desiredLinVel.length() > 0)) {

                        // desired final direction in body frame
                        Vector3f impulse = new Vector3f(desiredLinVel);
                        float impulseMag = impulse.length();

                        // apply some actation error to the direction of
                        // the velocity vector to make it go off course
                        if ((impulseMag > 0) && (actuationErrDirVar > 0)) {

                            impulse.normalize();

                            impulse.x += getRandom().nextGaussian() * actuationErrDirVar;
                            impulse.y += getRandom().nextGaussian() * actuationErrDirVar;
                            impulse.z += getRandom().nextGaussian() * actuationErrDirVar;

                            impulse.normalize();
                            impulse.scale(impulseMag);
                        }

                        // apply some actation error to the magnitude of the velocity so that
                        // the bee may go further/shorter than expected
                        if ((impulseMag > 0) && (actuationErrVelVar > 0)) {

                            impulseMag += (impulseMag * getRandom().nextGaussian() * actuationErrVelVar);

                            impulse.normalize();
                            impulse.scale(impulseMag);
                        }

                        Transform trans = new Transform();

                        // translate to world frame
                        trans.setIdentity();
                        trans.setRotation(getTruthOrientation());
                        trans.transform(impulse);

                        // adjust to compensate for external forces like wind
                        impulse.scale(getMass());
                        impulse.sub(totalNonHoverForce);
                        impulse.scale(1 / getMass());

                        // find the velocity change and determine the instantaneous acceleration required
                        impulse.sub(getTruthLinearVelocity());

                        // cap the translational force based on the max acceleration ability
                        impulseMag = impulse.length();

                        if (impulseMag > maxAccel) {

                            impulse.normalize();
                            impulse.scale(maxAccel);
                        }

                        // apply an instantaneous force to get the desired velocity change
                        impulse.scale(getMass());

                        // energy accounting
                        double expended = impulse.length() * (actuationEnergy / (mass * maxAccel));
                        getAggregator().addValue("energy", "actuation", expended * kinematicUpdateRate / TimeUnit.SECONDS.toMillis(1));

                        totalNonHoverForce.add(impulse);
                    }

                    // todo: drag?

                    // make it so
                    if (totalNonHoverForce.length() > 0) {

                        body.activate();
                        applyForce(totalNonHoverForce);
                    }
                }
            }, 0, TimeUnit.MILLISECONDS, kinematicUpdateRate, TimeUnit.MILLISECONDS);
        }
    }


    /** {@inheritDoc} */
    @Override
    protected RigidBody initializeBody(DiscreteDynamicsWorld world) {

        float halfLength = length / 2;

        // establish the bee body
        CollisionShape colShape = new SphereShape(halfLength);

        Transform startTransform = new Transform();
        startTransform.setIdentity();

        Vector3f localInertia = new Vector3f(0, 0, 0);
        colShape.calculateLocalInertia(mass, localInertia);

        Vector3f start = getStartPosition();
        start.z += halfLength;

        startTransform.origin.set(start);

        int id = getObjectId();
        
        getMotionRecorder().initializeObject(id, colShape);
        getMotionRecorder().updateMetadata(id, new Color(238, 201, 0), null, getName());

        MotionState myMotionState = new RecordedMotionState(id, getMotionRecorder(), startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState,
                                                                         colShape, localInertia);

        // modify the thresholds for deactivating the bee
        // because it moves at a much smaller scale
        rbInfo.linearSleepingThreshold = 0.08f;  // m/s
        rbInfo.angularSleepingThreshold = 0.1f;  // rad/s

        body = new RigidBody(rbInfo);

        // todo: put the bee's properties into the entity info?
        body.setUserPointer(new EntityInfo(id));

        // bees do not collide with each other or the hive
        world.addRigidBody(body, COLLISION_BEE, (short)(COLLISION_TERRAIN | COLLISION_FLOWER));

        hoverForce = new Vector3f(0, 0, mass * (float)-PhysicalConstants.EARTH_GRAVITY);

        return body;
    }


    /** {@inheritDoc} */
    @Override
    public void finish() {
    }


    /**
     * A method that is called by the kinematic timer. Derived classes are expected to implement
     * movemement functionality with this callback.
     *
     * @param time The current time.
     */
    protected void updateKinematics(SimTime time) {

        // default implementation does nothing, override to suit your needs
    }


    /**
     * Performs an instantaneous right-handed rotation of the body about the Z axis. The results of
     * this call will be visible immediately through the bee's sensors. At the same time, the rotation
     * about the X and Y axes are reset to zero (emulating level flight) and the angular velocity is
     * set to zero.
     *
     * @param angle The angle of rotation (rad).
     */
    protected final void turn(final float angle) {

        body.activate();
        body.setAngularVelocity(new Vector3f());
        
        Transform orient = new Transform();
        orient.setIdentity();
        orient.setRotation(getTruthOrientation());

        Vector3f unitX = new Vector3f(1, 0, 0);
        orient.transform(unitX);

        float ang = angle;

        if (actuationErrTurnVar > 0) {
            ang += getRandom().nextGaussian() * actuationErrTurnVar;
        }

        float heading = (float)Math.atan2(unitX.y, unitX.x) + ang;
        Quat4f quat = new Quat4f();

        MatrixUtil.getRotation(MathUtil.eulerZYXtoDCM(0, 0, heading), quat);

        body.getWorldTransform(orient);
        orient.setRotation(quat);

        body.setWorldTransform(orient);
    }


    /**
     * Performs an instantaneous rotation of the body to point in the direction of the given point. The results of
     * this call will be visible immediately through the bee's sensors.
     *
     * @param point The point in space toward which this bee will be rotated (in the world frame).
     */
    protected final void turnToward(final Vector3f point) {

        body.activate();

        Vector3f currPos = getTruthPosition();
        Vector3f diff = new Vector3f();
        Vector3f unitX = new Vector3f(1, 0, 0);

        diff.sub(point, currPos);
        diff.normalize();

        Vector3f axis = new Vector3f();

        axis.cross(unitX, diff);
        axis.normalize();

        if (Float.isNaN(axis.length())) {
            axis = new Vector3f(0, 0, 1);
        }

        float angle = (float)Math.acos(unitX.dot(diff));

        if (Float.isNaN(angle)) {
            return;
        }

        if (actuationErrTurnVar > 0) {
            angle += getRandom().nextGaussian() * actuationErrTurnVar;
        }

        Transform trans = new Transform();
        Quat4f rot = new Quat4f();

        QuaternionUtil.setRotation(rot, axis, angle);

        body.getWorldTransform(trans);
        trans.setRotation(rot);

        body.setWorldTransform(trans);
    }


    /**
     * Sets the linear velocity of the bee. The results of this
     * call will not be seen immediately, but applied to future movement
     * and maintained until modified.
     *
     * <br/>
     * If the linear velocity is set to zero, the bee will fall to the ground due
     * to the effects of gravity. To counteract this, enable hover mode.
     *
     * @param vel The desired linear velocity of the bee for the next time step (m/s, in the body frame).
     */
    protected final void setDesiredLinearVelocity(final Vector3f vel) {
        desiredLinVel = vel;
    }


    /**
     * Gets the linear velocity that this bee's automated flight control is
     * attempting to maintain.
     *
     * @return The linear velocity (m/s in the body frame).
     */
    protected final Vector3f getDesiredLinearVelocity() {
        return new Vector3f(desiredLinVel);
    }


    /**
     * Enables/disables hover mode for this bee. If enabled, the bee's automated
     * flight control will apply a force to counteract gravity. This allows the
     * bee to have zero linear velocity and maintain a position off the ground.
     *
     * @param hover True if hovering should be enabled for the future, false otherwise.
     */
    protected final void setHovering(final boolean hover) {
        hovering = hover;
    }


    /**
     * Indicates if hovering mode is enabled or disabled.
     *
     * @return True if hover mode is enabled for the future, false otherwise.
     */
    protected final boolean isHovering() {
        return hovering;
    }


    /**
     * Indicates if this bee was setup to be influenced by forces coming from the wind model.
     *
     * @return True if wind effects are being modeled, false otherwise.
     */
    protected final boolean isUsingWind() {
        return useWind;
    }


    public final float getLength() {
        return length;
    }


    public final float getMass() {
        return mass;
    }


    @Override
    public String toString() {
        return "SimpleBee " + getModelId() + " " + getObjectId();
    }


    @Inject(optional = true)
    public final void setLength(@Named("length") final float length) {

        if (!isInitialized()) {
            this.length = length;
        }
    }


    @Inject(optional = true)
    public final void setMass(@Named("mass") final float mass) {

        if (!isInitialized()) {
            this.mass = mass;
        }
    }


    @Inject(optional = true)
    public final void setMaxAcceleration(@Named("max-acceleration") final float max) {

        if (!isInitialized()) {
            this.maxAccel = max;
        }
    }


    @Inject(optional = true)
    public final void setKinematicUpdateRate(@Named("kinematic-update-rate") final long rate) {

        if (!isInitialized()) {
            this.kinematicUpdateRate = rate;
        }
    }


    @Inject(optional = true)
    public final void setUseWind(@Named("use-wind") final boolean use) {
        this.useWind = use;
    }


    @Inject(optional = true)
    public final void setHoverEnergy(@Named("hover-energy") final double energy) {

        if (!isInitialized()) {
            this.hoverEnergy = energy;
        }
    }


    @Inject(optional = true)
    public final void setActuationEnergyRate(@Named("actuation-energy") final double energy) {

        if (!isInitialized()) {
            this.actuationEnergy = energy;
        }
    }


    @Inject(optional = true)
    public final void setActuationErrTurnVar(@Named("actuation-error-turn-var") final float err) {

        if (!isInitialized()) {
            this.actuationErrTurnVar = err;
        }
    }


    @Inject(optional = true)
    public final void setActuationDirTurnVar(@Named("actuation-error-dir-var") final float err) {

        if (!isInitialized()) {
            this.actuationErrDirVar = err;
        }
    }


    @Inject(optional = true)
    public final void setActuationVelTurnVar(@Named("actuation-error-vel-var") final float err) {

        if (!isInitialized()) {
            this.actuationErrVelVar = err;
        }
    }
}
