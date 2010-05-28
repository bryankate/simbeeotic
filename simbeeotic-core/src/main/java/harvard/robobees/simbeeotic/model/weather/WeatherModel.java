package harvard.robobees.simbeeotic.model.weather;


import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;


/**
 * An interface that represents environmental conditions.
 *
 * @author bkate
 */
public interface WeatherModel {

    /**
     * Gets the wind velocity at the given point and time.
     *
     * @param time The time of the query.
     * @param position The position of the measurement (in the world frame).
     *
     * @return The wind velocity (m/s in the world frame).
     */
    public Vector3f getWindVelocity(SimTime time, Vector3f position);

}
