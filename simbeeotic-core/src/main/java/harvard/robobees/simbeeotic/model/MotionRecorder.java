package harvard.robobees.simbeeotic.model;


import com.bulletphysics.collision.shapes.CollisionShape;

import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Transform3D;
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

    private Set<MotionListener> listeners =
            new HashSet<MotionListener>();

    private static Logger logger = Logger.getLogger(MotionRecorder.class);


    /**
     * Set the details of the object shape and size. For the sake of any
     * {@link MotionListener}s, this must be called at least once prior to 
     * any call to {@link #updateKinematicState} or {@link #updateScale}.
     *
     * @param objectId The unique identifier of the object.
     * @param shape The shape and size of the object.
     */

    public void updateShape(int objectId, CollisionShape shape) {

        for (MotionListener listener : listeners) {

            try {
                listener.shapeUpdate(objectId, shape);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }


    /**
     * Set the details of the object's scale.
     *
     * @param objectId The unique identifier of the object.
     * @param scale The new scale of the object.
     */
    public void updateScale(int objectId, Vector3f scale) {

        for (MotionListener listener : listeners) {

            try {
                listener.scaleUpdate(objectId, scale);
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
    //For each listener, create CameraView representation
    public void addView(int cameraID, ImageComponent2D buf, Transform3D trans, int h, int w, float fl) {
        for (MotionListener listener : listeners) {

            try {
                listener.spawnCameraView(cameraID, buf, trans, w, h, fl);
            }
            catch(Exception e) {
                logger.warn("Caught an exception when updating MotionListener.", e);
            }
        }
    }

    //For each listener, update specified view
    public void updateView(int cameraID){
        for (MotionListener listener : listeners) {

            try {
                listener.renderCameraView(cameraID);
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
