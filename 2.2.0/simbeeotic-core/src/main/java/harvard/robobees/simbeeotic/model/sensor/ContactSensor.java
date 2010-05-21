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
}
