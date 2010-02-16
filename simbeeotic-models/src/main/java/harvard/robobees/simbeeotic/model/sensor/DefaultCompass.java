package harvard.robobees.simbeeotic.model.sensor;


import harvard.robobees.simbeeotic.model.PhysicalEntity;
import static harvard.robobees.simbeeotic.util.LinearMathUtil.quaternionToEulerZYX;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;

import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.TransformUtil;
import com.bulletphysics.linearmath.QuaternionUtil;


/**
 * @author bkate
 */
public class DefaultCompass extends AbstractSensor implements Compass {

    /**
     * Standard constructor.
     *
     * @param host The physical entity to which the sensor is being attached.
     * @param sigma The standard deviation of the error associated with readings from this sensor (degrees).
     * @param seed The seed for the random number generator, used for adding noise to readings.
     */
    public DefaultCompass(PhysicalEntity host, float sigma, long seed) {
        super(host, seed, sigma);
    }


    /** {@inheritDoc} */
    public float getHeading() {

        Transform orient = new Transform();
        orient.setIdentity();
        orient.setRotation(getHost().getTruthOrientation());

        Vector3f unitX = new Vector3f(1, 0, 0);
        orient.transform(unitX);

        float heading = (float)Math.toDegrees(Math.atan2(unitX.y, unitX.x));

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
}
