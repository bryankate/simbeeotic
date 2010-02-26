package harvard.robobees.simbeeotic.model;


/**
 * An interface that establishes the core functionality of a model in the
 * simulation. A model need not be associated with a physical representation
 * in the world.
 *
 * @author bkate
 */
public interface Model {

    /**
     * Initializes the model before the simulation starts. This method
     * will be called exactly once by the simulation executive prior to
     * any call to {@link #update(double)}.
     */
    public void initialize();


    /**
     * This method is called by the simulation executive when a model is
     * to be updated for a time step. It will be called exactly once
     * per time step (after initialization) for the duration of the simulation.
     *
     * @param currTime The time at the beginning of the time step being simulated (in seconds).
     */
    public void update(final double currTime);


    /**
     * Retrieves the unique model identifier assigned by the simulation executive
     * prior to initialization.
     *
     * @return An identifier in the range of (1, Integer.MAX_VALUE).
     */
    public int getModelId();
}
