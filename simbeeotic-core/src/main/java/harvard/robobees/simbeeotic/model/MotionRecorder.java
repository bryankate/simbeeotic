package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.CollisionShape;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import java.util.HashSet;
import java.util.Set;
import java.awt.Color;
import java.awt.Image;

import org.apache.log4j.Logger;


/**
 * A class that coordinates the dissemination of motion information for the
 * objects in the simulation. Interested classes can register as a listener
 * of motion state updates via the {@link MotionListener} interface.
 *
 * @author bkate
 */
public class MotionRecorder {

    private Set<MotionListener> listeners = new HashSet<MotionListener>();

    private static Logger logger = Logger.getLogger(MotionRecorder.class);


    /**
     * Set the details of the object. For the sake of any {@link MotionListener}s, this must be
     * called at least prior to any call to {@link #updateKinematicState}.
     *
     * @param objectId The unique identifier of the object.
     * @param shape The shape of the object.
     */
    public void initializeObject(int objectId, CollisionShape shape) {

        for (MotionListener listener : listeners) {

            try {
                listener.initializeObject(objectId, shape);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }


    /**
     * Update thie kinematic state of an object (its position and orientation).
     *
     * @param objectId The unique identifier of the object.
     * @param position The position of the object (in the world frame).
     * @param orientation The orientation of the object (in the world frame).
     */
    public void updateKinematicState(int objectId, Vector3f position, Quat4f orientation) {

        for (MotionListener listener : listeners) {

            try {
                listener.stateUpdate(objectId, position, orientation);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }


    /**
     * Updates the color of an object.
     *
     * @param objectId The unique identifier of the object.
     * @param color The new color information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, Color color) {
        updateMetadata(objectId, color, null, null);
    }


    /**
     * Updates the color of an object.
     *
     * @param objectId The unique identifier of the object.
     * @param texture The new texture information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, Image texture) {
        updateMetadata(objectId, null, texture, null);
    }


    /**
     * Updates the label associated with an object.
     *
     * @param objectId The unique identifier of the object.
     * @param label The new label information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, String label) {
        updateMetadata(objectId, null, null, label);
    }


    /**
     * Updates the metadata associated with an object.
     *
     * @param objectId The unique identifier of the object.
     * @param color The new color information (may be null if no change is being made).
     * @param texture The new texture information (may be null if no change is being made).
     * @param label The new label information (may be null if no change is being made).
     */
    public void updateMetadata(int objectId, Color color, Image texture, String label) {

        for (MotionListener listener : listeners) {

            try {
                listener.metaUpdate(objectId, color, texture, label);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }


    /**
     * Adds a listener to the recorder.
     *
     * @param listener The motion listener to be added.
     */
    public void addListener(MotionListener listener) {
        listeners.add(listener);
    }


    /**
     * Removes a listener from the recorder.
     *
     * @param listener The motion listener to be removed.
     */
    public void removeListener(MotionListener listener) {
        listeners.remove(listener);
    }


    /**
     * Shuts down the recorder. This includes stopping any processing threads.
     */
    public void shutdown() {
        listeners.clear();
    }
}
