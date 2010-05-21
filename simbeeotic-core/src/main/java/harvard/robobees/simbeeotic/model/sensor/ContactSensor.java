package harvard.robobees.simbeeotic.model.sensor;


/**
 * @author bkate
 */
public interface ContactSensor {

    /**
     * Determines if the contact sensor has been activated by contact with another
     * object.
     *
     * @return True if the sensor is activated (contact), false otherwise (no contact).
     */
    public boolean isTripped();


    /**
     * Adds a callback to perform when a collision occurs in this sensor's area.
     * 
     * @param listener The callback being attached to the sensor.
     */
    public void addListener(ContactSensorListener listener);


    /**
     * Removes a collision callback.
     *
     * @param listener The callback being removed from the sensor.
     */
    public void removeListener(ContactSensorListener listener);
}
