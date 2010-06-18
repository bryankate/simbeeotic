package harvard.robobees.simbeeotic.model;


import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.util.BoundingSphere;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.Set;
import java.util.HashSet;


/**
 * A convenience class that implements some of the base functionality that is
 * common to all {@link Model} {@link PhysicalEntity} implementations. This class encapsulates
 * a JBullet {@link RigidBody} and gives access to it via the {@link PhysicalEntity}
 * facade (with some extra functionality introduced in this class).
 *
 * @author bkate
 */
public abstract class AbstractPhysicalEntity extends AbstractModel implements PhysicalEntity {

    private DiscreteDynamicsWorld dynWorld;
    private RigidBody body;
    private MotionRecorder recorder;
    private Set<Integer> collisionListeners = new HashSet<Integer>();

    private Vector3f linearAccel;
    private Vector3f angularAccel;

    private int objectId;
    private Vector3f startPos = new Vector3f();    // m, geom center relative to world origin


    /**
     * Initializes the body in the physical world.
     *
     * @param world The Bullet physical world into which the entity should be inserted.
     *
     * @return The newly constructed rigid body.
     */
    protected abstract RigidBody initializeBody(DiscreteDynamicsWorld world);


    /** {@inheritDoc} */
    @Override
    public void initialize() {

        super.initialize();

        body = initializeBody(dynWorld);

        ((EntityInfo)body.getUserPointer()).getCollisionListeners().addAll(collisionListeners);

        linearAccel = new Vector3f(0, 0, 0);
        angularAccel = new Vector3f(0, 0, 0);
    }


    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }


    /**
     * {@inheritDoc}
     *
     * This implementation saves off the linear and angular acceleration (as derived from
     * the total force currently acting on the object).
     */
    @Override
    protected void checkpoint() {

        super.checkpoint();

        linearAccel.scale(body.getInvMass(), body.getTotalForce());
        angularAccel.scale(body.getInvMass(), body.getTotalTorque());
    }


    /** {@inheritDoc} */
    @Override
    public final void applyForce(final Vector3f F) {
        body.applyCentralForce(F);
    }


    /** {@inheritDoc} */
    @Override
    public final void applyForce(final Vector3f F, final Vector3f offset) {
        body.applyForce(F, offset);
    }


    /** {@inheritDoc} */
    @Override
    public final void applyImpulse(final Vector3f F) {
        body.applyCentralImpulse(F);
    }


    /** {@inheritDoc} */
    @Override
    public final void applyImpulse(final Vector3f F, final Vector3f offset) {
        body.applyImpulse(F, offset);
    }


    /** {@inheritDoc} */
    @Override
    public final void applyTorque(final Vector3f T) {
        body.applyTorque(T);
    }


    /** {@inheritDoc} */
    @Override
    public final void applyTorqueImpulse(final Vector3f T) {
        body.applyTorqueImpulse(T);
    }


    /** {@inheritDoc} */
    public final void clearForces() {
        body.clearForces();
    }


    /** {@inheritDoc} */
    @Override
    public final Vector3f getTruthPosition() {
        return new Vector3f(body.getMotionState().getWorldTransform(new Transform()).origin);
    }


    /** {@inheritDoc} */
    @Override
    public final Quat4f getTruthOrientation() {
        return body.getOrientation(new Quat4f());
    }


    /** {@inheritDoc} */
    @Override
    public final Vector3f getTruthLinearVelocity() {

        if (isActive()) {
            return body.getLinearVelocity(new Vector3f());
        }

        return new Vector3f();
    }


    /** {@inheritDoc} */
    @Override
    public final Vector3f getTruthAngularVelocity() {

        if (isActive()) {
            return body.getAngularVelocity(new Vector3f());
        }

        return new Vector3f();
    }


    /** {@inheritDoc} */
    @Override
    public final Vector3f getTruthLinearAcceleration() {

        if (isActive()) {
            return new Vector3f(linearAccel);
        }

        return new Vector3f();
    }


    /** {@inheritDoc} */
    @Override
    public final Vector3f getTruthAngularAcceleration() {
        return new Vector3f(angularAccel);
    }


    /** ${@inheritDoc} */
    @Override
    public final BoundingSphere getTruthBoundingSphere() {

        Vector3f center = new Vector3f();
        float[] radPtr = new float[1];

        body.getCollisionShape().getBoundingSphere(center, radPtr);

        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(getTruthOrientation());

        trans.transform(center);

        center.add(getTruthPosition());

        return new BoundingSphere(center, radPtr[0]);

    }


    /**
     * {@inheritDoc}
     *
     * This does not return a copy of the contact points, so an effort must be made to
     * not mutate the points (vectors). The properties can be altered as needed.
     */
    @Override
    public final Set<Contact> getContactPoints() {
        return ((EntityInfo)body.getUserPointer()).getContactPoints();
    }


    /** {@inheritDoc} */
    public void addCollisionListener(int modelId) {

        if (isInitialized()) {
            ((EntityInfo)body.getUserPointer()).getCollisionListeners().add(modelId);
        }
        else {
            collisionListeners.add(modelId);
        }
    }


    /**
     * Determines if this object is active in the physics simulation.
     *
     * @return True if the object is active (has moved recently), false if stationary.
     */
    protected final boolean isActive() {
        return body.isActive();
    }


    /**
     * Gets the identifier that is to be used when sending updates to the {@link MotionRecorder}.
     *
     * @return The unique identifier of this entity with respect to the motion recorder.
     */
    protected final int getObjectId() {
        return objectId;
    }


    /**
     * Gets the starting position of this model, as set by the user.
     *
     * @return The starting position, in the world frame.
     */
    protected final Vector3f getStartPosition() {
        return startPos;
    }


    /**
     * Gets the global instance of {@link MotionRecorder} that can be used to update the state
     * of this object when it changes.
     *
     * @return The motion recorder being used.
     */
    protected final MotionRecorder getMotionRecorder() {
        return recorder;
    }


    @Inject
    public final void setDynamicsWorld(@GlobalScope DiscreteDynamicsWorld world) {

        if (!isInitialized()) {
            this.dynWorld = world;
        }
    }


    @Inject
    public final void setMotionRecorder(@GlobalScope final MotionRecorder recorder) {

        if (!isInitialized()) {
            this.recorder = recorder;
        }
    }


    @Inject
    public final void setObjectId(@Named("object-id") final int id) {

        if (!isInitialized()) {
            this.objectId = id;
        }
    }


    @Inject(optional = true)
    public final void setStartPosition(@Named("start-position") final Vector3f pos) {

        if (!isInitialized()) {
            this.startPos = pos;
        }
    }
}
