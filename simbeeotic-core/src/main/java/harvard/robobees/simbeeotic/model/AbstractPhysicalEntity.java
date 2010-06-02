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
import java.util.Map;
import java.util.HashMap;


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
    private Set<Integer> collisionListeners = new HashSet<Integer>();

    private Vector3f linearAccel;
    private Vector3f angularAccel;
    private Map<String, Vector3f> externalForces = new HashMap<String, Vector3f>();

    private float startX = 0.0f;    // m, geom center relative to world origin
    private float startY = 0.0f;    // m, geom center relative to world origin
    private float startZ = 0.0f;    // m, geom center relative to world origin


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


    /**
     * Retrieves an external force that was applied to this entity.
     *
     * @param name The name of the external force.
     *
     * @return The force that corresponds to the given name, or {@code null} if none exists.
     */
    public final Vector3f getExternalForce(String name) {
        return externalForces.get(name);
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


    protected final float getStartX() {
        return startX;
    }


    protected final float getStartY() {
        return startY;
    }


    protected final float getStartZ() {
        return startZ;
    }


    @Inject
    public final void setDynamicsWorld(@GlobalScope DiscreteDynamicsWorld world) {

        if (!isInitialized()) {
            this.dynWorld = world;
        }
    }


    @Inject(optional = true)
    public final void setStartX(@Named("start-x") final float x) {

        if (!isInitialized()) {
            this.startX = x;
        }
    }


    @Inject(optional = true)
    public final void setStartY(@Named("start-y") final float y) {

        if (!isInitialized()) {
            this.startY = y;
        }
    }


    @Inject(optional = true)
    public final void setStartZ(@Named("start-z") final float z) {

        if (!isInitialized()) {
            this.startZ = z;
        }
    }
}
