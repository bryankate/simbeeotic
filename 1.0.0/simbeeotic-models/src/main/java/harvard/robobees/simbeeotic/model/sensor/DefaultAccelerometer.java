package harvard.robobees.simbeeotic.model.sensor;


import harvard.robobees.simbeeotic.model.PhysicalEntity;

import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;


/**
 * @author bkate
 */
public class DefaultAccelerometer extends AbstractSensor implements Accelerometer {

    /**
     * Standard constructor.
     *
     * @param host The physical entity to which the sensor is being attached.
     * @param sigma The standard deviation of the error associated with readings from this sensor (m/s^2).
     * @param seed The seed for the random number generator, used for adding noise to readings.
     */
    public DefaultAccelerometer(PhysicalEntity host, float sigma, long seed) {
        super(host, seed, sigma);
    }

    
    /** {@inheritDoc} */
    public Vector3f getLinearAcceleration() {

        Vector3f accel = getHost().getTruthLinearAcceleration();
        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(getHost().getTruthOrientation());
        trans.inverse();

        // rotate so that the acceleration is in the
        // body frame, not the world frame
        trans.transform(accel);

        // add noise
        return new Vector3f(addNoise(accel.x), addNoise(accel.y), addNoise(accel.z));
    }
}
