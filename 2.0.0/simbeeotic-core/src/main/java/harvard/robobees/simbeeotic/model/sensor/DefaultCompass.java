package harvard.robobees.simbeeotic.model.sensor;


import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class DefaultCompass extends AbstractSensor implements Compass {

    private float sigma = 0.0016f;   // degrees


    /** {@inheritDoc} */
    public float getHeading() {

        Transform orient = new Transform();
        orient.setIdentity();
        orient.setRotation(getHost().getTruthOrientation());

        Vector3f unitX = new Vector3f(1, 0, 0);
        orient.transform(unitX);

        float heading = addNoise((float)Math.toDegrees(Math.atan2(unitX.y, unitX.x)), sigma);

        // put into [0,360)
        if (heading < 0) {
            heading += 360;
        }

        // change to clockwise rotation (compass standard)
        heading = 360 - heading;

        if (heading == 360) {
            heading = 0;
        }

        return heading;
    }


    @Inject(optional = true)
    public final void setSigma(@Named(value = "sigma") final float sigma) {
        this.sigma = sigma;
    }
}
