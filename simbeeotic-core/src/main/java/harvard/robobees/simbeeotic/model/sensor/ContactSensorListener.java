package harvard.robobees.simbeeotic.model.sensor;


import harvard.robobees.simbeeotic.SimTime;


/**
 * A callback that can be attached to a {@link ContactSensor}. The callbck is
 * invoked when the contact sensor changes state from untripped (no contact) to
 * tripped (contact).
 *
 * @author bkate
 */
public interface ContactSensorListener {

    /**
     * The method that is called when the sensor is triggered.
     *
     * @param time The time at which the sensor is triggered.
     * @param sensor The snesor being triiggered.
     */
    public void tripped(SimTime time, ContactSensor sensor);
}
