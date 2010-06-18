package harvard.robobees.simbeeotic.environment;


import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import java.util.Map;

import harvard.robobees.simbeeotic.util.BoundingSphere;


/**
 * A container class that holds information about an object in the physical world
 * (obstacles, flowers, etc).
 *
 * @author bkate
 */
public class WorldObject {

    private int objectId;
    private Type type;
    private RigidBody body;
    private Map<String, Object> meta;

    /**
     * Enumerates the types of physical objects that populate the world.
     */
    public static enum Type {

        TERRAIN,
        OBSTACLE,
        FLOWER
    }


    public WorldObject(int objectId, Type type, RigidBody body, Map<String, Object> meta) {

        this.objectId = objectId;
        this.type = type;
        this.body = body;
        this.meta = meta;
    }


    /**
     * Gets the identifier of this physical object.
     *
     * @return The ID.
     */
    public int getObjectId() {
        return objectId;
    }


    /**
     * Gets the type of physical object.
     *
     * @return The enumerated type.
     */
    public Type getType() {
        return type;
    }


    /**
     * Gets the truth position of the object.
     *
     * @return The position of the object (in the world frame).
     */
    public Vector3f getTruthPosition() {
        return new Vector3f(body.getMotionState().getWorldTransform(new Transform()).origin);
    }


    /**
     * Gets the truth orientation of the object.
     *
     * @return The object's orientation (in the world frame).
     */
    public final Quat4f getTruthOrientation() {
        return body.getOrientation(new Quat4f());
    }


    /**
     * Gets the parameters of a sphere that encompasses the object.
     *
     * @return The bounding sphere for the object.
     */
    public BoundingSphere getTruthBoundingSphere() {

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
     * Gets the metadata associated with this object.
     *
     * @return The object's metadata.
     */
    public Map<String, Object> getMetadata() {
        return meta;
    }
}
