package harvard.robobees.simbeeotic.model;


import harvard.robobees.simbeeotic.SimTime;


/**
 * The user-defined functionality that is executed when a {@link Timer} fires.
 *
 * @author bkate
 */
public interface TimerCallback {

    /**
     * The method that is called when the timer fires.
     *
     * @param time The time at which the timer is firing.
     */
    public void fire(SimTime time);
}
