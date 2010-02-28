package harvard.robobees.simbeeotic.model.sensor;


import harvard.robobees.simbeeotic.model.PhysicalEntity;

import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * @author bkate
 */
public class DefaultAccelerometer extends AbstractSensor implements Accelerometer {

    private float sigma = 0.005f;   // m/s^2


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
        return new Vector3f(addNoise(accel.x, sigma),
                            addNoise(accel.y, sigma),
                            addNoise(accel.z, sigma));
    }


    @Inject(optional = true)
    public final void setSigma(@Named(value = "sigma") final float sigma) {
        this.sigma = sigma;
    }
}
