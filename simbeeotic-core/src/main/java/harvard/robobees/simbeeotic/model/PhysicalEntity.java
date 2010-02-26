package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.util.BoundingSphere;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import java.util.Set;


/**
 * An interface describing the base functionality of an object that has a physical
 * presence in the simulation.
 *
 * @author bkate
 */
public interface PhysicalEntity {

    public static final short COLLISION_NONE    = 0;
    public static final short COLLISION_BEE     = 1;
    public static final short COLLISION_FLOWER  = 2;
    public static final short COLLISION_HIVE    = 4;
    public static final short COLLISION_TERRAIN = 8;
    public static final short COLLISION_ALL     = Short.MAX_VALUE;


    /**
     * Initializes the physical entity (shape, mass, position, etc).
     */
    public void initialize();


    /**
     * Destroys the physical entity.
     */
    public void destroy();


    /**
     * Applies a force to the body (to take effect at the next simulation step). The
     * force is applied to the body's center of mass and is in effect (constant) for the
     * duration of the time step, after which it is cleared (removed).
     *
     * @param F The force to be applied (Newtons, in the world frame).
     */
    public void applyForce(final Vector3f F);


    /**
     * Applies a force to the body (to take effect at the next physics simulation step). The
     * force is applied to the given offset position and is in effect (constant) for the
     * duration of the time step, after which it is cleared (removed).
     *
     * @param F The force to be applied (Newtons, in the world frame).
     * @param offset The position on the body where the force will be applied (in the body frame).
     */
    public void applyForce(final Vector3f F, final Vector3f offset);


    /**
     * Applies a torque (moment) about the body axes (to take effect at the next
     * simulation step). The torque is constant over the time step and is
     * cleared at the end of the step.
     *
     * @param T The torque (in Newton-meters) to be applied about the body's center of mass.
     */
    public void applyTorque(final Vector3f T);


    /**
     * Gets the truth position of the entity, (x, y, z), in meters,
     * relative to the world origin.
     *
     * @return The current truth position vector.
     */
    public Vector3f getTruthPosition();


    /**
     * Gets the truth orientation of the entity as a Quaternion.
     *
     * @return The quaternion that represents the rotation of the body.
     */
    public Quat4f getTruthOrientation();


    /**
     * Gets the truth linear velocity vector, in m/s. The velocity
     * is relative to the world coordinate frame, not the body.
     *
     * @return The current linear velocity of the entity.
     */
    public Vector3f getTruthLinearVelocity();


    /**
     * Gets the truth angular velocity about the body axes (x, y, z),
     * in rad/s, of the entity.
     *
     * @return The current angular acceleration vector of the entity.
     */
    public Vector3f getTruthAngularVelocity();


    /**
     * Gets the truth linear acceleration vector of the entity,
     * in m/s^2, in the world reference frame.
     *
     * @return The truth linear acceleration vector.
     */
    public Vector3f getTruthLinearAcceleration();


    /**
     * Gets the truth angular acceleration about the body axes (x, y, z),
     * in rad/s^2.
     *
     * @return The current angular acceleration of the entity.
     */
    public Vector3f getTruthAngularAcceleration();


    /**
     * Gets a current bounding sphere for the entity (in world coordinates), using the
     * truth position and orientation.
     *
     * @return The current bounding sphere.
     */
    public BoundingSphere getTruthBoundingSphere();


    /**
     * Samples the kinematic state and calculates the linear and angular
     * acceleration of the entity over the last time step. The JBullet physics
     * engine does not allow direct access to this information, so sampling
     * is used.
     *
     * This should be called at every simulation step.
     *
     * @param timeStep The amount of time that has elapsed since the last sample.
     */
    public void sampleKinematics(final float timeStep);


    /**
     * Gets the current set of contact points, as reported by the physics engine.
     *
     * @return The set of contacts.
     */
    public Set<Contact> getContactPoints();

}
