package harvard.robobees.simbeeotic.model.comms;


/**
 * A theoretical isotropic antenna that radiates/receives power evenly over
 * a 3-dimensional space.
 *
 * @author bkate
 */
public class IsotropicAntenna implements AntennaPattern {

    /**
     * {@inheritDoc}
     *
     * This antenna will always return {@code 0 dBi} for any orientation.
     */
    @Override
    public double getGain(double azimuth, double elevation) {
        return 0;
    }
}
