package harvard.robobees.simbeeotic.model.sensor;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.model.PhysicalModel;

import javax.vecmath.Vector3f;
import java.util.Random;


/**
 * A base class for sensors that holds basic position and pointing information
 * and provides convenience methods.
 *
 * @author bkate
 */
public abstract class AbstractSensor {

    private PhysicalModel host;
    private Vector3f offset;
    private Vector3f pointing;
    private Random rand;


    /**
     * Adds gaussian noise to a reading according to the given sigma value. This mechanism
     * is provided so that the values will be repeatable. This is possible because of the
     * properly seeded random number generator in this class.
     *
     * @param reading The reading to be noised.
     *
     * @return The reading, with gaussian noise added.
     */
    protected final float addNoise(final float reading, final float sigma) {
        return reading + ((float)rand.nextGaussian() * sigma);
    }


    protected final PhysicalModel getHost() {
        return host;
    }


    protected final Vector3f getOffset() {
        return offset;
    }


    protected final Vector3f getPointing() {
        return pointing;
    }


    @Inject
    public final void setHost(final PhysicalModel host) {
        this.host = host;
    }


    @Inject
    public final void setRandomSeed(@Named(value = "random-seed") final long seed) {
        this.rand = new Random(seed);
    }


    @Inject
    public final void setPffset(@Named(value = "offset") final Vector3f offset) {
        this.offset = offset;
    }


    @Inject
    public final void setPointing(@Named(value = "pointing") final Vector3f pointing) {
        this.pointing = pointing;
    }
}
