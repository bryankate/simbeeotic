package harvard.robobees.simbeeotic.model.sensor;


import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public interface Accelerometer {

    /**
     * Gets the linear acceleration of the body, as measured by
     * a 3-axis accelerometer. The readings are relative to the body frame,
     * meaning that the reading in the x direction is aligned down the
     * x axis of the body, regardless of the body's orientation in the
     * physical world.
     * 
     * @return The acceleration along each body axis, (x, y, z), in m/s^2.
     */
    public Vector3f getLinearAcceleration();
}
