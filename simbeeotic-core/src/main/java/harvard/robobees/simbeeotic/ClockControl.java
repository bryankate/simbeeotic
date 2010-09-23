package harvard.robobees.simbeeotic;


import org.apache.log4j.Logger;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.Set;
import java.util.HashSet;


/**
 * A class that controls the clock in the simulation engine.
 *
 * @author bkate
 */
public class ClockControl {

    private boolean paused = false;
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    private SimTime currTime;
    private SimTime endTime;
    private Set<ClockListener> listeners = new HashSet<ClockListener>();

    private Logger logger = Logger.getLogger(ClockControl.class);


    public ClockControl(SimTime end) {
        endTime = end;
    }


    /**
     * Pauses the clock. The clock can be restarted by calling the
     * {@link #start()} method. If the clock is already in a paused
     * state, calling the method has no effect.
     */
    public void pause() {

        lock.lock();

        try {
            paused = true;
        }
        finally {
            lock.unlock();
        }
    }


    /**
     * Starts the clock. The clock can be paused by calling the
     * {@link #pause()} method. If the clock is already in the running
     * state, calling the method has no effect.
     */
    public void start() {

        lock.lock();

        try {

            paused = false;
            condition.signalAll();
        }
        finally {
            lock.unlock();
        }
    }


    /**
     * Blocks until the clock is started. If the clock is in a
     * running state, the method will not block.
     */
    public void waitUntilStarted() {

        lock.lock();

        try {

            while(paused) {

                try {
                    condition.await();
                }
                catch(InterruptedException ie) {

                    logger.warn("Clock control was interrupted.");
                    return;
                }
            }
        }
        finally {
            lock.unlock();
        }
    }


    /**
     * Notifies listeners that the clock has been updated.
     *
     * @param time The latest virtual time.
     */
    public void notifyListeners(SimTime time) {

        currTime = time;

        for (ClockListener listener : listeners) {
            listener.clockUpdated(time);
        }
    }


    /**
     * Gets the time of the event that was last processed (or is currently being processed). This
     * is essentially the global virtual time (GVT) of the simulation. No events can be scheduled
     * prior to this time.
     *
     * @return The current simulation time.
     */
    public SimTime getCurrentTime() {
        return currTime;
    }


    /**
     * Gets the time at which the scenario will end. The simulation engine will execute all events
     * that are less than or equal to this time.
     *
     * @return The time at which the simulation ends.
     */
    public SimTime getEndTime() {
        return endTime;
    }


    /**
     * Adds a listener to the set of interested classes.
     *
     * @param listener The listener to be added.
     */
    public void addListener(ClockListener listener) {
        listeners.add(listener);
    }


    /**
     * Removes a listener from the set of interested classes.
     *
     * @param listener The listener to be removed.
     */
    public void removeListener(ClockListener listener) {
        listeners.remove(listener);
    }
}
