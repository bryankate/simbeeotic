package harvard.robobees.simbeeotic.comms;


import harvard.robobees.simbeeotic.SimEngine;


/**
 * A base class that notifies a model when a message is received on its radio
 * by scheduling a {@link MessageReceivedEvent}.
 *
 * @author bkate
 */
public abstract class AbstractRadio implements Radio {

    private int hostModelId;
    private SimEngine engine;


    public AbstractRadio(final int hostModelId, final SimEngine engine) {

        this.hostModelId = hostModelId;
        this.engine = engine;
    }


    /**
     * {@inheritDoc}
     *
     * This implementation schedules an event on the host model when a message is received.
     */
    @Override
    public void receive(byte[] data) {
        engine.scheduleEvent(hostModelId, engine.getCurrentTime(), new MessageReceivedEvent(data));
    }
}
