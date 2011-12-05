package harvard.robobees.simbeeotic.model.sensor;


import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public interface Accelerometer {

    /**
     * <p>
     * Gets the linear acceleration of the body, as measured by
     * a 3-axis accelerometer. The readings are relative to the body frame,
     * meaning that the reading in the x direction is aligned down the
     * x axis of the body, regardless of the body's orientation in the
     * physical world.
     * </p>
     * <p>
     * The accelerometer is "calibrated" to return the
     * acceleration of the object relative to the global inertial frame
     * (the Earth). This means that an object at rest will record an
     * acceleration with magnitude 0g, whereas an object in freefall
     * will experience an acceleration of -1g. Note that this is
     * different from the raw readings recorded by many real world
     * accelerometers (1g at rest, 0g in freefall) that are not
     * calibrated for use in an intertial navigation scenario.
     * </p>
     *
     * @return The acceleration along each body axis, (x, y, z), in m/s^2.
     */
    public Vector3f getLinearAcceleration();
}
