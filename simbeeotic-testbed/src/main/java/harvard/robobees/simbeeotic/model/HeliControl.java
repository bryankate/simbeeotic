package harvard.robobees.simbeeotic.model;


/**
 * An interface that defines a simple set of controls for the
 * testbed helicopters.
 *
 * @author bkate
 */
public interface HeliControl {

    /**
     * Sets the yaw command to be executed by the heli.
     *
     * @param level A value in the range of (0,1) that corresponds to the
     *              percentage of the total possible output.
     */
    public void setYaw(double level);


    /**
     * Sets the pitch command to be executed by the heli.
     *
     * @param level A value in the range of (0,1) that corresponds to the
     *              percentage of the total possible output.
     */
    public void setPitch(double level);


    /**
     * Sets the roll command to be executed by the heli.
     *
     * @param level A value in the range of (0,1) that corresponds to the
     *              percentage of the total possible output.
     */
    public void setRoll(double level);


    /**
     * Sets the thrust command to be executed by the heli.
     *
     * @param level A value in the range of (0,1) that corresponds to the
     *              percentage of the total possible output.
     */
    public void setThrust(double level);
}
