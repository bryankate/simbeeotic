package harvard.robobees.simbeeotic.model.comms;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.apache.commons.math.special.Erf;
import org.apache.commons.math.MathException;

import harvard.robobees.simbeeotic.SimTime;


/**
 * A functional model of the CC2420 radio.
 *
 * @author bkate
 */
public class CC2420 extends ZigbeeRadio {

    private double txPower = 0;       // dBm
    private double minRxPower = -90;  // dBm

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
        getPropagationModel().transmit(this, data, txPower, getChannel());
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
    public void receive(SimTime time, byte[] data, double rxPower, double frequency) {

        // check if it is within the range of sensitivity of the radio
        if (rxPower < minRxPower) {
            return;
        }

        // todo: check the frequency against the current channel

        // consult the PRR/SNR curve
        double snr = rxPower - getPropagationModel().getNoiseFloor();
        double prr = 0;

        // this function approximates the PRR/SNR curve from the paper
        // "Improving Wireless Simulation Through Noise Modeling" by Lee, Cerpa, and Levis
//        prr = 1 / (1 + Math.exp(-((1.5 * snr) - 8.25)));

        // this calculation was pulled from TOSSIM (CpmModelC.nc), which is based on
        // CC2420 measurements for the paper "RSSI is Under Appreciated" by Srinivasan and Levis
        try {

            double beta1 = 0.9794;
            double beta2 = 2.3851;
            double pse = 0.5 * (1 - Erf.erf(beta1 * (snr - beta2) / Math.sqrt(2)));

            prr = Math.pow(1 - pse, 46);
        }
        catch(MathException me) {
            return;
        }

        if (getRandom().nextDouble() <= prr) {
            notifyListeners(time, data, rxPower);
        }
    }


    /**
     * Sets the output power level. The CC2420 has 8 transmit power settings, detailed on
     * page 52 of the CC2420 data sheet, rev. 1.4.
     *
     * @param level The transmit power level. It must be an integer in the set
     *              (3, 7, 11, 15, 19, 23, 27, 31).
     */
    @Inject(optional = true)
    public final void setTransmitPowerLevel(@Named("tx-power-level") final int level) {

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
    
}
