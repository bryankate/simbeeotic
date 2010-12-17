package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.CollisionShape;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import java.awt.*;


/**
 * An interface that allows an object to be informed about the kinematic
 * state of objects in the simulation.
 *
 * @author bkate
 */
public interface MotionListener {

    /**
     * Supplies the shape and size information for an object.
     *
     * @param objectId The unique identifier for this object.
     * @param shape The shape of the object. It can be queried to determine its
     *              specific type and then downcast.
     */
    public void shapeUpdate(int objectId, CollisionShape shape);


    /**
     * Supplies an update that scales the size of an object, relative to its original size.
     *
     * @param objectId The unique identifier for this object.
     * @param scale The scale of the object.
     */
    public void scaleUpdate(int objectId, Vector3f scale);


    /**
     * Updates the kinematic state of an object.
     *
     * @param objectId The unique identifier of the object.
     * @param position The new position of the object's center (in the world frame).
     * @param orientation The new orientation (in the world frame).
     */
    public void stateUpdate(int objectId, Vector3f position, Quat4f orientation);


    /**
     * Updates the metadata associated with an object.
     *
     * @param objectId The unique identifier of the object.
     * @param color The color update for the object (may be null if no change is being made from previous value).
     * @param texture The texture update for the object (may be null if no change is being made from previous value).
     * @param label The label update for the object (may be null if no change is being made from previous value).
     */
    public void metaUpdate(int objectId, Color color, Image texture, String label);

}
