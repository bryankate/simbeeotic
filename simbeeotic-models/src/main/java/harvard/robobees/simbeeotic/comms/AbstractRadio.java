package harvard.robobees.simbeeotic.comms;


import java.util.Set;
import java.util.HashSet;


/**
 * A base class that adds a callback functionality to a radio so that
 * a model can be notified when messages arrive.
 *
 * @author bkate
 */
public abstract class AbstractRadio implements Radio {

    private Set<MessageListener> listeners = new HashSet<MessageListener>();


    /**
     * {@inheritDoc}
     *
     * This implementation invokes all listeners registered with this radio to
     * receive notifications when a message is received.
     */
    @Override
    public void receive(float time, byte[] data) {

        for (MessageListener l : listeners) {
            l.messageReceived(time, data);
        }
    }


    /**
     * Subscribes a listener to notifications of message arrivals.
     *
     * @param listener The listener to invoke when a message is received.
     */
    public final void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }


    /**
     * Unsubscribes a listener from notifications.
     *
     * @param listener The listener that is to be removed from the set of
     *                 active listeners.
     */
    public final void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }
}
