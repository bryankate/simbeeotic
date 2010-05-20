package harvard.robobees.simbeeotic.model.sensor;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.model.Contact;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.CollisionEvent;
import harvard.robobees.simbeeotic.model.EventHandler;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;
import java.util.Set;
import java.util.HashSet;


/**
 * @author bkate
 */
public class DefaultContactSensor extends AbstractSensor implements ContactSensor {

    private float radius = 0.005f;  // m

    private Set<ContactSensorListener> listeners = new HashSet<ContactSensorListener>();


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


    /**
     * Handles events that are generated when collisions occur on the parent model.
     *
     * @param time The time of the collision.
     * @param event The corresponding event.
     */
    @EventHandler
    public final void handleCollisionEvent(SimTime time, CollisionEvent event) {

        // just because we had a collision on the parent, it doesn't mean
        // it occurred in the range of the sensor
        if (isTripped()) {

            for (ContactSensorListener listener : listeners) {
                listener.tripped(time, this);
            }
        }
    }


    /**
     * {@inheritDoc}
     *
     * This imeplmentation adds the sensor as a listener for {@link CollisionEvent}s.
     */
    @Override
    public void setParentModel(Model parent) {
        
        super.setParentModel(parent);

        ((PhysicalEntity)parent).addCollisionListener(getModelId());
    }


    /** {@inheritDoc} */
    public final void addListener(ContactSensorListener listener) {
        listeners.add(listener);
    }


    /** {@inheritDoc} */
    public final void removeListener(ContactSensorListener listener) {
        listeners.remove(listener);
    }


    @Inject(optional = true)
    public final void setradius(@Named("radius") final float radius) {
        this.radius = radius;
    }
}
