package harvard.robobees.simbeeotic.comms;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.bulletphysics.linearmath.QuaternionUtil;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

import harvard.robobees.simbeeotic.SimClock;
import harvard.robobees.simbeeotic.util.LinearMathUtil;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;


/**
 * An RF propagation model that treats all radios as point sources and
 * transmitted power degrades as a function of range according to
 * the inverse-square law.
 * 
 * @author bkate
 */
public class DefaultPropagationModel implements PropagationModel {

    private SimClock clock;

    private float snrMargin = 10;            // dB
    private float noiseFloorMean = 0.01f;    // mW
    private float noiseFloorSigma = 0.005f;  // mW

    private Random rand = new Random(112181);
    private Set<Radio> radios = new HashSet<Radio>();


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


    /** {@inheritDoc} */
    @Override
    public void transmit(Radio tx, byte[] data, float txPower) {

        Vector3f txPos = tx.getPosition();
        Vector3f txPointing = tx.getPointing();
        Vector3f diff = new Vector3f();

        // generate a random noise level
        float noise = noiseFloorMean + ((float)rand.nextGaussian() * noiseFloorSigma);

        // degrade the signal to each radio
        for (Radio rx : radios) {

            // do not overhear your own transmission
            if (rx.equals(tx)) {
                continue;
            }

            diff.sub(rx.getPosition(), txPos);

            float distSq = diff.lengthSquared();

            if (distSq > 0) {

                // find the rotation needed to get from the pointing vector
                // in the world frame to the antenna frame. the calculated
                // azimuth and elevation must be in the antenna frame before
                // querying the antenna pattern
                Quat4f rot = LinearMathUtil.getRotation(txPointing, new Vector3f(0, 0, 1));
                Transform trans = new Transform();

                trans.setIdentity();

                if (!rot.equals(new Quat4f())) {
                    trans.setRotation(rot);
                }

                // transform the vector between rx and tx using this rotation to make it
                // relative to the antenna frame
                trans.transform(diff);
                diff.normalize();

                float az = (float)Math.atan2(diff.y, diff.x);
                float el = (float)Math.atan2(diff.x, diff.z);

                // adjust the power according to the tx antenna pattern
                float output = tx.getAntennaPattern().getPower(az, el);

                txPower *= Math.pow(10, output / 10);
            }

            // todo: use antenna pattern of receiver?

            // todo: random degradation of signal?
            float rxPower = txPower;

            // simple degradation with inverse-square law assuming a
            // point source and isotropic rx antenna
            if (distSq >= 1) {
                rxPower /= distSq;
            }

            float snr = 10 * (float)Math.log10(rxPower / noise);

            // enough power to capture signal?
            if (snr >= snrMargin) {

                // todo: copy the data?
                rx.receive(clock.getCurrentTime(), data, rxPower);
            }
        }
    }


    @Inject
    public final void setSimClock(@GlobalScope final SimClock clock) {
        this.clock = clock;
    }


    @Inject
    public final void setRandomSeed(@Named(value = "random-seed") final long seed) {
        this.rand = new Random(seed);
    }


    @Inject(optional = true)
    public final void setSnrMargin(@Named(value = "snr-margin") final float margin) {
        this.snrMargin = margin;
    }


    @Inject(optional = true)
    public final void setNoiseFloorMean(@Named(value = "noise-floor-mean") final float noiseFloorMean) {
        this.noiseFloorMean = noiseFloorMean;
    }


    @Inject(optional = true)
    public final void setNoiseFloorSigma(@Named(value = "noise-floor-sigma") final float noiseFloorSigma) {
        this.noiseFloorSigma = noiseFloorSigma;
    }
}
