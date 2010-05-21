package harvard.robobees.simbeeotic;


/**
 * An interface that provide access to global simulation timing information.
 *
 * @note This will likely go away if the simulation is converted to a discrete event
 * framework.
 *
 * @author bkate
 */
public interface SimClock {

    /**
     * Gets the current simulated time. This time represents the starting
     * point of the step currently being simulated.
     *
     * @return The start time of the current simulation step, in seconds.
     */
    public double getCurrentTime();


    /**
     * Gets the size of the time step that is being used to push the simulation
     * forward.
     *
     * @return The simulation time step.
     */
    public double getTimeStep();
}
