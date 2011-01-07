package harvard.robobees.simbeeotic.model.sensor;


import javax.vecmath.Quat4f;


/**
 * A sensor that can detect the orentation of an entity.
 *
 * @author bkate
 */
public interface PoseSensor {

    /**
     * Gets the sensed orientation in the world.
     *
     * @return The orientation of the sensor, as a quaternion,
     *         relative to the initial condition in which the body
     *         axes are aligned with the world axes.
     */
    public Quat4f getPose();
}
