package harvard.robobees.simbeeotic.model;


import org.apache.log4j.Logger;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.MatrixUtil;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;

import harvard.robobees.simbeeotic.model.sensor.PositionSensor;
import harvard.robobees.simbeeotic.model.sensor.Gyroscope;
import harvard.robobees.simbeeotic.model.sensor.Accelerometer;
import harvard.robobees.simbeeotic.model.sensor.RangeSensor;
import harvard.robobees.simbeeotic.model.sensor.DefaultRangeSensor;
import harvard.robobees.simbeeotic.model.sensor.DefaultGyroscope;
import harvard.robobees.simbeeotic.model.sensor.DefaultAccelerometer;
import harvard.robobees.simbeeotic.model.sensor.DefaultPositionSensor;
import harvard.robobees.simbeeotic.model.sensor.ContactSensor;
import harvard.robobees.simbeeotic.model.sensor.DefaultContactSensor;
import harvard.robobees.simbeeotic.model.sensor.Compass;
import harvard.robobees.simbeeotic.model.sensor.DefaultCompass;
import harvard.robobees.simbeeotic.model.sensor.FlowerSensor;
import harvard.robobees.simbeeotic.util.LinearMathUtil;
import harvard.robobees.simbeeotic.environment.PhysicalConstants;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.comms.PropagationModel;
import harvard.robobees.simbeeotic.comms.Radio;
import harvard.robobees.simbeeotic.comms.DefaultRadio;
import harvard.robobees.simbeeotic.comms.AbstractRadio;


/**
 * A simple bee platform that can be used as a starting point for creating
 * more complicated bee models. This base class creates a physical body in
 * the world (a sphere) and has primitive flight controls to simplify
 * movement. A number of sensors are defined along with a radio, but the
 * logic of the bee is left unimplemented.
 *
 * @author bkate
 */
public abstract class SimpleBee extends AbstractPhysicalModel {

    // parameters
    private float length = 0.2f;                 // m
    private float mass = 0.128f;                 // g
    private float positionSensorSigma = 0.5f;    // m
    private float compassSigma = 0.0016f;        // degrees
    private float gyroSigma = 0.0085f;           // rad/s
    private float accelerometerSigma = 0.005f;   // m/s^2
    private float contactSensorRadius = 0.005f;  // m
    private float rangeSensorMin = 0.1f;         // m
    private float rangeSensorMax = 5.0f;         // m
    private float rangeSensorSigma = 0.05f;      // m
    private float flowerSensorMax = 1.0f;        // m
    private float radioPowerMax = 50;            // mW

    // sensors
    protected PositionSensor positionSensor;
    protected Compass compass;
    protected Gyroscope gyro;
    protected Accelerometer accelerometer;
    protected ContactSensor contactSensorBottom;
    protected RangeSensor rangeSensorTop;
    protected RangeSensor rangeSensorBottom;
    protected RangeSensor rangeSensorLeft;
    protected RangeSensor rangeSensorRight;
    protected RangeSensor rangeSensorFront;
    protected RangeSensor rangeSensorBack;
    protected FlowerSensor flowerSensor;

    // comms
    private PropagationModel commModel;
    protected AbstractRadio radio;

    // physical state
    private RigidBody body;
    private Vector3f desiredLinVel = new Vector3f();
    private Vector3f hoverForce;
    private boolean hovering = false;

    
    private static Logger logger = Logger.getLogger(SimpleBee.class);


    /**
     * Executes the custom logic for this bee.
     */
    protected abstract void applyLogic(final double currTime);


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

        startTransform.origin.set(new Vector3f(getStartX(), getStartY(), getStartZ() + halfLength));

        MotionState myMotionState = new DefaultMotionState(startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState,
                                                                         colShape, localInertia);

        // modify the thresholds for deactivating the bee
        // because it moves at a much smaller scale
        rbInfo.linearSleepingThreshold = 0.08f;  // m/s
        rbInfo.angularSleepingThreshold = 0.1f;  // rad/s

        body = new RigidBody(rbInfo);

        // todo: put the bee's properties into the entity info?
        body.setUserPointer(new EntityInfo());

        world.addRigidBody(body, COLLISION_BEE, (short)(COLLISION_TERRAIN | COLLISION_BEE | COLLISION_FLOWER));

        hoverForce = new Vector3f(0, 0, getMass() * -PhysicalConstants.EARTH_GRAVITY);

        // setup sensors
        positionSensor = new DefaultPositionSensor(this, positionSensorSigma, getRandom().nextLong());
        compass = new DefaultCompass(this, compassSigma, getRandom().nextLong());
        gyro = new DefaultGyroscope(this, gyroSigma, getRandom().nextLong());
        accelerometer = new DefaultAccelerometer(this, accelerometerSigma, getRandom().nextLong());
        contactSensorBottom = new DefaultContactSensor(this, new Vector3f(0, 0, -halfLength), contactSensorRadius, getRandom().nextLong());

        rangeSensorTop = new DefaultRangeSensor(this, new Vector3f(0, 0, halfLength), new Vector3f(0, 0, 1), world,
                                                rangeSensorMin, rangeSensorMax, rangeSensorSigma, getRandom().nextLong());

        rangeSensorBottom = new DefaultRangeSensor(this, new Vector3f(0, 0, -halfLength), new Vector3f(0, 0, -1), world,
                                                   rangeSensorMin, rangeSensorMax, rangeSensorSigma, getRandom().nextLong());

        rangeSensorLeft = new DefaultRangeSensor(this, new Vector3f(0, -halfLength, 0), new Vector3f(0, -1, 0), world,
                                                 rangeSensorMin, rangeSensorMax, rangeSensorSigma, getRandom().nextLong());

        rangeSensorRight = new DefaultRangeSensor(this, new Vector3f(0, halfLength, 0), new Vector3f(0, 1, 0), world,
                                                  rangeSensorMin, rangeSensorMax, rangeSensorSigma, getRandom().nextLong());

        rangeSensorFront = new DefaultRangeSensor(this, new Vector3f(halfLength, 0, 0), new Vector3f(1, 0, 0), world,
                                                  rangeSensorMin, rangeSensorMax, rangeSensorSigma, getRandom().nextLong());

        rangeSensorBack = new DefaultRangeSensor(this, new Vector3f(-halfLength, 0, 0), new Vector3f(-1, 0, 0), world,
                                                 rangeSensorMin, rangeSensorMax, rangeSensorSigma, getRandom().nextLong());

        flowerSensor = new FlowerSensor(this, new Vector3f(0, 0, -halfLength), new Vector3f(0, 0, -1), world, flowerSensorMax);

        // setup comms
        radio = new DefaultRadio(commModel, this, radioPowerMax);
        commModel.addRadio(radio);

        return body;
    }


    /** {@inheritDoc} */
    @Override
    public final void update(double currTime) {

        applyLogic(currTime);

        if (desiredLinVel.length() > 0) {
            body.activate();
        }

        // apply changes to motion for the next timestep
        Vector3f impulse = new Vector3f(desiredLinVel);
        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(getTruthOrientation());

        trans.transform(impulse);
        impulse.sub(getTruthLinearVelocity());
        impulse.scale(getMass());

        // apply an instantaneous force to get the desired velocity change
        body.applyCentralImpulse(impulse);

        // account for gravity (or not)
        if (hovering) {

            body.activate();
            applyForce(hoverForce);
        }
    }


    /**
     * Performs an instantaneous right-handed rotation of the body about the Z axis. The results of
     * this call will be visible immediately through the bee's sensors. At the same time, the rotation
     * about the X and Y axes are reset to 0 (emulating level flight).
     *
     * @param angle The angle of rotation (rad).
     */
    protected final void turn(final float angle) {

        body.activate();
        
        Transform orient = new Transform();
        orient.setIdentity();
        orient.setRotation(getTruthOrientation());

        Vector3f unitX = new Vector3f(1, 0, 0);
        orient.transform(unitX);

        float heading = (float)Math.atan2(unitX.y, unitX.x) + angle;
        Quat4f quat = new Quat4f();

        MatrixUtil.getRotation(LinearMathUtil.eulerZYXtoDCM(0, 0, heading), quat);

        body.getWorldTransform(orient);
        orient.setRotation(quat);

        body.setWorldTransform(orient);
    }


    /**
     * Sets the linear velocity of the bee for the next time step. The results of this
     * call will not be seen immediately, but applied to future movement.
     *
     * @note If the linear velocity is set to zero, the bee will fall to the ground due
     *       to the effects of gravity. To counteract this, enable hover mode.
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
     * @param hover True if hovering should be enabled for the next time step, false otherwise.
     */
    protected final void setHovering(final boolean hover) {
        hovering = hover;
    }


    /**
     * Indicates if hovering mode is enabled or disabled.
     *
     * @return True if hover mode is enabled for the next time step, false otherwise.
     */
    protected boolean isHovering() {
        return hovering;
    }


    public final float getLength() {
        return length;
    }


    public final float getMass() {
        return mass;
    }


    @Inject
    public final void setCommModel(@GlobalScope PropagationModel commModel) {

        if (!isInitialized()) {
            this.commModel = commModel;
        }
    }


    @Inject(optional = true)
    public final void setLength(@Named(value = "length") final float length) {

        if (!isInitialized()) {
            this.length = length;
        }
    }


    @Inject(optional = true)
    public final void setMass(@Named(value = "mass") final float mass) {

        if (!isInitialized()) {
            this.mass = mass;
        }
    }


    @Inject(optional = true)
    public final void setPositionSensorSigma(@Named(value = "position-sensor-sigma") final float positionSensorSigma) {
        
        if (!isInitialized()) {
            this.positionSensorSigma = positionSensorSigma;
        }
    }


    @Inject(optional = true)
    public final void setGyroSigma(@Named(value = "gyro-sigma") final float gyroSigma) {

        if (!isInitialized()) {
            this.gyroSigma = gyroSigma;
        }
    }


    @Inject(optional = true)
    public final void setAccelerometerSigma(@Named(value = "accelerometer-sigma") final float accelerometerSigma) {

        if (!isInitialized()) {
            this.accelerometerSigma = accelerometerSigma;
        }
    }


    @Inject(optional = true)
    public final void setCompassSigma(@Named(value = "compass-sigma") final float compassSigma) {

        if (!isInitialized()) {
            this.compassSigma = compassSigma;
        }
    }


    @Inject(optional = true)
    public final void setContactSensorRadius(@Named(value = "contact-sensor-radius") final float contactSensorRadius) {

        if (!isInitialized()) {
            this.contactSensorRadius = contactSensorRadius;
        }
    }


    @Inject(optional = true)
    public final void setRangeSensorMin(@Named(value = "range-sensor-min") final float rangeSensorMin) {

        if (!isInitialized()) {
            this.rangeSensorMin = rangeSensorMin;
        }
    }


    @Inject(optional = true)
    public final void setRangeSensorMax(@Named(value = "range-sensor-max") final float rangeSensorMax) {

        if (!isInitialized()) {
            this.rangeSensorMax = rangeSensorMax;
        }
    }


    @Inject(optional = true)
    public final void setRangeSensorSigma(@Named(value = "range-sensor-sigma") final float rangeSensorSigma) {

        if (!isInitialized()) {
            this.rangeSensorSigma = rangeSensorSigma;
        }
    }


    @Inject(optional = true)
    public final void setFlowerSensorMax(@Named(value = "flower-sensor-max") final float flowerSensorMax) {

        if (!isInitialized()) {
            this.flowerSensorMax = flowerSensorMax;
        }
    }


    @Inject(optional = true)
    public final void setRadioPowerMax(@Named(value = "radio-power-max") final float radioPowerMax) {

        if (!isInitialized()) {
            this.radioPowerMax = radioPowerMax;
        }
    }
}
