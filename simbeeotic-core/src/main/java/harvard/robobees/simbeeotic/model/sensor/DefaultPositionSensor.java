package harvard.robobees.simbeeotic.model.sensor;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class DefaultPositionSensor extends AbstractSensor implements PositionSensor {

    private float sigma = 0.5f;  // m


    /** {@inheritDoc} */
    public Vector3f getPosition() {

        Vector3f pos = getHost().getTruthPosition();

        return new Vector3f(addNoise(pos.x, sigma),
                            addNoise(pos.y, sigma),
                            addNoise(pos.z, sigma));
    }


    @Inject(optional = true)
    public final void setSigma(@Named("sigma") final float sigma) {
        this.sigma = sigma;
    }
}
