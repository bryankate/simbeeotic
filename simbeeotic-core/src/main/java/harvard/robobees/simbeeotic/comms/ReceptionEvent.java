package harvard.robobees.simbeeotic.comms;


import harvard.robobees.simbeeotic.model.Event;


/**
 * An event that marksthe reception of an RF transmission by a radio.
 *
 * @author bkate
 */
public final class ReceptionEvent implements Event {

    private byte[] data;
    private double rxPower;
    private Band band;


    public ReceptionEvent(byte[] data, double rxPower, Band band) {

        this.data = data;
        this.rxPower = rxPower;
        this.band = band;
    }


    public Band getBand() {
        return band;
    }


    public double getRxPower() {
        return rxPower;
    }


    public byte[] getData() {
        return data;
    }
}
