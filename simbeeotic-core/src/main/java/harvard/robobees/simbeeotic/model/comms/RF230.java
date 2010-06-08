package harvard.robobees.simbeeotic.model.comms;


import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import harvard.robobees.simbeeotic.SimTime;


/**
 * A functional model of the Atmel RF230 radio.
 *
 * @author bkate
 */
public class RF230 extends ZigbeeRadio {

    private double txPower = 3;         // dBm
    private double minRxPower = -101;   // dBm
    private double txEnergy = 16.5;     // mA

    private static Logger logger = Logger.getLogger(RF230.class);


    /**
     * {@inheritDoc}
     *
     * Transmits a packet at the currently set power level on the currently set channel.
     * The RF230 spec defines 16 transmit power settings ranging from 3 dBm to -17.2 dBm.
     * The transmit power can be adjusted with the {@link #setTransmitPowerLevel(int)} method.
     */
    @Override
    public void transmit(byte[] data) {

        super.transmit(data);

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

        super.receive(time, data, rxPower, frequency);

        // check if it is within the range of sensitivity of the radio
        if (rxPower < minRxPower) {
            return;
        }

        // todo: check the frequency against the current channel

        // consult the PRR/SNR curve
        double snr = rxPower - getPropagationModel().getNoiseFloor();

        // todo: define a PRR/SNR function
        double prr = 1;

        if (getRandom().nextDouble() <= prr) {
            notifyListeners(time, data, rxPower);
        }
    }


    /** {@inheritDoc} */
    @Override
    protected double getIdleEnergy() {

        // mA (page 80 of the RF230 data sheet, (5131E-MCU Wireless-02/09))
        return 1.5;
    }


    /** {@inheritDoc} */
    @Override
    protected double getRxEnergy() {

        // mA (page 80 of the RF230 data sheet, (5131E-MCU Wireless-02/09))
        return 15.5;
    }


    /** {@inheritDoc} */
    @Override
    protected double getTxEnergy() {
        return txEnergy;
    }


    /** {@inheritDoc} */
    @Override
    protected double getBandwidth() {

        // kbps (page 78 of the RF230 data sheet, (5131E-MCU Wireless-02/09))
        return 250;
    }


    /**
     * Sets the output power level. The RF230 has 16 transmit power settings, detailed on
     * page 59 of the RF230 data sheet (5131E-MCU Wireless-02/09).
     *
     * @param level The transmit power level. It must be an integer in the range (0,15).
     */
    @Inject(optional = true)
    public final void setTransmitPowerLevel(@Named("tx-power-level") final int level) {

        switch(level) {

            case 0:

                txPower = 3;
                txEnergy = 16.5;
                break;

            case 1:

                txPower = 2.6;
                txEnergy = 16.1;
                break;

            case 2:

                txPower = 2.1;
                txEnergy = 15.5;
                break;

            case 3:

                txPower = 1.6;
                txEnergy = 15.1;
                break;

            case 4:

                txPower = 1.1;
                txEnergy = 14.5;
                break;

            case 5:

                txPower = 0.5;
                txEnergy = 14.2;
                break;

            case 6:

                txPower = -0.2;
                txEnergy = 14;
                break;

            case 7:

                txPower = -1.2;
                txEnergy = 13.5;
                break;

            case 8:

                txPower = -2.2;
                txEnergy = 13.0;
                break;

            case 9:

                txPower = -3.2;
                txEnergy = 12.5;
                break;

            case 10:

                txPower = -4.2;
                txEnergy = 12.3;
                break;

            case 11:

                txPower = -5.2;
                txEnergy = 12.1;
                break;

            case 12:

                txPower = -7.2;
                txEnergy = 11.6;
                break;

            case 13:

                txPower = -9.2;
                txEnergy = 11.2;
                break;

            case 14:

                txPower = -12.2;
                txEnergy = 10.6;
                break;

            case 15:

                txPower = -17.2;
                txEnergy = 9.5;
                break;

            default:
                logger.warn("Unrecognized TX power level, using default level.");
        }
    }

}
