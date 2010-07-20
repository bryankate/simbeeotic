package harvard.robobees.simbeeotic.model.comms;


/**
 * An interface that represents the radiation pattern of an antenna.
 *
 * @author bkate
 */
public interface AntennaPattern {

    /**
     * Gets the gain of the antenna at a specific azimuth
     * and elevation. For reference, consider the antenna to be oriented
     * in a 3-dimensional space along the positive {@code Z}
     * axis. This means that the azimuth is an angle in the {@code XY}
     * plane (about the {@code Z axis} measured counter-clockwise from
     * the {@code X} axis. Likewise, the elevation is an angle in the
     * {@code XZ} plane (about the {@code Y} axis) measured
     * clockwise from the {@code Z} axis.
     *
     * @param azimuth The azimuth angle in the pattern (rad).
     * @param elevation The angle of elevation in the pattern (rad).
     *
     * @return The gain of the antenna (in dBi).
     */
    public double getGain(double azimuth, double elevation);
}
