package harvard.robobees.simbeeotic.comms;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.vecmath.Vector3f;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

import harvard.robobees.simbeeotic.SimClock;
import harvard.robobees.simbeeotic.configuration.ConfigurationAnnotations.GlobalScope;


/**
 * An RF propagation model that treats all radios as point source dipole
 * antennas where transmitted power degrades as a function of range.
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

            // todo: random degradation of signal

            // simple degradation with inverse-square law assuming a
            // point source and dipole antenna
            float rxPower = txPower / diff.lengthSquared();

            if (rxPower == Float.POSITIVE_INFINITY) {
                rxPower = txPower;
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
