package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.model.comms.AbstractRadio;
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
public abstract class GenericModel extends AbstractPhysicalEntity implements Platform {

    private Map<String, AbstractSensor> sensors = new HashMap<String, AbstractSensor>();
    private AbstractRadio radio;


    @Override
    public final AbstractSensor getSensor(final String name) {
        return sensors.get(name);
    }


    @Override
    public final <T> T getSensor(final String name, Class<T> type) {

        AbstractSensor sensor = sensors.get(name);

        if (sensor != null) {
            return type.cast(sensor);
        }

        return null; 
    }


    @Override
    public final <T> Set<T> getSensors(Class<T> type) {

        Set<T> found = new HashSet<T>();

        for (AbstractSensor s : sensors.values()) {

            if (type.isAssignableFrom(s.getClass())) {
                found.add(type.cast(s));
            }
        }

        return found;
    }


    @Override
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


    @Override
    public final AbstractRadio getRadio() {
        return radio;
    }


    public final void setRadio(AbstractRadio radio) {
        this.radio = radio;
    }
}
