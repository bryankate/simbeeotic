package harvard.robobees.simbeeotic.model.comms;


import com.bulletphysics.linearmath.MatrixUtil;
import com.bulletphysics.linearmath.Transform;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.ClockControl;
import harvard.robobees.simbeeotic.model.PhysicalEntity;
import harvard.robobees.simbeeotic.model.AbstractModel;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.model.EventHandler;
import harvard.robobees.simbeeotic.model.TimerCallback;
import harvard.robobees.simbeeotic.SimTime;
import harvard.robobees.simbeeotic.util.MathUtil;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * A base class that adds a callback functionality to a radio so that
 * a model can be notified when messages arrive.
 *
 * @author bkate
 */
public abstract class AbstractRadio extends AbstractModel implements Radio {

    private final int SENDQUEUE_SIZE = 100;
    private PhysicalEntity host;
    private PropagationModel propModel;
    private double txRxTime = 0;

    //async send data
    private LinkedList<byte[]> sendQueue = new LinkedList();
    private harvard.robobees.simbeeotic.model.Timer sendTimer;

    // parameters
    private Vector3f offset = new Vector3f();
    private Vector3f pointing = new Vector3f(0, 0, 1);
    private Vector3f pointingNormal = new Vector3f(1, 0, 0);
    private double rotation = 0;
    private AntennaPattern pattern;

    private Set<MessageListener> listeners = new HashSet<MessageListener>();


    /** {@inheritDoc} */
    public void initialize() {

        super.initialize();

        calculatePointingNormal();

        // there may be no propagation model, in which case comms won't work but we shouldn't throw an exception
        // until someone tries to use them. it is possible that someone attached a radio for no reason...
        propModel = getSimEngine().findModelByType(PropagationModel.class);

        final long idlePollPeriod = 100;   // ms

        // a timer that periodically measures idle energy usage
        createTimer(new TimerCallback() {

            public void fire(SimTime time) {

                double idleTime = idlePollPeriod - txRxTime;

                if (idleTime < 0) {
                    txRxTime -= idlePollPeriod;
                }
                else {

                    txRxTime = 0;
                    getAggregator().addValue("energy", "radio-idle", getIdleEnergy() * (idleTime / TimeUnit.SECONDS.toMillis(1)));
                }
            }

        }, 0, TimeUnit.MICROSECONDS, idlePollPeriod, TimeUnit.MILLISECONDS);


        sendTimer=createTimer(new TimerCallback() {
           public void fire(SimTime t) {
               /**
                * check if queue is empty, if not schedule next timer when the next packet has been sent
                * if queue is empty, send nextFiring to null so that sendAsync checks and knows that it needs
                * to restart the timer.
                * Finally, send packet using send()
                * NOTE: Packet sent at the end of time period of sending. 
                 */
               long sendTime;

               // send packet
               transmit((byte[])sendQueue.removeFirst());

               if(sendQueue.isEmpty()) {
                   sendTime = 0; 
               }
               else {
                   sendTime = (new Double(((byte[])sendQueue.getFirst()).length / 125 / getBandwidth())).longValue();
               }

               //reset timer              
               sendTimer.reset(t, sendTime, TimeUnit.MILLISECONDS, 0, TimeUnit.MILLISECONDS);
                
           }
        }, 0, TimeUnit.MICROSECONDS, 0, TimeUnit.MILLISECONDS);
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
    public void receive(SimTime time, byte[] data, double rxPower, double frequency) {

        double timeToRx = data.length / 125.0 / getBandwidth();

        txRxTime += timeToRx * TimeUnit.SECONDS.toMillis(1);

        getAggregator().addValue("energy", "radio-rx", timeToRx * getRxEnergy());
    }


    /** {@inheritDoc} */
    public void transmit(byte[] data) {

        double timeToTx = data.length / 125.0 / getBandwidth();

        txRxTime += timeToTx * TimeUnit.SECONDS.toMillis(1);

        getAggregator().addValue("energy", "radio-tx", timeToTx * getTxEnergy());
    }


    /** {@inheritDoc} */
    public int transmitAsync(byte[] data) {

        double timeToTx = data.length / 125.0 / getBandwidth();
        int timerSuccess = 0;

        //Check if queue is full
        if(sendQueue.size() > SENDQUEUE_SIZE) {
            return 0;
        }

        // Queue not full - so add this packet to queue
        sendQueue.add(data);

        // Check if timer is running for send

        timerSuccess = checkTimer();        

        if(timerSuccess == 0)
            return 0;

        txRxTime += timeToTx * TimeUnit.SECONDS.toMillis(1);

        getAggregator().addValue("energy", "radio-tx", timeToTx * getTxEnergy());

        return 1;
    }

    /**
     * Gets the amount of energy used by the radio when it is receiving a
     * message. The return from this method may be dependent on the current
     * state of the raio, and thus cannot be considered static.
     *
     * @return The amount of energy used to receive data (in mA).
     */
    protected abstract double getRxEnergy();


    /**
     * Gets the amount of energy used by the radio when it is transmitting a
     * message. The return from this method may be dependent on the current
     * state of the raio, and thus cannot be considered static.
     *
     * @return The amount of energy used to transmit data (in mA).
     */
    protected abstract double getTxEnergy();


    /**
     * Gets the amount of energy used by this radio when it is idling (not sending or
     * receiving) for one second. The return from this method may be dependent on the current
     * state of the raio, and thus cannot be considered static.
     *
     * @return The energy consumption in idle mode (in mA).
     */
    protected abstract double getIdleEnergy();


    /**
     * Gets the bandwidth of the radio.
     *
     * @return The bitrate of the radio (in kbps).
     */
    protected abstract double getBandwidth();


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
    public final Vector3f getAntennaPointing() {
        return transformToWorldFrame(pointing);
    }


    /** {@inheritDoc} */
    @Override
    public Vector3f getAntennaNormal() {
        return transformToWorldFrame(pointingNormal);
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


    /**
     * Transforms a vector in the body frame of the host to the
     * world reference frame accodring to the body's current orientation.
     *
     * @param vec The vector to be transformed (in the body frame).
     *
     * @return The transformed vector (in the world frame).
     */
    private Vector3f transformToWorldFrame(Vector3f vec) {

        Vector3f temp = new Vector3f(vec);
        Transform trans = new Transform();

        trans.setIdentity();
        trans.setRotation(host.getTruthOrientation());

        trans.transform(temp);

        return temp;
    }


    /**
     * Calculates the normal to the antenna pointing vector in the body frame.
     */
    private void calculatePointingNormal() {

        Vector3f bodyY = new Vector3f(0, 1, 0);

        // the rotation needed to transform the body Z axis to the pointing vector
        Quat4f bodyRot = MathUtil.getRotation(new Vector3f(0, 0, 1), pointing);

        // the body Y axis, rotated according to the pointing vector
        Transform bodyTrans = new Transform();

        bodyTrans.setIdentity();

        if (!bodyRot.equals(new Quat4f())) {
            bodyTrans.setRotation(bodyRot);
        }

        bodyTrans.transform(bodyY);

        // the transform needed to rotate the antenna about its major axis
        Quat4f antRot = new Quat4f();
        MatrixUtil.getRotation(MathUtil.eulerZYXtoDCM(0, 0, (float)rotation), antRot);
        Transform rotTrans = new Transform();

        rotTrans.setIdentity();

        if (!antRot.equals(new Quat4f())) {
            rotTrans.setRotation(antRot);
        }

        rotTrans.transform(bodyY);

        // the pointing normal in the body frame
        pointingNormal.cross(bodyY, pointing);
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
     * <br/>
     * This is a point in the body frame. It is added to the absolute position of
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
     * <br/>
     * The input to this method is a vector in the body frame. It will
     * be converted to the world frame at runtime according to the body's
     * orientation.
     *
     * @param pointing The antenna pointing vector (in the body frame).
     */
    @Inject(optional = true)
    public final void setPointing(@Named("pointing") final Vector3f pointing) {
        this.pointing = pointing;
    }


    /**
     * Sets the rotation angle of the antenna. This is the angle that
     * the antenna is rotated (counter clockwise) about its major axis.
     *
     * @param rotation The rotation angle (radians).
     */
    @Inject(optional = true)
    public final void setRotation(@Named("rotation") final double rotation) {
        this.rotation = rotation;
    }


    @Inject
    public final void setAntennaPattern(final AntennaPattern pattern) {
        this.pattern = pattern;
    }

    protected int checkTimer() {
        if(sendTimer.getNextFiringTime().getTime() == 0) {
            long sendTime = (new Double(((byte[])sendQueue.getFirst()).length / 125.0 / getBandwidth())).longValue();
            sendTimer.reset(/*((ClockControl)getSimEngine().findModelByType(ClockControl.class)).getCurrentTime()*/ getCurrTime(), sendTime, TimeUnit.MILLISECONDS, 0, TimeUnit.MILLISECONDS);
        }
        return 0;
    }
}
