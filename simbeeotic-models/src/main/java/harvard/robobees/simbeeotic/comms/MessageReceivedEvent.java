package harvard.robobees.simbeeotic.comms;


import harvard.robobees.simbeeotic.model.Event;


/**
 * An event that is scheduled on a host model by a radio when a message
 * is received.
 *
 * @author bkate
 */
public class MessageReceivedEvent implements Event {

    private byte[] data;

    
    public MessageReceivedEvent(final byte[] data) {
        this.data = data;
    }


    /**
     * Gets the message payload.
     *
     * @note The byte array returned may be shared by multiple message
     *       recipients so modelers must take care to avoid mutating the data.
     *
     * @return The message payload as a byte array.
     */
    public byte[] getData() {
        return data;
    }
}
