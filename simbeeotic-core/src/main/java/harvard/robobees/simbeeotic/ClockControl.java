/*
 * Copyright (c) 2012, The President and Fellows of Harvard College.
 * All Rights Reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *  3. Neither the name of the University nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE UNIVERSITY OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package harvard.robobees.simbeeotic;


import org.apache.log4j.Logger;

import java.util.Date;
import java.util.concurrent.TimeUnit;
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

    private long epoch = 0;   // ms since standard epoch
    private SimTime currTime;
    private SimTime endTime;
    private Set<ClockListener> listeners = new HashSet<ClockListener>();

    private Logger logger = Logger.getLogger(ClockControl.class);


    public ClockControl(SimTime end, long dateEpoch) {

        endTime = end;
        epoch = dateEpoch;
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
     * Gets the date that is associated with the current virtual time. Since the units
     * of the date are less precise than the units of virtual time, multiple
     * events with different virtual times may correspond to the same Date. As such, the
     * Date should not be sued to distinguish between events (use the SimTime).
     *
     * @return The current virtual time as a Date.
     */
    public Date getCurrentDate() {
        return new Date(epoch + TimeUnit.NANOSECONDS.toMillis(currTime.getTime()));
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
