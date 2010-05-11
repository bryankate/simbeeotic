package harvard.robobees.simbeeotic.model.sensor;


import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public interface PositionSensor {

    /**
     * Gets the sensed position in the world.
     *
     * @return The position of the sensor, (x, y, z), in meters,
     *         relative to the world origin.
     */
    public Vector3f getPosition();
}
