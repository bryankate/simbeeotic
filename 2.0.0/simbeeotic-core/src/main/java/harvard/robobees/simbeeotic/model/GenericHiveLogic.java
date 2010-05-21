package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.comms.MessageListener;


/**
 * An interface to be implemented by users that wish to define custom hive logic.
 * The logic implementation is attached to a {@link GenericHive} at runtime to
 * provide specific behavior.
 * 
 * @author bkate
 */
public interface GenericHiveLogic extends MessageListener {

    /**
     * Initializes the hive logic. This method is called once by the
     * simulation prior to any simulation step.
     *
     * @param bee The hive that the logic is controlling.
     */
    public void intialize(GenericHive bee);


    /**
     * This method is called at the beginning of each simulation step to
     * invoke the custom hive logic.
     *
     * @param time The start time of the current time step (in seconds).
     */
    public void update(double time);
}
