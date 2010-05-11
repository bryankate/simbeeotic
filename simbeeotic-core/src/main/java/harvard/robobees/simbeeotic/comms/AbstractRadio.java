package harvard.robobees.simbeeotic.comms;


import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.model.PhysicalModel;

import javax.vecmath.Vector3f;
import java.util.HashSet;
import java.util.Set;


/**
 * A base class that adds a callback functionality to a radio so that
 * a model can be notified when messages arrive.
 *
 * @author bkate
 */
public abstract class AbstractRadio implements Radio {

    private PhysicalModel host;
    private PropagationModel propModel;

    private Vector3f offset = new Vector3f();
    private Vector3f pointing = new Vector3f(0, 0, 1);
    private AntennaPattern pattern;

    private Set<MessageListener> listeners = new HashSet<MessageListener>();


    /** {@inheritDoc} */
    @Override
    public final Vector3f getPosition() {

        Vector3f pos = host.getTruthPosition();

        if (offset.lengthSquared() > 0) {

            // transform the offset (which is in the body frame)
            // to account for the body's current orientation
            Vector3f off = new Vector3f(offset);
            Transform trans = new Transform();

            trans.setIdentity();
            trans.setRotation(host.getTruthOrientation());

            trans.transform(off);

            // add the offset to the absolute position (in the world frame)
            pos.add(off);
        }

        return pos;
    }


    /** {@inheritDoc} */
    @Override
    public final Vector3f getPointing() {

        // transform the offset (which is in the body frame)
        // to account for the body's current orientation
        Vector3f point = new Vector3f(pointing);
        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(host.getTruthOrientation());

        trans.transform(point);

        return point;
    }


    /** {@inheritDoc} */
    @Override
    public final AntennaPattern getAntennaPattern() {
        return pattern;
    }


    /**
     * Notifies all listeners that a message has been received.
     *
     * @param time The simulation time when the message was received.
     * @param data The data received.
     * @param rxPower The strength of the received signal (in dBm).
     */
    protected final void notifyListeners(double time, byte[] data, double rxPower) {

        for (MessageListener l : listeners) {
            l.messageReceived(time, data, rxPower);
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

        propModel.addRadio(this);
    }


    /**
     * Sets the offset position of the antenna base, essentially where the antenna is
     * attached to the body.
     *
     * @note This is a point in the body frame. It is added to the absolute position of
     * the body at runtime.
     *
     * @param offset The offset of the antenna base (in the body frame).
     */
    @Inject(optional = true)
    public final void setOffset(@Named(value = "offset") final Vector3f offset) {
        this.offset = offset;
    }


    /**
     * Sets the pointing vector of the antenna. This is a vector that points along the
     * major axis of the antenna.
     *
     * @note The input to this method is a vector in the body frame. It will
     * be converted to the world frame at runtime according to the body's
     * orientation.
     *
     * @param pointing The antenna pointing vector (in the body frame).
     */
    @Inject(optional = true)
    public final void setPointing(@Named(value = "pointing") final Vector3f pointing) {
        this.pointing = pointing;
    }


    @Inject
    public final void setAntennaPattern(final AntennaPattern pattern) {
        this.pattern = pattern;
    }
}
