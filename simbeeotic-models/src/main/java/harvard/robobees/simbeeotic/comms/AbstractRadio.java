package harvard.robobees.simbeeotic.comms;


import com.google.inject.Inject;

import java.util.Set;
import java.util.HashSet;

import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.PhysicalModel;

import javax.vecmath.Vector3f;


/**
 * A base class that adds a callback functionality to a radio so that
 * a model can be notified when messages arrive.
 *
 * @author bkate
 */
public abstract class AbstractRadio implements Radio {

    private PhysicalModel host;
    private PropagationModel propModel;
    private Set<MessageListener> listeners = new HashSet<MessageListener>();


    /**
     * {@inheritDoc}
     *
     * This implementation invokes all listeners registered with this radio to
     * receive notifications when a message is received.
     */
    @Override
    public void receive(double time, byte[] data, float rxPower) {

        for (MessageListener l : listeners) {
            l.messageReceived(time, data, rxPower);
        }
    }


    /** {@inheritDoc} */
    @Override
    public Vector3f getPosition() {
        return host.getTruthPosition();
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


    protected final PhysicalModel getHost() {
        return host;
    }
    

    protected final PropagationModel getPropagationModel() {
        return propModel;
    }


    @Inject
    public final void setHost(final PhysicalModel host) {
        this.host = host;
    }


    @Inject
    public final void setPropagationModel(@GlobalScope PropagationModel propModel) {
        this.propModel = propModel;
    }
}
