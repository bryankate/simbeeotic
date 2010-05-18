package harvard.robobees.simbeeotic.comms;


import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import harvard.robobees.simbeeotic.util.MathUtil;
import harvard.robobees.simbeeotic.SimClock;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;


/**
 * A base class that implements some boilerplate functionality of a propagation model.
 *
 * @author bkate
 */
public abstract class AbstractPropagationModel implements PropagationModel {

    protected Random rand = new Random(112181);
    private Set<Radio> radios = new HashSet<Radio>();
    private SimClock clock;

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
    @Override
    public void transmit(Radio tx, byte[] data, double txPower, Band band) {

        Vector3f diff = new Vector3f();

        // determine the received signal strength at each radio
        for (Radio rx : radios) {

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

            // todo: copy the data?
            rx.receive(clock.getCurrentTime(), data, rxPower, band.getCenterFrequency());
        }
    }


    /** {@inheritDoc} */
    @Override
    public void addRadio(Radio radio) {
        radios.add(radio);
    }


    /** {@inheritDoc} */
    @Override
    public void removeRadio(Radio radio) {
        radios.remove(radio);
    }


    /**
     * {@inheritDoc}
     *
     * This implementation generates a random value using a Gaussian
     * distribution around a noise floor mean using the given sigma.
     */
    @Override
    public double getNoiseFloor() {
        return noiseFloorMean + (rand.nextGaussian() * noiseFloorSigma);
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

        Vector3f txPos = tx.getPosition();
        Vector3f txPointing = tx.getPointing();
        Vector3f diff = new Vector3f();
        double rxPower = txPower;

        diff.sub(rx.getPosition(), txPos);

        float distSq = diff.lengthSquared();

        if (distSq > 0) {

            // find the rotation needed to get from the pointing vector
            // in the world frame to the antenna frame. the calculated
            // azimuth and elevation must be in the antenna frame before
            // querying the antenna pattern
            Quat4f rot = MathUtil.getRotation(txPointing, new Vector3f(0, 0, 1));
            Transform trans = new Transform();

            trans.setIdentity();

            if (!rot.equals(new Quat4f())) {
                trans.setRotation(rot);
            }

            // transform the vector between rx and tx using this rotation to make it
            // relative to the antenna frame
            trans.transform(diff);
            diff.normalize();

            double az = Math.atan2(diff.y, diff.x);
            double el = Math.atan2(diff.x, diff.z);

            // adjust the power according to the tx antenna pattern
            rxPower += tx.getAntennaPattern().getPower(az, el);

            // todo: use antenna pattern of receiver
        }

        return rxPower;
    }


    @Inject
    public final void setRandomSeed(@Named(value = "random-seed") final long seed) {
        this.rand = new Random(seed);
    }


    @Inject
    public final void setSimClock(@GlobalScope final SimClock clock) {
        this.clock = clock;
    }


    @Inject(optional = true)
    public final void setNoiseFloorMean(@Named(value = "noise-floor-mean") final float noiseFloorMean) {
        this.noiseFloorMean = noiseFloorMean;
    }


    @Inject(optional = true)
    public final void setNoiseFloorSigma(@Named(value = "noise-floor-sigma") final float noiseFloorSigma) {
        this.noiseFloorSigma = noiseFloorSigma;
    }

    
    @Inject(optional = true)
    public final void setReceiveRadiusThreshold(@Named(value = "range-thresh") final double thresh) {
        this.rangeThresh = thresh;
    }
}