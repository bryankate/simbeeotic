package harvard.robobees.simbeeotic;


/**
 * An interface that defines a listener interested in clock updates.
 *
 * @author bkate
 */
public interface ClockListener {

    /**
     * This method is invoked when the clock is updated, which only occurs
     * when a discrete event is executed.
     * 
     * @param time The new virtual time.
     */
    public void clockUpdated(SimTime time);
}
