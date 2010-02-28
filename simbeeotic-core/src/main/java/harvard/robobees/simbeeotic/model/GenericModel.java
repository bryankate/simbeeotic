package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.model.sensor.AbstractSensor;
import harvard.robobees.simbeeotic.comms.AbstractRadio;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;


/**
 * A convenience class that acts as a container for generic models - ones
 * that have an "inventory" of sensors and a radio.
 *
 * @author bkate
 */
public abstract class GenericModel extends AbstractPhysicalModel {

    private Map<String, AbstractSensor> sensors = new HashMap<String, AbstractSensor>();
    private AbstractRadio radio;


    /**
     * Gets the sensor with the given name.
     *
     * @param name The name of the sensor to retrieve.
     *
     * @return The sensor, or {@code null} if none exists for the given name.
     */
    public final AbstractSensor getSensor(final String name) {
        return sensors.get(name);
    }


    /**
     * Gets the sensor with the given name. An attempt will be made to cast
     * the sensor to the givne type.
     *
     * @param name The name of the sensor to retrieve.
     * @param type The type to which the sensor is cast prior to returning.
     *
     * @return The sensor, or {@code null} if none exists for the given name.
     */
    public final <T> T getSensor(final String name, Class<T> type) {

        AbstractSensor sensor = sensors.get(name);

        if (sensor != null) {
            return type.cast(sensor);
        }

        return null; 
    }


    /**
     * Gets all the sensors attached to this model.
     *
     * @return The set of sensors.
     */
    public final Set<AbstractSensor> getSensors() {
        return new HashSet<AbstractSensor>(sensors.values());
    }


    /**
     * Adds a sensor to the model.
     *
     * @param name The name of the sensor, used for later retrieval.
     * @param sensor The sensor to add.
     */
    public final void addSensor(final String name, AbstractSensor sensor) {

        if ((name == null) || (sensor == null)) {
            throw new RuntimeException("The sensor to add (and its name) must not be null.");
        }

        if (sensors.containsKey(name)) {
            throw new RuntimeException("Duplicate sensor definition: " + name);
        }

        sensors.put(name, sensor);
    }


    public final AbstractRadio getRadio() {
        return radio;
    }


    public final void setRadio(AbstractRadio radio) {
        this.radio = radio;
    }
}
