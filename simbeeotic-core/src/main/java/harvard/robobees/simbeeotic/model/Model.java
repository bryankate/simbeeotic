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
     * any events being executed.
     */
    public void initialize();


    /**
     * This is the workhorse method of the model. When invoked by the sim executive
     * the model is to process the given event. It is up to the model to determine
     * the action to be taken based on the subtype of the event.
     *
     * Events are said to be executed instantaneously in time, meaning that no virtual
     * time passes while processing the event. Time is moved forward by processing
     * events in causal order.
     *
     * @param time The time at which the event is executed.
     * @param event The event to be executed.
     */
    public void processEvent(final SimTime time, final Event event);


    /**
     * Retrieves the unique model identifier assigned by the simulation executive
     * prior to initialization.
     *
     * @return An identifier in the range of (1, Integer.MAX_VALUE).
     */
    public int getModelId();
}
