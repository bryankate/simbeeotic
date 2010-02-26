package harvard.robobees.simbeeotic.model;


/**
 * @author bkate
 */
public class SimTime implements Comparable<SimTime> {

    private double time;
    private int priority = 0;


    public SimTime(final double time) {
        this.time = time;
    }


    public SimTime(final double time, final int priority) {

        this.time = time;
        this.priority = priority;
    }


    public final int getPriority() {
        return priority;
    }


    public final double getTime() {
        return time;
    }


    @Override
    public String toString() {
        return time + ":" + priority;
    }


    @Override
    public int compareTo(SimTime o) {

        int timeComp = Double.valueOf(time).compareTo(o.time);

        if (timeComp == 0) {
            return Integer.valueOf(priority).compareTo(o.priority);
        }

        return timeComp;
    }
}
