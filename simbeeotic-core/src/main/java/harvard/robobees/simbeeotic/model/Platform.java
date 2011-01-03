package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.model.comms.AbstractRadio;
import harvard.robobees.simbeeotic.model.sensor.AbstractSensor;

import java.util.Set;


/**
 * An interface that allows a model access to attached sensors and radio.
 *
 * @author bkate
 */
public interface Platform extends TimerFactory {


    /**
     * Gets the sensor with the given name.
     *
     * @param name The name of the sensor to retrieve.
     *
     * @return The sensor, or {@code null} if none exists for the given name.
     */
    public AbstractSensor getSensor(final String name);


    /**
     * Gets the sensor with the given name. An attempt will be made to cast
     * the sensor to the given type.
     *
     * @param name The name of the sensor to retrieve.
     * @param type The type to which the sensor is cast prior to returning.
     *
     * @return The sensor, or {@code null} if none exists for the given name.
     */
    public <T> T getSensor(final String name, Class<T> type);


    /**
     * Gets the sensor(s) of a given type attached to this model.
     *
     * @param type The type of sensor to search for.
     *
     * @return The sensor(s), or an empty set if none exists for the given type.
     */
    public <T> Set<T> getSensors(Class<T> type);


    /**
     * Gets all the sensors attached to this model.
     *
     * @return The set of sensors.
     */
    public Set<AbstractSensor> getSensors();


    /**
     * Gets the radio attached to this model.
     *
     * @return The radio, or {@code null} if none is attached.
     */
    public AbstractRadio getRadio();
}
