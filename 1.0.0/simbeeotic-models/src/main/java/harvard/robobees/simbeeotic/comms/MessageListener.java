package harvard.robobees.simbeeotic.comms;


/**
 * An interface that is used to identify listener objects that wish to be notified
 * when a message is received by a radio.
 *
 * @author bkate
 */
public interface MessageListener {

    /**
     * Called when a message is received by a radio.
     *
     * @param time The start interval of the time step when the message was received.
     * @param data The data received.
     */
    public void messageReceived(float time, byte[] data);
}
