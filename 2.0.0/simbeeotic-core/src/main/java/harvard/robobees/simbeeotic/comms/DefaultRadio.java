package harvard.robobees.simbeeotic.comms;


import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * A radio that transmits at maximum power. There is no concept of bandwidth or rate.
 *
 * @author bkate
 */
public class DefaultRadio extends AbstractRadio {

    private float maxPower = 50;  // mW
    // You don't snuggle with Max Power, you strap yourself in and feel the G's!


    /** {@inheritDoc} */
    @Override
    public void transmit(byte[] data) {
        getPropagationModel().transmit(this, data, maxPower);
    }


    @Inject(optional = true)
    public final void setMaxPower(@Named(value = "max-power") final float power) {
        this.maxPower = power;
    }
}
