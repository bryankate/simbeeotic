package harvard.robobees.simbeeotic.model.sensor;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.AbstractModel;
import harvard.robobees.simbeeotic.model.Model;

import javax.vecmath.Vector3f;
import java.util.Random;


/**
 * A base class for sensors that holds basic position and pointing information
 * and provides convenience methods.
 *
 * @author bkate
 */
public abstract class AbstractSensor extends AbstractModel {

    private PhysicalEntity host;

    private Vector3f offset;
    private Vector3f pointing;
    private Random rand;


    /** {@inheritDoc} */
    public void initialize() {
    }


    /** {@inheritDoc} */
    public void finish() {
    }


    /**
     * Adds gaussian noise to a reading according to the given sigma value. This mechanism
     * is provided so that the values will be repeatable. This is possible because of the
     * properly seeded random number generator in this class.
     *
     * @param reading The reading to be noised.
     * @param sigma The variance of the Gaussian noise.
     *
     * @return The reading, with gaussian noise added.
     */
    protected final float addNoise(final float reading, final float sigma) {
        return reading + ((float)rand.nextGaussian() * sigma);
    }


    protected final PhysicalEntity getHost() {
        return host;
    }


    protected final Vector3f getOffset() {
        return offset;
    }


    protected final Vector3f getPointing() {
        return pointing;
    }


    /**
     * {@inheritDoc}
     *
     * This implementation ensures that the host model is a {@link PhysicalEntity}.
     */
    @Override
    public void setParentModel(Model parent) {

        super.setParentModel(parent);

        if (parent instanceof PhysicalEntity) {
            setHost((PhysicalEntity)parent);
        }
    }


    // this is only optional when wired up by the standard way (parent is a model that implements PhysicalEntity)
    @Inject(optional = true)
    public final void setHost(final PhysicalEntity host) {
        this.host = host;
    }


    @Inject
    public final void setRandomSeed(@Named("random-seed") final long seed) {
        this.rand = new Random(seed);
    }


    @Inject
    public final void setOffset(@Named("offset") final Vector3f offset) {
        this.offset = offset;
    }


    @Inject
    public final void setPointing(@Named("pointing") final Vector3f pointing) {
        this.pointing = pointing;
    }
}
