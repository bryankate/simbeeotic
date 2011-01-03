package harvard.robobees.simbeeotic.model;


import java.util.concurrent.TimeUnit;


/**
 * A factory for generating timers.
 *
 * @author bkate
 */
public interface TimerFactory {

    /**
     * Creates a new timer that will fire at a future point in time.
     *
     * @param callback The callback that will be made when the timer fires.
     * @param offset The offset from the current time at which the timer should fire. This
     *               value must be greater than or equal to zero.
     * @param offsetUnit The time unit in which the offset is measured.
     *
     * @return The timer object, which can be used to reset or cancel the firing.
     */
    public Timer createTimer(TimerCallback callback, long offset, TimeUnit offsetUnit);


    /**
     * Creates a new timer that will fire at a periodic rate starting at a future point in time.
     *
     * @param callback The callback that will be made when the timer fires.
     * @param offset The offset from the current time at which the timer should fire. This
     *               value must be greater than or equal to zero.
     * @param offsetUnit The time unit in which the offset is measured.
     * @param period The rate at which this timer fires. If the value is less than or equal to zero
     *               then the behavior is identical to {@link #createTimer(TimerCallback, long, java.util.concurrent.TimeUnit)}
     * @param periodUnit The time unit in which the period is measured.
     *
     * @return The timer object, which can be used to reset or cancel the firing.
     */
    public Timer createTimer(TimerCallback callback, long offset, TimeUnit offsetUnit, long period, TimeUnit periodUnit);
}
