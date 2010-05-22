package harvard.robobees.simbeeotic.model;


import javax.vecmath.Vector3f;


/**
 * An event that corresponds to an external force being applied to
 * a physical entity in the simulation.
 *
 * @author bkate
 */
public class ExternalForceEvent implements Event {

    private String id;
    private Vector3f force;  // Newtons in the world frame


    public ExternalForceEvent(Vector3f force) {
        this("", force);
    }


    public ExternalForceEvent(String id, Vector3f force) {

        this.id = id;
        this.force = force;
    }


    /**
     * Gets the identifier for this force.
     *
     * @return The identifier of this force, or an empty string if not set.
     */
    public String getId() {
        return id;
    }


    /**
     * Gets the force vector.
     *
     * @return The force vector in the world frame (Newtons).
     */
    public Vector3f getForce() {
        return new Vector3f(force);
    }
}
