package harvard.robobees.simbeeotic.model.sensor;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class DefaultGyroscope extends AbstractSensor implements Gyroscope {

    private float sigma = 0.0085f;   // rad/s


    /** {@inheritDoc} */
    public Vector3f getAngularVelocity() {

        Vector3f vel = getHost().getTruthAngularVelocity();

        return new Vector3f(addNoise(vel.x, sigma),
                            addNoise(vel.y, sigma),
                            addNoise(vel.z, sigma));
    }


    @Inject(optional = true)
    public final void setSigma(@Named("sigma") final float sigma) {
        this.sigma = sigma;
    }
}
