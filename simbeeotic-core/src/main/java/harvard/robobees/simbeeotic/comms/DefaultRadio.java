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

    private Band band = new Band(2442.5, 85);
    private double snrMargin = 10;          // dBm
    private double maxPower = 15;           // dBm
    // You don't snuggle with Max Power, you strap yourself in and feel the G's!


    /**
     * {@inheritDoc}
     *
     * Transmits at the maximum power.
     */
    @Override
    public void transmit(byte[] data) {
        getPropagationModel().transmit(this, data, maxPower, band);
    }


    /**
     * {@inheritDoc}
     *
     * This implementation performs an SNR thresholding and invokes all listeners
     * registered with this radio to receive notifications when a message is received.
     */
    @Override
    public void receive(double time, byte[] data, double rxPower, double frequency) {

        double snr = rxPower - getPropagationModel().getNoiseFloor();

        // enough power to capture signal?
        if (snr >= snrMargin) {
            notifyListeners(time, data, rxPower);
        }
    }


    /** {@inheritDoc} */
    @Override
    public Band getOperatingBand() {
        return band;
    }


    @Inject(optional = true)
    public final void setCenterFrequency(@Named(value = "center-frequency") final double freq) {
        this.band = new Band(freq, this.band.getBandwidth());
    }


    @Inject(optional = true)
    public final void setBandwidth(@Named(value = "bandwidth") final double bandwidth) {
        this.band = new Band(this.band.getCenterFrequency(), bandwidth);
    }


    @Inject(optional = true)
    public final void setMaxPower(@Named(value = "max-power") final double power) {
        this.maxPower = power;
    }


    @Inject(optional = true)
    public final void setSnrMargin(@Named(value = "snr-margin") final double margin) {
        this.snrMargin = margin;
    }
}
