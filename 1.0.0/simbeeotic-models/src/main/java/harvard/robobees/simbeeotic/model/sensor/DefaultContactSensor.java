package harvard.robobees.simbeeotic.model.sensor;


import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.Contact;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class DefaultContactSensor extends AbstractSensor implements ContactSensor {

    private Vector3f offset;
    private float radius;


    /**
     * Standard constructor.
     *
     * @param host The physical entity to which the sensor is being attached.
     * @param offset The offset, relative to the host's body origin, of the sensor's origin.
     * @param radius The radius of the bounding sphere representing the sensor's area of sensitivity.
     * @param seed The seed for the random number generator, used for adding noise to readings.
     */
    public DefaultContactSensor(PhysicalEntity host, Vector3f offset, float radius, long seed) {

        super(host, seed, 0);

        this.offset = offset;
        this.radius = radius;
    }


    /** {@inheritDoc} */
    public boolean isTripped() {

        // try each known contact point and determine if it is within the sensor's area.
        for (Contact contact : getHost().getContactPoints()) {

            Vector3f diff = new Vector3f(contact.getBodyContactPoint());

            diff.sub(offset);

            if (diff.length() <= radius) {
                return true;
            }
        }

        return false;
    }
}
