package harvard.robobees.simbeeotic.model.comms;


import harvard.robobees.simbeeotic.SimTime;

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
     * @param rxPower The strength of the received signal (in dBm).
     * @param frequency The frequency of the received signal (in MHz).
     */
    public void receive(SimTime time, byte[] data, double rxPower, double frequency);


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
    public Vector3f getAntennaPointing();


    /**
     * Gets the vector that is normal to the antenna pointing vector. This
     * vector is essentially the {@code X axis} of the antenna frame translated
     * into the world frame.
     *
     * @return The antenna normal, in the world reference frame.
     */
    public Vector3f getAntennaNormal();


    /**
     * Gets the radiation pattern of the antenna attached to the radio.
     *
     * @return The antenna pattern in use.
     */
    public AntennaPattern getAntennaPattern();


    /**
     * Gets the RF band in which this radio operates. This method must return the full
     * range of possible frequencies, not just the channel in which it is currently
     * operating. The return from this methid is expected to be static over time.
     * 
     * @return The full operating range of the radio.
     */
    public Band getOperatingBand();
}
