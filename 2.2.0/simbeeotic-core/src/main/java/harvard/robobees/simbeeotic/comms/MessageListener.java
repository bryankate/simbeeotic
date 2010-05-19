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
     * @param rxPower The strength of the received signal (in dBm).
     */
    public void messageReceived(double time, byte[] data, double rxPower);
}
