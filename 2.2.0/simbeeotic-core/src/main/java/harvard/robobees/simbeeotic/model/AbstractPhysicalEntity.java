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
import java.util.Random;
import java.util.Set;


/**
 * A convenience class that implements some of the base functionality that is
 * common to all {@link PhysicalEntity} implementations. This class encapsulates
 * a JBullet {@link RigidBody} and gives access to it via the {@link PhysicalEntity}
 * facade (with some extra functionality introduced in this class).
 *
 * @author bkate
 */
public abstract class AbstractPhysicalEntity implements PhysicalEntity {

    private DiscreteDynamicsWorld dynWorld;
    private RigidBody body;

    private Random rand;

    private Vector3f linearAccel;
    private Vector3f angularAccel;
    private Vector3f lastLinearVel;
    private Vector3f lastAngularVel;

    private float startX = 0.0f;    // m, geom center relative to world origin
    private float startY = 0.0f;    // m, geom center relative to world origin
    private float startZ = 0.0f;    // m, geom center relative to world origin

    private boolean initialized = false;


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

        initialized = true;

        body = initializeBody(dynWorld);

        linearAccel = new Vector3f(0, 0, 0);
        angularAccel = new Vector3f(0, 0, 0);
        lastLinearVel = body.getLinearVelocity(new Vector3f());
        lastAngularVel = body.getAngularVelocity(new Vector3f());
    }


    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }


    protected final boolean isInitialized() {
        return initialized;
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
    public final void applyTorque(final Vector3f T) {
        body.applyTorque(T);
    }


    /**
     * Returns the random number generator associated with this entity. The generator
     * has been seeded deterministically so that it produces repeatable number streams
     * if a scenario is executed multiple times.
     *
     * @return The seeded random number begerator for this entity.
     */
    public Random getRandom() {
        return rand;
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


    /** {@inheritDoc} */
    @Override
    public final void sampleKinematics(final double timeStep) {

        if (isActive()) {

            Vector3f currLinearVel = getTruthLinearVelocity();
            Vector3f currAngularVel = getTruthAngularVelocity();

            // delta velocity over the last time step
            linearAccel.sub(currLinearVel, lastLinearVel);
            angularAccel.sub(currAngularVel, lastAngularVel);

            // scale by time step to get a reading in m/s^2
            linearAccel.scale(1.0f / (float)timeStep);
            angularAccel.scale(1.0f / (float)timeStep);

            lastLinearVel = currLinearVel;
            lastAngularVel = currAngularVel;
        }
        else {

            // not moving, reset sampled acceleration
            lastLinearVel = new Vector3f();
            lastAngularVel = new Vector3f();
            linearAccel = new Vector3f();
            angularAccel = new Vector3f();
        }
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

        if (!initialized) {
            this.dynWorld = world;
        }
    }


    @Inject
    public final void setRandomSeed(@Named("random-seed") final long seed) {

        if (!initialized) {
            this.rand = new Random(seed);
        }
    }


    @Inject(optional = true)
    public final void setStartX(@Named(value = "start-x") final float x) {

        if (!initialized) {
            this.startX = x;
        }
    }


    @Inject(optional = true)
    public final void setStartY(@Named(value = "start-y") final float y) {

        if (!initialized) {
            this.startY = y;
        }
    }


    @Inject(optional = true)
    public final void setStartZ(@Named(value = "start-z") final float z) {

        if (!initialized) {
            this.startZ = z;
        }
    }
}
