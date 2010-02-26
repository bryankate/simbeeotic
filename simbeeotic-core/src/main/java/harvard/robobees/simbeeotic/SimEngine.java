package harvard.robobees.simbeeotic;


import harvard.robobees.simbeeotic.model.SimTime;
import harvard.robobees.simbeeotic.model.Event;


/**
 * @author bkate
 */
public interface SimEngine {

    public SimTime getCurrentTime();

    public void scheduleEvent(int modelId, SimTime time, Event event);
}
