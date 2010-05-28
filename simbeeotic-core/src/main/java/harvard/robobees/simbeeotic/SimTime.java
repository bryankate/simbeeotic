package harvard.robobees.simbeeotic;


import java.util.concurrent.TimeUnit;


/**
 * The virtual time representation used to schedule and execute events. The time
 * being represented if the time since the start of the simulation.
 *
 * <br/>
 * At some point in the future this class may also encapsulate a number
 * of tiebreaking fields used to deterministically order events. This is
 * especially important in parallel discrete event simulations.
 *
 * @author bkate
 */
public final class SimTime implements Comparable<SimTime> {

    private long time;  // nanoseconds


    /**
     * Creates a new time object.
     *
     * @param time The virtual time (in milliseconds).
     */
    public SimTime(long time) {
        this(time, TimeUnit.MILLISECONDS);
    }


    /**
     * Creates a new time object.
     *
     * @param time The virtual time.
     * @param unit The unit of the {@code time} parameter.
     */
    public SimTime(long time, TimeUnit unit) {
        this.time = unit.toNanos(time);
    }


    /**
     * Creates a new time object.
     *
     * @param base The virtual time to use as a base.
     * @param offset The amount of time to add to the base.
     * @param unit The unit of the {@code offset} parameter.
     */
    public SimTime(SimTime base, long offset, TimeUnit unit) {
        this.time = base.getTime() + unit.toNanos(offset);
    }


    /**
     * Gets the precise value of time.
     *
     * @return The value of virtual time (in nanoseconds).
     */
    public long getTime() {
        return time;
    }


    /**
     * Gets the imprecise value of time. This value is imprecise
     * because it is a floating point representation of the time.
     *
     * @return The virtual time as a floating point numbr (in seconds).
     */
    public double getImpreciseTime() {
        return (double)time / TimeUnit.SECONDS.toNanos(1);
    }


    /**
     * Determines the difference between this time and another time object.
     *
     * @param other The other time to compare against.
     *
     * @return The difference in times, obtained by subtracting the {@code other} time from this time.
     */
    public SimTime diff(SimTime other) {
        return new SimTime(time - other.time, TimeUnit.NANOSECONDS);
    }


    @Override
    public String toString() {
        return time + "";
    }


    @Override
    public int compareTo(SimTime o) {
        return Long.valueOf(time).compareTo(o.time);
    }


    @Override
    public boolean equals(Object obj) {

        if (obj instanceof SimTime) {
            return (time == ((SimTime)obj).time);
        }

        return false;
    }


    @Override
    public int hashCode() {
        return Long.valueOf(time).hashCode();
    }
}
