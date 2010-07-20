package harvard.robobees.simbeeotic.model.comms;


import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.model.AbstractModel;
import harvard.robobees.simbeeotic.model.Model;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;
import harvard.robobees.simbeeotic.ClockControl;


/**
 * A base class that implements some boilerplate functionality of a propagation model.
 *
 * @author bkate
 */
public abstract class AbstractPropagationModel extends AbstractModel implements PropagationModel {

    @Inject
    @GlobalScope
    private ClockControl clockControl;

    private Map<Integer, Radio> radios = new HashMap<Integer, Radio>();

    // parameters
    private double rangeThresh = 10;      // m
    private float noiseFloorMean = -100;  // dBm
    private float noiseFloorSigma = 10;   // dBm


    /**
     * Calculates the path loss between the transmitter and receiver.
     *
     * @param tx The transmitting radio.
     * @param rx The receiving radio.
     * @param txPower The singal strength at the transmitter (in dBm).
     * @param band The band in which the radio is transmitting.
     * @param distance The distance between the transmitter and receiver (in m).
     *
     * @return The signal strength at the receiving radio (in dBm).
     */
    protected abstract double calculatePathLoss(Radio tx, Radio rx, double txPower, Band band, double distance);


    /** {@inheritDoc} */
    public void initialize() {

        super.initialize();

        List<Radio> radioModels = getSimEngine().findModelsByType(Radio.class);

        for (Radio radio : radioModels) {
            radios.put(((Model)radio).getModelId(), radio);
        }
    }


    /** {@inheritDoc} */
    public void finish() {
    }


    /** {@inheritDoc} */
    @Override
    public void transmit(Radio tx, byte[] data, double txPower, Band band) {

        Vector3f diff = new Vector3f();

        // determine the received signal strength at each radio
        for (Map.Entry<Integer, Radio> entry : radios.entrySet()) {

            Radio rx = entry.getValue();

            // do not overhear your own transmission
            if (rx.equals(tx)) {
                continue;
            }

            // check if it is within the operating band of the receiver
            if (!rx.getOperatingBand().isInBand(band.getCenterFrequency())) {
                continue;
            }

            diff.sub(rx.getPosition(), tx.getPosition());

            double dist = diff.length();

            // a course filtering of recipients based on range
            if (dist > rangeThresh) {
                continue;
            }

            double rxPower = calculatePathLoss(tx, rx, txPower, band, dist);

            // todo: random degradation of signal?

            // todo: offset reception time?

            // todo: copy the data?

            getSimEngine().scheduleEvent(entry.getKey(), clockControl.getCurrentTime(),
                                         new ReceptionEvent(data, rxPower, band));
        }
    }


    /**
     * {@inheritDoc}
     *
     * This implementation generates a random value using a Gaussian
     * distribution around a noise floor mean using the given sigma.
     * The value is independent of the given band.
     */
    @Override
    public double getNoiseFloor(Band band) {
        return noiseFloorMean + (getRandom().nextGaussian() * noiseFloorSigma);
    }


    /**
     * Gets the power at the receiving radio. The position and orientation of the
     * antennas are taken into account, along with their patterns. We assume
     * a direct line-of-sight between the two antenna positions.
     *
     * @param tx The transmitting radio.
     * @param rx The receiving radio.
     * @param txPower The transmited power (in dBm).
     * 
     * @return The power level of the signal at the receiver (in dBm).
     */
    protected double getReceivedPower(Radio tx, Radio rx, double txPower) {

        Vector3f diff = new Vector3f();
        double rxPower = txPower;

        // vector between two radios
        diff.sub(rx.getPosition(), tx.getPosition());

        float distSq = diff.lengthSquared();

        if (distSq > 0) {

            // start with gain from transmitter

            // find the rotations needed to get from the pointing vector
            // in the world frame to the antenna frame. the calculated
            // azimuth and elevation must be in the antenna frame before
            // querying the antenna pattern
            Vector3f diffAz = transformAndNormalize(tx.getAntennaNormal(), new Vector3f(1, 0, 0), diff);
            Vector3f diffEl = transformAndNormalize(tx.getAntennaPointing(), new Vector3f(0, 0, 1), diff);

            double az = Math.atan2(diffAz.y, diffAz.x);
            double el = Math.atan2(diffEl.x, diffEl.z);

            // adjust the power according to the tx antenna pattern
            rxPower += tx.getAntennaPattern().getGain(az, el);

            // invert the difference vector and get the receiver's gain
            diff.negate();

            diffAz = transformAndNormalize(rx.getAntennaNormal(), new Vector3f(1, 0, 0), diff);
            diffEl = transformAndNormalize(rx.getAntennaPointing(), new Vector3f(0, 0, 1), diff);

            az = Math.atan2(diffAz.y, diffAz.x);
            el = Math.atan2(diffEl.x, diffEl.z);

            // adjust the power according to the tx antenna pattern
            rxPower += rx.getAntennaPattern().getGain(az, el);
        }

        return rxPower;
    }


    /**
     * Figures out the rotation needed to get from one vector to another, then applies
     * that rotation to a third vector and normalizes the result.
     *
     * @param from The vector from which we are finding the transform.
     * @param to The vector to which we are finding the transform.
     * @param vec The vector to be transformed.
     *
     * @return A new vector that represents the rotated and normalized input {@code vec}.
     */
    protected Vector3f transformAndNormalize(Vector3f from, Vector3f to, Vector3f vec) {

        // find the transform between the first two vectors
        Quat4f rot = MathUtil.getRotation(from, to);
        Transform trans = new Transform();

        trans.setIdentity();

        if (!rot.equals(new Quat4f())) {
            trans.setRotation(rot);
        }

        // apply the transform to the third vector and normalize
        Vector3f temp = new Vector3f(vec);

        trans.transform(temp);
        temp.normalize();

        return temp;
    }


    @Inject(optional = true)
    public final void setNoiseFloorMean(@Named("noise-floor-mean") final float noiseFloorMean) {
        this.noiseFloorMean = noiseFloorMean;
    }


    @Inject(optional = true)
    public final void setNoiseFloorSigma(@Named("noise-floor-sigma") final float noiseFloorSigma) {
        this.noiseFloorSigma = noiseFloorSigma;
    }

    
    @Inject(optional = true)
    public final void setReceiveRadiusThreshold(@Named("range-thresh") final double thresh) {
        this.rangeThresh = thresh;
    }
}
