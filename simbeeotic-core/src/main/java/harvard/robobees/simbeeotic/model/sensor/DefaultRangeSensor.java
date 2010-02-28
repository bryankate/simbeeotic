package harvard.robobees.simbeeotic.model.sensor;


import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.model.Contact;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;


/**
 * @author bkate
 */
public class DefaultRangeSensor extends AbstractSensor implements RangeSensor {

    private DynamicsWorld world;

    private float sigma = 0.05f;    // m
    private float minRange = 0.1f;  // m
    private float maxRange = 5.0f;  // m

    private static final float CONTACT_EPSILON = 0.01f;
    

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
            diff.sub(getOffset(), c.getBodyContactPoint());

            if (diff.length() <= CONTACT_EPSILON) {
                return Float.POSITIVE_INFINITY;
            }
        }


        // we need to find the sensor's position and pointing vector
        // (in world coordinates) given the body's current orientation
        Vector3f rotatedOffset = new Vector3f(getOffset());
        Vector3f rotatedPointing = new Vector3f(getPointing());

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
        float range = addNoise(callback.getMinDIstance(), sigma);

        if ((range < minRange) || (range > maxRange)) {
            range = Float.POSITIVE_INFINITY;
        }

        return range;
    }


    @Inject
    public final void setDynamicsWorld(@GlobalScope DynamicsWorld world) {
        this.world = world;
    }


    @Inject(optional = true)
    public final void setSigma(@Named(value = "sigma") final float sigma) {
        this.sigma = sigma;
    }


    @Inject(optional = true)
    public final void setMinRange(@Named(value = "min-range") final float minRange) {
        this.minRange = minRange;
    }


    @Inject(optional = true)
    public final void setMaxRange(@Named(value = "max-range") final float maxRange) {
        this.maxRange = maxRange;
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
