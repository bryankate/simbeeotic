package harvard.robobees.simbeeotic.comms;


import harvard.robobees.simbeeotic.model.PhysicalEntity;

import javax.vecmath.Vector3f;


/**
 * A radio that has a simple dipole antenna and transmits at maximum power. There
 * is no concept of bandwidth or rate.
 *
 * @author bkate
 */
public class DefaultRadio extends AbstractRadio {

    private PropagationModel prop;
    private PhysicalEntity host;

    private float maxPower;  // mW
    // You don't snuggle with Max Power, you strap yourself in and feel the G's!


    public DefaultRadio(PropagationModel prop, PhysicalEntity host, float maxPower) {

        this.prop = prop;
        this.host = host;
        this.maxPower = maxPower;
    }


    /** {@inheritDoc} */
    @Override
    public void transmit(byte[] data) {
        prop.transmit(this, data, maxPower);
    }


    /** {@inheritDoc} */
    @Override
    public Vector3f getPosition() {
        return host.getTruthPosition();
    }
}
