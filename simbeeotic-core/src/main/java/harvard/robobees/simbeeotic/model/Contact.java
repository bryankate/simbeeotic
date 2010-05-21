package harvard.robobees.simbeeotic.model;


import javax.vecmath.Vector3f;
import java.util.Map;


/**
 * A container class for information related to a contact between two
 * physical objects in the virtual world.
 * 
 * @author bkate
 */
public class Contact {

    private Vector3f bodyContactPoint;
    private Vector3f worldContactPoint;

    // the properties of the object we are touching
    private Map<String, Object> contactMetadata;


    public Contact(final Vector3f body, final Vector3f world, final Map<String, Object> props) {

        this.bodyContactPoint = new Vector3f(body);
        this.worldContactPoint = new Vector3f(world);
        this.contactMetadata = props;
    }


    /**
     * Gets the point of contact in body coordinates.
     *
     * @return The point on the body at which contact is made.
     */
    public Vector3f getBodyContactPoint() {
        return bodyContactPoint;
    }


    /**
     * Gets the point of contact in world coordinates.
     *
     * @return The point in the world in which contact is made.
     */
    public Vector3f getWorldContactPoint() {
        return worldContactPoint;
    }


    /**
     * Gets the properties of the object that is being touched.
     *
     * @return The object's user-defined proeprties.
     */
    public Map<String, Object> getContactMetadata() {
        return contactMetadata;
    }


    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Contact contact = (Contact) o;

        if (!bodyContactPoint.equals(contact.bodyContactPoint)) {
            return false;
        }

        if (!worldContactPoint.equals(contact.worldContactPoint)) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {

        int result = bodyContactPoint.hashCode();
        result = 31 * result + worldContactPoint.hashCode();

        return result;
    }
}
