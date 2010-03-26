package harvard.robobees.simbeeotic.comms;


/**
 * An interface that describes the physical environment through which RF
 * communications propagate. It is responsible for ensuring that transmissions
 * from one radio are received by others within range. Implementations of this
 * interface
 *
 * @author bkate
 */
public interface PropagationModel {

    /**
     * Adds a radio to environment.
     *
     * @param radio The radio to be added.
     */
    public void addRadio(Radio radio);


    /**
     * Removes a radio from the environment.
     *
     * @param radio The radio to be removed.
     */
    public void removeRadio(Radio radio);


    /**
     * Broadcasts a message in RF.
     *
     * @param tx The {@link Radio} that is transmitting.
     * @param data The data to be transferred.
     * @param txPower The amount of energy used to transmit the data (in mW).
     */
    public void transmit(Radio tx, byte[] data, float txPower);


    /**
     * Gets a noise floor measurement from the RF environment. The returned value
     * from this call may not be constant over time.
     *
     * @return The current noise floor (mW).
     */
    public float getNoiseFloor();
}
