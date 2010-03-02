package harvard.robobees.simbeeotic.model.sensor;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.model.Contact;

import javax.vecmath.Vector3f;


/**
 * @author bkate
 */
public class DefaultContactSensor extends AbstractSensor implements ContactSensor {

    private float radius = 0.005f;  // m


    /** {@inheritDoc} */
    public boolean isTripped() {

        // try each known contact point and determine if it is within the sensor's area.
        for (Contact contact : getHost().getContactPoints()) {

            Vector3f diff = new Vector3f(contact.getBodyContactPoint());

            diff.sub(getOffset());

            if (diff.length() <= radius) {
                return true;
            }
        }

        return false;
    }


    @Inject(optional = true)
    public final void setradius(@Named(value = "radius") final float radius) {
        this.radius = radius;
    }
}
