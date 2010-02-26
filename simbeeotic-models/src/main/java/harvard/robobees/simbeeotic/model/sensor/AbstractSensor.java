package harvard.robobees.simbeeotic.model.sensor;


import harvard.robobees.simbeeotic.model.PhysicalEntity;

import java.util.Random;


/**
 * @author bkate
 */
abstract class AbstractSensor {

    private PhysicalEntity host;

    private Random rand;
    private float sigma;


    public AbstractSensor(PhysicalEntity host, long seed, float sigma) {

        this.host = host;
        this.rand = new Random(seed);
        this.sigma = sigma;
    }


    /**
     * Gets the entity to which this sensor is attached.
     *
     * @return The host of this sensor.
     */
    protected final PhysicalEntity getHost() {
        return host;
    }


    /**
     * Adds gaussian noise to a reading according to the configured sigma value.
     *
     * @param reading The reading to be noised.
     *
     * @return The reading, with gaussian noise added.
     */
    protected final float addNoise(final float reading) {
        return reading + ((float)rand.nextGaussian() * sigma);
    }
}
