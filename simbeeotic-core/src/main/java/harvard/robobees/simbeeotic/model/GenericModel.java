package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.comms.AbstractRadio;
import harvard.robobees.simbeeotic.model.sensor.AbstractSensor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * A convenience class that acts as a container for generic models - ones
 * that have an "inventory" of sensors and a radio.
 *
 * @author bkate
 */
public abstract class GenericModel extends AbstractPhysicalEntity {

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
     * the sensor to the given type.
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
     * Gets the sensor(s) of a given type attached to this model.
     *
     * @param type The type of sensor to search for.
     *
     * @return The sensor(s), or an empty set if none exists for the given type.
     */
    public final <T> Set<T> getSensors(Class<T> type) {

        Set<T> found = new HashSet<T>();

        for (AbstractSensor s : sensors.values()) {

            if (type.isAssignableFrom(s.getClass())) {
                found.add(type.cast(s));
            }
        }

        return found;
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
     * @param sensor The sensor to add.
     */
    public final void addSensor(AbstractSensor sensor) {
        addSensor(sensor.getName(), sensor);
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
            throw new RuntimeException("Duplicate sensor name: " + name);
        }

        sensors.put(name, sensor);
    }


    /**
     * {@inheritDoc}
     *
     * This implementation checks if the child is a sensor or radio and handles it appropriately.
     */
    @Override
    public void addChildModel(Model child) {

        super.addChildModel(child);

        if (child instanceof AbstractSensor) {
            addSensor((AbstractSensor)child);
        }

        if (child instanceof AbstractRadio) {
            setRadio((AbstractRadio)child);
        }
    }


    public final AbstractRadio getRadio() {
        return radio;
    }


    public final void setRadio(AbstractRadio radio) {
        this.radio = radio;
    }
}
