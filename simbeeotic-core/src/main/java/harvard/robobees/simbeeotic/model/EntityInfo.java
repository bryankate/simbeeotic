package harvard.robobees.simbeeotic.model;


import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;


/**
 * A class that acts as a go-between for Simbeeotic models and the JBullet bodies
 * that represent them in the physics engine. An instance of this class is attached
 * to each JBullet body, so when an event occurs in the physics engine (like a
 * collision) we can make updates to it that will be seen by the Simbeeotic models.
 * This way we do not need to keep a global map associating bodies with models.
 *
 * @author bkate
 */
public class EntityInfo {

    private Map<String, Object> metadata = new HashMap<String, Object>();
    private Set<Contact> contactPoints = new HashSet<Contact>();
    private Set<Integer> collisionListeners = new HashSet<Integer>();


    public EntityInfo() {
    }


    public EntityInfo(final Map<String, Object> meta) {
        metadata = meta;
    }


    /**
     * Get the current set of contact points for the physical body.
     *
     * @return The current set of contact points, or an empty set if none exist.
     */
    public Set<Contact> getContactPoints() {
        return contactPoints;
    }


    /**
     * Gets the set of {@link Model}s that should receive a {@link CollisionEvent}
     * when a collision is detected between the physical body and another body.
     *
     * <br/>
     * This is a bit of a hack, but it is better than broadcasting an event
     * when a collision occurs and having every model check if it is involved.
     *
     * @return The model identifiers of the interested models.
     */
    public Set<Integer> getCollisionListeners() {
        return collisionListeners;
    }


    /**
     * Gets the metadata associated with the physical body.
     *
     * @return The metadata for this physical body.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
