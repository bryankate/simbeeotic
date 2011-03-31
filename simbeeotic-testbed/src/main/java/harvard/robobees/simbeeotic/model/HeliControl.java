package harvard.robobees.simbeeotic.model;


/**
 * An interface that defines a simple set of controls for the
 * testbed helicopters.
 *
 * @author bkate
 */
public interface HeliControl {

    /**
     * Gets the ID of the helicopter that is being controlled.
     *
     * @return The ID of the heli being controlled.
     */
    public int getHeliId();

    
    /**
     * Gets the currently set yaw command.
     *
     * @return The level that is currently set, as a value in the range (0,1)
     *         correspnding to the percentage of total possible output.
     */
    public double getYaw();


    /**
     * Sets the yaw command to be executed by the heli.
     *
     * @param level A value in the range of (0,1) that corresponds to the
     *              percentage of the total possible output.
     */
    public void setYaw(double level);


    /**
     * Gets the currently set pitch command.
     *
     * @return The level that is currently set, as a value in the range (0,1)
     *         correspnding to the percentage of total possible output.
     */
    public double getPitch();


    /**
     * Sets the pitch command to be executed by the heli.
     *
     * @param level A value in the range of (0,1) that corresponds to the
     *              percentage of the total possible output.
     */
    public void setPitch(double level);


    /**
     * Gets the currently set roll command.
     *
     * @return The level that is currently set, as a value in the range (0,1)
     *         correspnding to the percentage of total possible output.
     */
    public double getRoll();


    /**
     * Sets the roll command to be executed by the heli.
     *
     * @param level A value in the range of (0,1) that corresponds to the
     *              percentage of the total possible output.
     */
    public void setRoll(double level);


    /**
     * Gets the currently set thrust command.
     *
     * @return The level that is currently set, as a value in the range (0,1)
     *         correspnding to the percentage of total possible output.
     */
    public double getThrust();


    /**
     * Sets the thrust command to be executed by the heli.
     *
     * @param level A value in the range of (0,1) that corresponds to the
     *              percentage of the total possible output.
     */
    public void setThrust(double level);
}
