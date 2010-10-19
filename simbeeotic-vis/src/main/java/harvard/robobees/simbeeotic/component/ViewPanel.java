package harvard.robobees.simbeeotic.component;


import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;


/**
 * An interface that defines the behavior of a viewing panel that is used to
 * display a 3D world. Eac implementation of this class must support the minimal
 * features defined in this interface.
 * <p/>
 * Of course, this interface is geared
 * toward a Java3D implementation at this point in time, but it could change
 * to be more generic in the future.
 * 
 * @author bkate
 */
public interface ViewPanel {

    /**
     * Creates a new window that shows the world from the perspective of
     * a simulated object. A camera is set at the obejct's center, pointing
     * in the direction of it's positive body X axis.
     *
     * @param objectId The ID of the object for which the view is to be spawned.
     */
    public void spawnObjectView(int objectId);


    /**
     * Sets the position and orientation of the camera used in the main 3D panel.
     *
     * @param from The position of the camera.
     * @param to A point in the world upon which the camera is focused.
     * @param up The unit vector indicating the direction that is "up".
     */
    public void setMainView(Point3d from, Point3d to, Vector3d up);


    /**
     * Sets the transform (position and orientation) of the camera used in the
     * main 3D panel.
     *
     * @param t3d The new camera transform.
     */
    public void setMainViewTransform(Transform3D t3d);


    /**
     * Gets the transform (position and orientation) of the camera that
     * is used in the main 3D panel.
     *
     * @return The main camera's transform.
     */
    public Transform3D getMainViewTransform();


    /**
     * Toggles the visibility of object labels.
     *
     * @param visible True if visible, false otherwise.
     */
    public void setLabelsVisible(boolean visible);


    /**
     * Called by the parent frame when it is disposed. The intent is to
     * close any child frames that were spawned.
     */
    public void dispose();
}
