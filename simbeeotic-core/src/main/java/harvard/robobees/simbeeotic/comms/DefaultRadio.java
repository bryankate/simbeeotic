package harvard.robobees.simbeeotic.comms;


import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A radio that transmits at maximum power and receives 100% of packets
 * with received power over a specific SNR threshold. 
 *
 * @author bkate
 */
public class DefaultRadio extends AbstractRadio {

    private float snrMargin = 10;   // dB
    private float maxPower = 50;    // mW
    // You don't snuggle with Max Power, you strap yourself in and feel the G's!


    /**
     * {@inheritDoc}
     *
     * Transmits at the maximum power.
     */
    @Override
    public void transmit(byte[] data) {
        getPropagationModel().transmit(this, data, maxPower);
    }


    /**
     * {@inheritDoc}
     *
     * This implementation performs an SNR thresholding and invokes all listeners
     * registered with this radio to receive notifications when a message is received.
     */
    @Override
    public void receive(double time, byte[] data, float rxPower) {

        float noise = getPropagationModel().getNoiseFloor();
        float snr = 10 * (float)Math.log10(rxPower / noise);

        // enough power to capture signal?
        if (snr >= snrMargin) {
            notifyListeners(time, data, rxPower);
        }
    }


    @Inject(optional = true)
    public final void setMaxPower(@Named(value = "max-power") final float power) {
        this.maxPower = power;
    }


    @Inject(optional = true)
    public final void setSnrMargin(@Named(value = "snr-margin") final float margin) {
        this.snrMargin = margin;
    }
}
