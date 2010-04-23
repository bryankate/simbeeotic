package harvard.robobees.simbeeotic.comms;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;


/**
 * A class that implements some boilerplate functionality of radios
 * that operate in the 2.4 GHz range according to the IEEE 802.15.4
 * specification.
 *
 * @author bkate
 */
public abstract class ZigbeeRadio extends AbstractRadio {

    private Band band = new Band(2405, 5);  // channel 11

    private static Band operating = new Band(2442.5, 85);  // zigbee spectrum

    private static Logger logger = Logger.getLogger(RF230.class);


    /** {@inheritDoc} */
    @Override
    public final Band getOperatingBand() {
        return operating;
    }


    /**
     * Gets the current channel on which the radio is operating.
     *
     * @return The current channel.
     */
    public final Band getChannel() {
        return band;
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

}
