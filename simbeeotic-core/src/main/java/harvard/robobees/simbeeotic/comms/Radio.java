package harvard.robobees.simbeeotic.comms;


import javax.vecmath.Vector3f;


/**
 * An interface that describes a radio that is capable of sending and receiving data.
 *
 * @author bkate
 */
public interface Radio {

    /**
     * Transmits a message over the physical medium. The {@link PropagationModel}
     * in use will determine which radios, if any, receive the message.
     *
     * @param data The data to be transmitted.
     */
    public void transmit(byte[] data);


    /**
     * Called by the {@link PropagationModel} when a transmission is received by this
     * radio.
     *
     * @param time The simulation time when the message was received.
     * @param data The data received.
     * @param rxPower The strength of the received signal (in mW).
     */
    public void receive(double time, byte[] data, float rxPower);


    /**
     * Gets the position of the radio.
     *
     * @return The position of the radio's antenna, in the world reference frame.
     */
    public Vector3f getPosition();


    /**
     * Gets the pointing vector of the antenna. This is a vector that points
     * along the major antenna axis.
     *
     * @return The pointing vector of the antenna, in the world reference frame.
     */
    public Vector3f getPointing();


    /**
     * Gets the radiation pattern of the antenna attached to the radio.
     *
     * @return The antenna pattern in use.
     */
    public AntennaPattern getAntennaPattern();
}
