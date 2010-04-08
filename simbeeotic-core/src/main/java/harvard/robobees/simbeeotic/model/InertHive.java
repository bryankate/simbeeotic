package harvard.robobees.simbeeotic.model;


/**
 * A hive logic implementation that has no behavior. The hive is
 * completely inert.
 *
 * @author bkate
 */
public class InertHive implements GenericHiveLogic {

    /**
     * {@inheritDoc}
     *
     * Does nothing.
     */
    @Override
    public void initialize(GenericHive bee) {
    }


    /**
     * {@inheritDoc}
     *
     * Does nothing.
     */
    @Override
    public void update(double time) {
    }


    /**
     * {@inheritDoc}
     *
     * Does nothing.
     */
    @Override
    public void messageReceived(double time, byte[] data, float rxPower) {
    }


    /**
     * {@inheritDoc}
     *
     * Does nothing.
     */
    @Override
    public void finish() {
    }
}
