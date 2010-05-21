package harvard.robobees.simbeeotic.model.sensor;


import harvard.robobees.simbeeotic.model.PhysicalEntity;

import javax.vecmath.Vector3f;



/**
 * @author bkate
 */
public class DefaultGyroscope extends AbstractSensor implements Gyroscope {

    /**
     * Standard constructor.
     *
     * @param host The physical entity to which the sensor is being attached.
     * @param sigma The standard deviation of the error associated with readings from this sensor (rad/s).
     * @param seed The seed for the random number generator, used for adding noise to readings.
     */
    public DefaultGyroscope(PhysicalEntity host, float sigma, long seed) {
        super(host, seed, sigma);
    }


    /** {@inheritDoc} */
    public Vector3f getAngularVelocity() {

        Vector3f vel = getHost().getTruthAngularVelocity();

        return new Vector3f(addNoise(vel.x), addNoise(vel.y), addNoise(vel.z));
    }
}
