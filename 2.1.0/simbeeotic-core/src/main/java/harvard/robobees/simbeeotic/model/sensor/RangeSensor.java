package harvard.robobees.simbeeotic.model.sensor;


/**
 * @author bkate
 */
public interface RangeSensor {

    /**
     * Gets the current reading from the range sensor.
     *
     * @return The range to the closest object, in meters. If the
     *         range is not within the sensor's bounds (too close
     *         or too far) then Float.POSITIVE_INFINITY is returned.
     */
    public float getRange();
}
