package harvard.robobees.simbeeotic.model;


/**
 * An event that corresponds to a {@link Timer} being fired.
 *
 * @author bkate
 */
public final class TimerEvent implements Event {

    private Timer timer;


    public TimerEvent(Timer timer) {
        this.timer = timer;
    }


    public Timer getTimer() {
        return timer;
    }
}
