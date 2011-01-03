package harvard.robobees.simbeeotic.model;


/**
 * An interface for helicopter behaviors.
 *
 * @author bkate
 */
public interface HeliBehavior {

    /**
     * Indicates that the behavior may start processing. This must be a
     * non-blocking call, with all work for the behavior implemented using
     * timer callbacks.
     *
     * @param platform The platform upon which the behavior is executing.
     * @param control The control interface for the helicopter.
     */
    public void start(Platform platform, HeliControl control);


    /**
     * Indicates that the behavior must stop all processing and cede control
     * of the helicopter. All timers relating to this behavior must be canceled
     * prior to returning from this method.
     */
    public void stop();
}
