package harvard.robobees.simbeeotic;


import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.model.Event;
import harvard.robobees.simbeeotic.model.Model;

import java.util.Set;


/**
 * An interface that defines the public functionality of the simulation engine
 * executive. This is the interface that models will use to schedule events
 * and locate other models.
 *
 * @author bkate
 */
public interface SimEngine {

    /**
     * Gets the time of the event that was last processed (or is currently being processed). This
     * is essentially the global virtual time (GVT) of the simulation. No events can be scheduled
     * prior to this time.
     *
     * @return The current simulation time.
     */
    public SimTime getCurrentTime();


    /**
     * Schedules an event to be processed in the future.
     *
     * @param modelId The ID of the model that is the target for the event.
     * @param time The simulation time at whicht he event should be executed.
     * @param event The event to execute at the given time.
     *
     * @return The handle of the scheduled event, which can be used to cancel the event.
     */
    public long scheduleEvent(int modelId, SimTime time, Event event);


    /**
     * Cancels the execution of an event, if it hasn't already been processed.
     *
     * @param eventId The ID of the event to cancel.
     */
    public void cancelEvent(long eventId);


    /**
     * Finds a model by its identifier.
     *
     * @param ID The identifier of the model to locate.
     *
     * @return The model instance, if one exists by the given identifier.
     */
    public Model findModelById(int ID);


    /**
     * Find the set of models with a given name. There may be more than one model
     * returned because we do not require unique model names.
     *
     * @param name The name of the model to locate.
     * 
     * @return The set of models with the given name, or an empty set if none exist.
     */
    public Set<Model> findModelsByName(String name);


    /**
     * Find the set of models of a certain type.
     *
     * @param type The type of the model to locate.
     *
     * @return The set of models that are a givne type, or an empty set if none exist.
     */
    public Set<Model> findModelsByType(Class type);
}
