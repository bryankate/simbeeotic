package harvard.robobees.simbeeotic.comms;


/**
 * An interface that describes the physical environment through which RF
 * communications propagate. It is responsible for ensuring that transmissions
 * from one radio are received by others within range. 
 *
 * @author bkate
 */
public interface PropagationModel {

    /**
     * Broadcasts a message in RF.
     *
     * @param tx The {@link Radio} that is transmitting.
     * @param data The data to be transferred.
     * @param txPower The amount of energy used to transmit the data (in dBm).
     * @param band The RF band for this transmission.
     */
    public void transmit(Radio tx, byte[] data, double txPower, Band band);


    /**
     * Gets a noise floor measurement from the RF environment. The returned value
     * from this call may not be constant over time.
     *
     * @return The current noise floor (in dBm).
     */
    public double getNoiseFloor();
}
