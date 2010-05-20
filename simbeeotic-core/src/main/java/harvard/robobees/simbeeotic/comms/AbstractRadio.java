package harvard.robobees.simbeeotic.comms;


import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.AbstractModel;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.model.EventHandler;
import harvard.robobees.simbeeotic.SimTime;

import javax.vecmath.Vector3f;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;


/**
 * A base class that adds a callback functionality to a radio so that
 * a model can be notified when messages arrive.
 *
 * @author bkate
 */
public abstract class AbstractRadio extends AbstractModel implements Radio {

    private PhysicalEntity host;
    private PropagationModel propModel;

    private Vector3f offset = new Vector3f();
    private Vector3f pointing = new Vector3f(0, 0, 1);
    private AntennaPattern pattern;

    private Set<MessageListener> listeners = new HashSet<MessageListener>();


    /** {@inheritDoc} */
    public void initialize() {

        // find the propagation model and register with it
        Set<Model> propModels = getSimEngine().findModelsByType(PropagationModel.class);

        if (propModels.size() > 1) {
            throw new RuntimeException("There is more than one PropagationModel in the scenario.");
        }

        // there may be no propagation model, in which case comms won't work but we shouldn't throw an exception
        // until someone tries to use them. it is possible that someone attached a radio for no reason...
        if (!propModels.isEmpty()) {
            propModel = (PropagationModel)new ArrayList<Model>(propModels).get(0);
        }
    }


    /** {@inheritDoc} */
    public void finish() {
    }


    /**
     * Handles the reception of an RF transmission.
     *
     * @param time The time of the transmission.
     * @param event The details of the transmission.
     */
    @EventHandler
    public final void handleReceptionEvent(SimTime time, ReceptionEvent event) {
        receive(time, event.getData(), event.getRxPower(), event.getBand().getCenterFrequency());
    }


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
    protected final void notifyListeners(SimTime time, byte[] data, double rxPower) {

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


    protected final PhysicalEntity getHost() {
        return host;
    }
    

    protected final PropagationModel getPropagationModel() {

        if (propModel == null) {
            throw new RuntimeException("The propagation model could not be found in the scenario!");
        }

        return propModel;
    }


    /**
     * {@inheritDoc}
     *
     * This implementation ensures that the host model is a {@link PhysicalEntity}.
     */
    @Override
    public void setParentModel(Model parent) {

        super.setParentModel(parent);

        if (parent instanceof PhysicalEntity) {
            setHost((PhysicalEntity)parent);
        }
    }


    // this is only optional when wired up by the standard way (parent is a model that implements PhysicalEntity)
    @Inject(optional = true)
    public final void setHost(final PhysicalEntity host) {
        this.host = host;
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
    public final void setOffset(@Named("offset") final Vector3f offset) {
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
    public final void setPointing(@Named("pointing") final Vector3f pointing) {
        this.pointing = pointing;
    }


    @Inject
    public final void setAntennaPattern(final AntennaPattern pattern) {
        this.pattern = pattern;
    }
}
