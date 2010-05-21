package harvard.robobees.simbeeotic.model.sensor;


import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.Contact;


/**
 * @author bkate
 */
public class DefaultRangeSensor extends AbstractSensor implements RangeSensor {

    private DynamicsWorld world;

    private Vector3f offset;
    private Vector3f pointing;
    private float minRange;
    private float maxRange;

    private static final float CONTACT_EPSILON = 0.01f;
    

    /**
     * Standard constructor.
     *
     * @param host The physical entity to which the sensor is being attached.
     * @param offset The offset, relative to the host's body origin, of the sensor.
     * @param pointing The pointing vector, relative to the coordinate frame of the offset position.
     * @param world The physics world in which the sensor is sensing.
     * @param minRange The minimum range for which this sensor is calibrated (meters).
     * @param maxRange The maximum range for which this sensor is calibrated (meters).
     * @param sigma The standard deviation of the sensor error (meters).
     * @param seed The seed for the random number generator, used for adding noise to readings.
     */
    public DefaultRangeSensor(PhysicalEntity host, Vector3f offset, Vector3f pointing, DynamicsWorld world,
                              float minRange, float maxRange, float sigma, long seed) {

        super(host, seed, sigma);

        this.world = world;

        this.offset = offset;
        this.pointing = pointing;
        this.pointing.normalize();

        this.minRange = minRange;
        this.maxRange = maxRange;
    }


    /** {@inheritDoc} */
    public float getRange() {

        // check if the range sensor is actually in contact with another object.
        // if there is an object contacting the body in the vicinity of the range sensor,
        // JBullet will actually see through the adjacent object to the next object. for example,
        // consider the body resting on a box that is sitting on the ground. if the range sensor
        // is on the bottom of the body (which is contacting the box), the ray cast downward
        // will see through the box and hit the floor, even though the range to the closest
        // object should be 0. perhaps this is not the best strategy, but it works for most cases.
        for (Contact c : getHost().getContactPoints()) {

            Vector3f diff = new Vector3f();
            diff.sub(offset, c.getBodyContactPoint());

            if (diff.length() <= CONTACT_EPSILON) {
                return Float.POSITIVE_INFINITY;
            }
        }


        // we need to find the sensor's position and pointing vector
        // (in world coordinates) given the body's current orientation
        Vector3f rotatedOffset = new Vector3f(offset);
        Vector3f rotatedPointing = new Vector3f(pointing);

        rotatedPointing.scale(maxRange);

        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(getHost().getTruthOrientation());

        trans.transform(rotatedOffset);
        trans.transform(rotatedPointing);

        Vector3f from = new Vector3f(getHost().getTruthPosition());
        Vector3f to = new Vector3f(getHost().getTruthPosition());

        from.add(rotatedOffset);
        to.add(rotatedOffset);
        to.add(rotatedPointing);

        // collide the ray with the world and see what objects are intersected
        RayCallback callback = new RayCallback(maxRange);

        world.rayTest(from, to, callback);

        // add noise and check bounds
        float range = addNoise(callback.getMinDIstance());

        if ((range < minRange) || (range > maxRange)) {
            range = Float.POSITIVE_INFINITY;
        }

        return range;
    }


    /**
     * A callback that handles ray intersections with objects in the world. It records
     * the minimum distance to any object.
     */
    private static class RayCallback extends CollisionWorld.RayResultCallback {

        private float rayLength;
        private float minDistance = Float.POSITIVE_INFINITY;


        public RayCallback(float length) {
            rayLength = length;
        }

        public float addSingleResult(CollisionWorld.LocalRayResult rayResult, boolean normalInWorldSpace) {

            float dist = rayResult.hitFraction * rayLength;

            if (dist < minDistance) {
                minDistance = dist;
            }

            return rayResult.hitFraction;
        }

        public float getMinDIstance() {
            return minDistance;
        }
    }
}
