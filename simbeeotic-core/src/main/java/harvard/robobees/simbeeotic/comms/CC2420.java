package harvard.robobees.simbeeotic.comms;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;

import java.util.Random;


/**
 * A functional model of the CC2420 radio.
 *
 * @author bkate
 */
public class CC2420 extends AbstractRadio {

    private Random rand;

    private Band band = new Band(2405, 5);  // channel 11
    private double txPower = 0;             // dBm
    private double minRxPower = -90;        // dBm

    private static Band operating = new Band(2442.5, 85);  // zigbee spectrum

    private static Logger logger = Logger.getLogger(CC2420.class);


    /**
     * {@inheritDoc}
     *
     * Transmits a packet at the currently set power level on the currently set channel.
     * The CC2420 spec defines 8 transmit power settings ranging from 0 dBm to -25 dBm.
     * The transmit power can be adjusted with the {@link #setTransmitPowerLevel(int)} method.
     */
    @Override
    public void transmit(byte[] data) {
        getPropagationModel().transmit(this, data, txPower, band);
    }


    /**
     * {@inheritDoc}
     *
     * This implementation performs the following steps:
     * <ol>
     * <li>Determines if the received power level is within the sensitivity range.</li>
     * <li>Queries the PRR/SNR function to determine the packet reception rate.</li>
     * <li>Makes a random draw to determine if the packet should be received.</li>
     * </ol>
     */
    @Override
    public void receive(double time, byte[] data, double rxPower, double frequency) {

        // check if it is within the range of sensitivity of the radio
        if (rxPower < minRxPower) {
            return;
        }

        // todo: check the frequency against the current channel

        // consult the PRR/SNR curve
        double snr = rxPower - getPropagationModel().getNoiseFloor();

        // todo: find a more reliable source?
        // this function approximates the SNR curve from the paper
        // "Improving Wireless Simulation Through Noise Modeling" by Lee, Cerpa, and Levis
        double prr = 1 / (1 + Math.exp(-((1.5 * snr) - 8.25)));

        if (rand.nextDouble() <= prr) {
            notifyListeners(time, data, rxPower);
        }
    }


    /** {@inheritDoc} */
    @Override
    public Band getOperatingBand() {
        return operating;
    }


    /**
     * Sets the output power level. The CC2420 has 8 transmit power settings, detailed on
     * page 52 of the CC2420 data sheet, rev. 1.4.
     *
     * @param level The transmit power level. It must be an integer in the set
     *              (3, 7, 11, 15, 19, 23, 27, 31).
     */
    @Inject(optional = true)
    public final void setTransmitPowerLevel(@Named(value = "tx-power-level") final int level) {

        switch(level) {

            case 31:
                
                txPower = 0;
                break;

            case 27:

                txPower = -1;
                break;

            case 23:

                txPower = -3;
                break;

            case 19:

                txPower = -5;
                break;

            case 15:

                txPower = -7;
                break;

            case 11:

                txPower = -10;
                break;

            case 7:

                txPower = -15;
                break;

            case 3:

                txPower = -25;
                break;

            default:
                logger.warn("Unrecognized TX power level, using default level.");
        }
    }


    /**
     * Sets the channel on which the radio is operating.
     *
     * @param channel The operating channel. It must be an integer in the range (11,26).
     */
    @Inject(optional = true)
    public final void setChannel(@Named(value = "channel") final int channel) {

        if ((channel < 11) || (channel > 26)) {

            logger.warn("Unrecognized channel, using default channel.");

            band = new Band(2405, 5);
        }

        band= new Band(2405 + (5 * (channel - 11)), 5);
    }


    @Inject
    public final void setRandomSeed(@Named(value = "random-seed") final long seed) {
        this.rand = new Random(seed);
    }
}
