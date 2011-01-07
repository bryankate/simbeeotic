package harvard.robobees.simbeeotic.model.sensor;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Quat4f;


/**
 * An immplementation of the pose sensor that uses truth to determine the
 * orientation of the entity, adding some noise.
 *
 * @author bkate
 */
public class DefaultPoseSensor extends AbstractSensor implements PoseSensor {

    private float sigma = 0.01f;


    /** {@inheritDoc} */
    @Override
    public Quat4f getPose() {

        Quat4f orient = getHost().getTruthOrientation();

        // todo: add noise

        return orient;
    }


    @Inject(optional = true)
    public final void setSigma(@Named("sigma") final float sigma) {
        this.sigma = sigma;
    }
}
