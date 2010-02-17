package harvard.robobees.simbeeotic.model.sensor;


/**
 * @author bkate
 */
public interface Compass {

    /**
     * Gets the current heading as a clockwise degree offset from true north.
     *
     * @return The heading from true north, in the range [0,360).
     */
    public float getHeading();
}
