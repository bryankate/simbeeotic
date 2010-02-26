package harvard.robobees.simbeeotic.model.sensor;


import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.model.PhysicalEntity;


/**
 * @author bkate
 */
public class DefaultPositionSensor extends AbstractSensor implements PositionSensor {


    /**
     * Standard constructor.
     *
     * @param host The physical entity to which the sensor is being attached.
     * @param sigma The standard deviation of the error associated with readings from this sensor (rad).
     * @param seed The seed for the random number generator, used for adding noise to readings.
     */
    public DefaultPositionSensor(PhysicalEntity host, float sigma, long seed) {
        super(host, seed, sigma);
    }


    /** {@inheritDoc} */
    public Vector3f getPosition() {

        Vector3f pos = getHost().getTruthPosition();

        return new Vector3f(addNoise(pos.x), addNoise(pos.y), addNoise(pos.z));
        
    }
}
