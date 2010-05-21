package harvard.robobees.simbeeotic.model.sensor;


import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.Contact;
import harvard.robobees.simbeeotic.model.EntityInfo;

import javax.vecmath.Vector3f;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


/**
 * A sensor that can detect if a flower is within range of a sensor that is pointing
 * away from the body into the environment.
 *
 * @author bkate
 */
public class FlowerSensor extends AbstractSensor {

    private DiscreteDynamicsWorld world;

    private float maxRange  = 1.0f;  // m;

    private static final float CONTACT_EPSILON = 0.01f;


    /**
     * Determines if there is a flower object within range and field of view of the sensor.
     *
     * @return True if a flower is detected, false otherwise.
     */
    public boolean isFlowerDetected() {
        return !senseFlowers().isEmpty();
    }


    /**
     * Determines if there is a flower object within range and field of view of the sensor.
     *
     * @return The meta-properties of the flower detected.
     */
    public Set<Map<String, Object>> detectFlowers() {
        return senseFlowers();
    }


    private Set<Map<String, Object>> senseFlowers() {

        Set<Map<String, Object>> flowerProps = new HashSet<Map<String, Object>>();

        // check if the sensor is actually in contact with another object.
        // if there is an object contacting the body in the vicinity of the sensor,
        // JBullet will actually see through the adjacent object to the next object. for example,
        // consider the body resting on a box that is sitting on the ground. if the sensor
        // is on the bottom of the body (which is contacting the box), the ray cast downward
        // will see through the box and hit the floor, even though the range to the closest
        // object should be 0. perhaps this is not the best strategy, but it works for most cases.
        for (Contact c : getHost().getContactPoints()) {

            Vector3f diff = new Vector3f();
            diff.sub(getOffset(), c.getBodyContactPoint());

            if (diff.length() <= CONTACT_EPSILON) {

                flowerProps.add(c.getContactMetadata());
                return flowerProps;
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

        if (isFlower(callback.getClosestObjectProps())) {
            flowerProps.add(callback.getClosestObjectProps());
        }

        return flowerProps;
    }


    public static boolean isFlower(final Map<String, Object> info) {

        if (info == null) {
            return false;
        }

        // todo: do this in a better way?
        Object prop = info.get("isFlower");

        return ((prop != null) && (Boolean)prop);
    }


    @Inject
    public final void setDynamicsWorld(@GlobalScope DiscreteDynamicsWorld world) {
        this.world = world;
    }


    @Inject(optional = true)
    public final void setMaxRange(@Named("max-range") final float maxRange) {
        this.maxRange = maxRange;
    }


    /**
     * A callback that handles ray intersections with objects in the world. It records
     * the minimum distance to any object.
     */
    private static class RayCallback extends CollisionWorld.RayResultCallback {

        private float rayLength;
        private float minDistance = Float.POSITIVE_INFINITY;
        private EntityInfo minObjectInfo;


        public RayCallback(float length) {
            rayLength = length;
        }

        public float addSingleResult(CollisionWorld.LocalRayResult rayResult, boolean normalInWorldSpace) {

            float dist = rayResult.hitFraction * rayLength;

            if (dist < minDistance) {

                minDistance = dist;
                minObjectInfo = (EntityInfo)rayResult.collisionObject.getUserPointer();
            }

            return rayResult.hitFraction;
        }

        public Map<String, Object> getClosestObjectProps() {
            return minObjectInfo.getMetadata();
        }
    }
}
